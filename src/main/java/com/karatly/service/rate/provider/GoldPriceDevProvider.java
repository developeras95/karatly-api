package com.karatly.service.rate.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.karatly.config.RatesProperties;
import com.karatly.entity.rate.RateSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * goldprice.dev: gold, silver and copper (free plan: 5,000 requests a month, prices 15 minutes late).
 *
 * <p>One fetch uses at most two requests: {@code GET /v1/prices} returns every spot price at once (gold in
 * many currencies, silver in USD per troy ounce, copper in USD per pound), and {@code GET /v1/convert}
 * gives the USD exchange rate for the metals not quoted in the wanted currency.
 */
public class GoldPriceDevProvider implements RateProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };
    private static final String USD = "USD";

    /** Karatly's metal code and the symbol goldprice.dev uses for it. */
    private static final Map<String, String> SYMBOLS = Map.of("XAU", "XAU", "XAG", "XAG", "XCU", "HG");
    private static final List<String> METALS = List.of("XAU", "XAG", "XCU");

    private final RestClient client;
    private final RatesProperties.Provider settings;

    public GoldPriceDevProvider(RestClient client, RatesProperties.Provider settings) {
        this.client = client;
        this.settings = settings;
    }

    @Override
    public RateSource source() {
        return RateSource.GOLDPRICE_DEV;
    }

    @Override
    public List<String> metals() {
        return METALS;
    }

    @Override
    public boolean isAvailable() {
        return settings.enabled();
    }

    @Override
    public String unavailableReason() {
        return settings.enabled() ? null : "Switched off (KARATLY_GOLDPRICE_DEV_ENABLED=false).";
    }

    @Override
    public int requestsPerFetch(String currency) {
        return USD.equals(currency) ? 1 : 2;
    }

    @Override
    public int monthlyBudget() {
        return settings.monthlyBudget();
    }

    @Override
    public List<FetchedRate> fetch(String currency) {
        List<Map<?, ?>> rows = spotRows();
        BigDecimal usdRate = null;
        List<FetchedRate> rates = new ArrayList<>();
        for (String metal : METALS) {
            String symbol = SYMBOLS.get(metal);
            Optional<Map<?, ?>> inCurrency = find(rows, symbol, currency);
            Map<?, ?> row;
            BigDecimal factor;
            if (inCurrency.isPresent()) {
                row = inCurrency.get();
                factor = BigDecimal.ONE;
            } else {
                row = find(rows, symbol, USD).orElseThrow(() ->
                        new RateFetchException("goldprice.dev returned no spot price for " + symbol + "."));
                if (usdRate == null) {
                    usdRate = usdRate(currency);
                }
                factor = usdRate;
            }
            String context = "goldprice.dev " + symbol;
            BigDecimal price = JsonValues.decimal(row, "price", context).multiply(factor);
            Object unit = row.get("unit");
            BigDecimal perGram = MetalUnits.perGram(price, unit == null ? null : unit.toString());
            rates.add(new FetchedRate(metal, perGram, JsonValues.time(row.get("computed_at"), Instant.now())));
        }
        return rates;
    }

    private List<Map<?, ?>> spotRows() {
        Map<String, Object> body = get("/v1/prices");
        if (!(body.get("symbols") instanceof List<?> list)) {
            throw new RateFetchException("goldprice.dev /v1/prices answered without a 'symbols' list.");
        }
        List<Map<?, ?>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rows.add(map);
            }
        }
        return rows;
    }

    private static Optional<Map<?, ?>> find(List<Map<?, ?>> rows, String symbol, String currency) {
        return rows.stream()
                .filter(row -> symbol.equals(row.get("symbol")))
                .filter(row -> currency.equals(row.get("quote_currency")))
                .filter(row -> row.get("contract_type") == null || "spot".equals(row.get("contract_type")))
                .findFirst();
    }

    /** How many units of the currency one US dollar buys. */
    private BigDecimal usdRate(String currency) {
        Map<String, Object> body = get("/v1/convert?from=USD&to=" + currency + "&amount=1");
        BigDecimal rate = JsonValues.decimal(body, "rate", "goldprice.dev USD→" + currency);
        if (rate.signum() <= 0) {
            throw new RateFetchException("goldprice.dev gave a USD→" + currency + " rate of " + rate + ".");
        }
        return rate.setScale(8, RoundingMode.HALF_UP);
    }

    private Map<String, Object> get(String uri) {
        try {
            Map<String, Object> body = client.get().uri(uri).retrieve().body(JSON);
            if (body == null) {
                throw new RateFetchException("goldprice.dev " + uri + " answered with an empty body.");
            }
            return body;
        } catch (RestClientException ex) {
            throw new RateFetchException("goldprice.dev " + uri + " failed: " + ex.getMessage(), ex);
        }
    }
}
