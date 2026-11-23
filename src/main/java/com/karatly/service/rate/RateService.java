package com.karatly.service.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.karatly.config.RatesProperties;
import com.karatly.dto.rate.ManualRateRequest;
import com.karatly.dto.rate.PremiumResponse;
import com.karatly.dto.rate.ProviderStatusResponse;
import com.karatly.dto.rate.PurityPrice;
import com.karatly.dto.rate.RateHistoryResponse;
import com.karatly.dto.rate.RateResponse;
import com.karatly.dto.rate.RefreshResult;
import com.karatly.dto.rate.RefreshResult.Status;
import com.karatly.entity.rate.MetalRate;
import com.karatly.entity.rate.RatePremium;
import com.karatly.entity.rate.RateProviderCall;
import com.karatly.entity.rate.RateSource;
import com.karatly.entity.reference.Metal;
import com.karatly.entity.reference.Purity;
import com.karatly.exception.BadRequestException;
import com.karatly.exception.NotFoundException;
import com.karatly.repository.rate.MetalRateRepository;
import com.karatly.repository.rate.RatePremiumRepository;
import com.karatly.repository.rate.RateProviderCallRepository;
import com.karatly.repository.reference.MetalRepository;
import com.karatly.repository.reference.PurityRepository;
import com.karatly.service.rate.provider.FetchedRate;
import com.karatly.service.rate.provider.RateProvider;
import com.karatly.util.Decimals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Metal rates: the latest rate per metal, rate history, manual rates, premiums, and fetching from the
 * outside providers.
 *
 * <p>The metal_rate table is the cache. A provider is only called when a cached rate of one of its metals
 * is older than the TTL (24 h / karatly.rates.fetches-per-day, checked against updated_at) or missing. A
 * provider that failed is left alone for karatly.rates.retry-after, and no fetch may take a provider over
 * its monthly request budget. Reads never wait for a fetch that is already running.
 */
@Service
public class RateService {

    private static final Logger log = LoggerFactory.getLogger(RateService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PRICE_SCALE = 6;
    private static final int MAX_HISTORY_DAYS = 366;
    private static final int DEFAULT_HISTORY_DAYS = 30;

    private final MetalRateRepository rates;
    private final RatePremiumRepository premiums;
    private final RateProviderCallRepository calls;
    private final MetalRepository metals;
    private final PurityRepository purities;
    private final List<RateProvider> providers;
    private final RatesProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ReentrantLock fetchLock = new ReentrantLock();

    public RateService(MetalRateRepository rates, RatePremiumRepository premiums, RateProviderCallRepository calls,
                       MetalRepository metals, PurityRepository purities, List<RateProvider> providers,
                       RatesProperties properties, TransactionTemplate transaction, Clock clock) {
        this.rates = rates;
        this.premiums = premiums;
        this.calls = calls;
        this.metals = metals;
        this.purities = purities;
        this.providers = List.copyOf(providers);
        this.properties = properties;
        this.transaction = transaction;
        this.clock = clock;
    }

    // ---- Reading ----------------------------------------------------------------------------------------

    /**
     * The newest rate of every active metal, with the rate for each purity. When karatly.rates.refresh-on-read
     * is on, rates past the TTL are fetched first. Metals that never had a rate are left out.
     */
    public List<RateResponse> latest() {
        if (properties.refreshOnRead()) {
            refreshIfDue();
        }
        String currency = properties.currency();
        LocalDate today = today();
        Map<String, List<Purity>> purityByMetal = purities.findAllActive().stream()
                .collect(Collectors.groupingBy(Purity::getMetalCode, LinkedHashMap::new, Collectors.toList()));

        List<RateResponse> result = new ArrayList<>();
        for (Metal metal : metals.findByActiveTrueOrderBySortOrderAsc()) {
            effective(rates.findTop10ByMetalCodeAndCurrencyOrderByRateDateDescUpdatedAtDesc(metal.getCode(), currency))
                    .ifPresent(rate -> result.add(toResponse(rate, metal.getName(), today,
                            purityByMetal.getOrDefault(metal.getCode(), List.of()))));
        }
        return result;
    }

    /** One rate per day between from and to (inclusive); a manual rate wins over a fetched one. */
    public RateHistoryResponse history(String metalCode, LocalDate from, LocalDate to) {
        Metal metal = findMetal(metalCode);
        LocalDate end = to == null ? today() : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_HISTORY_DAYS) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "'from' must not be after 'to'.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_HISTORY_DAYS) {
            throw new BadRequestException("DATE_RANGE_TOO_LONG",
                    "Ask for at most " + MAX_HISTORY_DAYS + " days at a time.");
        }
        String currency = properties.currency();
        Map<LocalDate, MetalRate> perDay = new LinkedHashMap<>();
        for (MetalRate rate : rates.findByMetalCodeAndCurrencyAndRateDateBetweenOrderByRateDateAsc(
                metal.getCode(), currency, start, end)) {
            perDay.merge(rate.getRateDate(), rate, RateService::preferred);
        }
        List<RateHistoryResponse.Point> points = perDay.values().stream()
                .map(rate -> new RateHistoryResponse.Point(rate.getRateDate(), rate.getPricePerGram(), rate.getSource()))
                .toList();
        return new RateHistoryResponse(metal.getCode(), currency, start, end, points);
    }

    // ---- Manual rates and premiums ------------------------------------------------------------------------

    /** Stores a rate typed in by hand for a day (today by default), replacing an earlier manual rate. */
    @Transactional
    public RateResponse addManual(ManualRateRequest request) {
        Metal metal = findMetal(request.metal());
        LocalDate date = request.date() == null ? today() : request.date();
        String currency = properties.currency();
        Instant now = clock.instant();
        MetalRate rate = rates.findByMetalCodeAndCurrencyAndRateDateAndSource(metal.getCode(), currency, date,
                        RateSource.MANUAL)
                .orElseGet(() -> new MetalRate(metal.getCode(), currency, date, RateSource.MANUAL, now));
        rate.setPrices(null, BigDecimal.ZERO, request.pricePerGram().setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                now, now);
        MetalRate saved = rates.save(rate);
        return toResponse(saved, metal.getName(), today(), purities.findActiveByMetal(metal.getCode()));
    }

    public List<PremiumResponse> listPremiums() {
        return premiums.findAllByOrderByMetalCodeAsc().stream().map(PremiumResponse::from).toList();
    }

    @Transactional
    public PremiumResponse updatePremium(String metalCode, BigDecimal premiumPercent) {
        String code = normalise(metalCode);
        RatePremium premium = premiums.findById(code)
                .orElseThrow(() -> new NotFoundException("METAL_NOT_FOUND", "Metal " + code + " was not found."));
        premium.change(premiumPercent, clock.instant());
        return PremiumResponse.from(premium);
    }

    // ---- Fetching ---------------------------------------------------------------------------------------

    /**
     * Asks every provider for rates. With force, providers are called even when their cached rates are
     * within the TTL (still within their monthly budget); used by POST /api/v1/rates/refresh. Waits for a
     * fetch already running.
     */
    public List<RefreshResult> refresh(boolean force) {
        fetchLock.lock();
        try {
            return providers.stream().map(provider -> refresh(provider, force)).toList();
        } finally {
            fetchLock.unlock();
        }
    }

    /** Fetches from providers whose cached rates are past the TTL. Returns at once if a fetch is running. */
    public List<RefreshResult> refreshIfDue() {
        if (!fetchLock.tryLock()) {
            return List.of();
        }
        try {
            return providers.stream().map(provider -> refresh(provider, false)).toList();
        } finally {
            fetchLock.unlock();
        }
    }

    /** Settings, quota use and timing of every provider. */
    public List<ProviderStatusResponse> providerStatus() {
        Instant now = clock.instant();
        Instant monthStart = monthStart();
        return providers.stream().map(provider -> {
            String name = provider.source().name();
            Optional<RateProviderCall> last = calls.findFirstByProviderOrderByCalledAtDesc(name);
            return new ProviderStatusResponse(
                    provider.source(),
                    provider.metals(),
                    provider.isAvailable(),
                    provider.unavailableReason(),
                    properties.ttl().toMinutes(),
                    (int) calls.sumRequestsSince(name, monthStart),
                    provider.monthlyBudget(),
                    last.map(RateProviderCall::getCalledAt).orElse(null),
                    last.map(RateProviderCall::isSuccess).orElse(null),
                    nextDue(provider, now));
        }).toList();
    }

    private RefreshResult refresh(RateProvider provider, boolean force) {
        RateSource source = provider.source();
        if (!provider.isAvailable()) {
            return new RefreshResult(source, Status.UNAVAILABLE, provider.metals(), provider.unavailableReason());
        }
        Instant now = clock.instant();
        if (!force) {
            Instant due = nextDue(provider, now);
            if (due.isAfter(now)) {
                return new RefreshResult(source, Status.NOT_DUE, provider.metals(), "Cached rates are fresh until " + due + ".");
            }
        }
        String currency = properties.currency();
        int needed = provider.requestsPerFetch(currency);
        long used = calls.sumRequestsSince(source.name(), monthStart());
        if (used + needed > provider.monthlyBudget()) {
            return new RefreshResult(source, Status.OVER_BUDGET, provider.metals(),
                    "Used " + used + " of " + provider.monthlyBudget() + " requests this month.");
        }

        List<FetchedRate> fetched;
        try {
            fetched = provider.fetch(currency);
        } catch (RuntimeException ex) {
            log.warn("Rate provider {} failed: {}", source, ex.getMessage());
            calls.save(new RateProviderCall(source.name(), now, needed, false, ex.getMessage()));
            return new RefreshResult(source, Status.FAILED, provider.metals(), ex.getMessage());
        }

        transaction.executeWithoutResult(status -> {
            store(source, currency, fetched, now);
            calls.save(new RateProviderCall(source.name(), now, needed, true, null));
        });
        List<String> fetchedMetals = fetched.stream().map(FetchedRate::metalCode).toList();
        log.info("Fetched {} rates from {}", fetchedMetals, source);
        return new RefreshResult(source, Status.FETCHED, fetchedMetals, null);
    }

    /**
     * When the provider should be fetched next (TTL logic). TTL = 24 h / karatly.rates.fetches-per-day. For
     * each of the provider's metals, the cached rate's updated_at + TTL is when it expires; the provider is
     * due at the earliest expiry, or now if a metal has no cached rate. After a failure it is left alone for
     * karatly.rates.retry-after, so a provider that is down is not called on every check.
     */
    private Instant nextDue(RateProvider provider, Instant now) {
        String currency = properties.currency();
        Duration ttl = properties.ttl();
        Instant due = null;
        for (String metal : provider.metals()) {
            Optional<MetalRate> cached = rates.findFirstByMetalCodeAndCurrencyAndSourceOrderByUpdatedAtDesc(
                    metal, currency, provider.source());
            Instant expires = cached.map(rate -> rate.getUpdatedAt().plus(ttl)).orElse(now);
            due = due == null || expires.isBefore(due) ? expires : due;
        }
        if (due == null || due.isBefore(now)) {
            due = now;
        }
        Optional<RateProviderCall> lastCall = calls.findFirstByProviderOrderByCalledAtDesc(provider.source().name());
        if (lastCall.isPresent() && !lastCall.get().isSuccess()) {
            due = latest(due, lastCall.get().getCalledAt().plus(properties.retryAfter()));
        }
        return due;
    }

    /** Stores fetched spot prices as today's rates, applying each metal's premium. */
    private void store(RateSource source, String currency, List<FetchedRate> fetched, Instant now) {
        Map<String, BigDecimal> premiumByMetal = premiums.findAll().stream()
                .collect(Collectors.toMap(RatePremium::getMetalCode, RatePremium::getPremiumPercent));
        LocalDate today = today();
        for (FetchedRate item : fetched) {
            BigDecimal premium = premiumByMetal.getOrDefault(item.metalCode(), BigDecimal.ZERO);
            BigDecimal spot = item.spotPricePerGram().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal price = withPremium(spot, premium);
            MetalRate rate = rates.findByMetalCodeAndCurrencyAndRateDateAndSource(item.metalCode(), currency, today, source)
                    .orElseGet(() -> new MetalRate(item.metalCode(), currency, today, source, now));
            rate.setPrices(spot, premium, price, item.pricedAt(), now);
            rates.save(rate);
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------------------

    /** spot × (1 + premium / 100), rounded to 6 decimals. */
    static BigDecimal withPremium(BigDecimal spot, BigDecimal premiumPercent) {
        BigDecimal factor = BigDecimal.ONE.add(premiumPercent.divide(HUNDRED, 10, RoundingMode.HALF_UP));
        return spot.multiply(factor).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** The rate to use from the newest rows: the latest day, and on that day a manual rate first. */
    private static Optional<MetalRate> effective(List<MetalRate> newestFirst) {
        if (newestFirst.isEmpty()) {
            return Optional.empty();
        }
        LocalDate latestDay = newestFirst.get(0).getRateDate();
        return newestFirst.stream()
                .filter(rate -> rate.getRateDate().equals(latestDay))
                .reduce(RateService::preferred);
    }

    /** Of two rates for the same day: the manual one, otherwise the more recently updated one. */
    private static MetalRate preferred(MetalRate a, MetalRate b) {
        if (a.getSource() == RateSource.MANUAL) {
            return a;
        }
        if (b.getSource() == RateSource.MANUAL) {
            return b;
        }
        return b.getUpdatedAt().isAfter(a.getUpdatedAt()) ? b : a;
    }

    private RateResponse toResponse(MetalRate rate, String metalName, LocalDate today, List<Purity> metalPurities) {
        List<PurityPrice> purityPrices = metalPurities.stream()
                .map(purity -> new PurityPrice(purity.getLabel(), Decimals.plain(purity.getFineness()),
                        rate.getPricePerGram().multiply(purity.getFineness()).setScale(2, RoundingMode.HALF_UP)))
                .toList();
        return new RateResponse(
                rate.getMetalCode(),
                metalName,
                rate.getCurrency(),
                rate.getRateDate(),
                rate.getPricePerGram().setScale(2, RoundingMode.HALF_UP),
                rate.getSpotPricePerGram() == null ? null : rate.getSpotPricePerGram().setScale(2, RoundingMode.HALF_UP),
                Decimals.plain(rate.getPremiumPercent()),
                rate.getSource(),
                rate.getPricedAt(),
                rate.getRateDate().isBefore(today),
                purityPrices);
    }

    private Metal findMetal(String code) {
        String normalised = normalise(code);
        return metals.findById(normalised)
                .orElseThrow(() -> new NotFoundException("METAL_NOT_FOUND", "Metal " + normalised + " was not found."));
    }

    private static String normalise(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Instant monthStart() {
        return today().withDayOfMonth(1).atStartOfDay(clock.getZone()).toInstant();
    }
}
