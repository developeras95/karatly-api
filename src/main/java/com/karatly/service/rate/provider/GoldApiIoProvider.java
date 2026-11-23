package com.karatly.service.rate.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.karatly.config.RatesProperties;
import com.karatly.entity.rate.RateSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * goldapi.io: platinum and palladium (free plan: 150 requests a month). Needs an API key, sent in the
 * {@code x-access-token} header. One request per metal: {@code GET /api/price/{metal}/{currency}}.
 */
public class GoldApiIoProvider implements RateProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };
    private static final List<String> METALS = List.of("XPT", "XPD");

    private final RestClient client;
    private final RatesProperties.Provider settings;

    public GoldApiIoProvider(RestClient client, RatesProperties.Provider settings) {
        this.client = client;
        this.settings = settings;
    }

    @Override
    public RateSource source() {
        return RateSource.GOLDAPI_IO;
    }

    @Override
    public List<String> metals() {
        return METALS;
    }

    @Override
    public boolean isAvailable() {
        return settings.enabled() && settings.hasApiKey();
    }

    @Override
    public String unavailableReason() {
        if (!settings.enabled()) {
            return "Switched off (KARATLY_GOLDAPI_IO_ENABLED=false).";
        }
        return settings.hasApiKey() ? null : "No API key: set KARATLY_GOLDAPI_IO_API_KEY (free key at goldapi.io).";
    }

    @Override
    public int requestsPerFetch(String currency) {
        return METALS.size();
    }

    @Override
    public int monthlyBudget() {
        return settings.monthlyBudget();
    }

    @Override
    public List<FetchedRate> fetch(String currency) {
        List<FetchedRate> rates = new ArrayList<>();
        for (String metal : METALS) {
            Map<String, Object> body = price(metal, currency);
            String context = "goldapi.io " + metal + "/" + currency;
            BigDecimal perGram;
            if (body.get("price_per_unit") instanceof Map<?, ?> perUnit && perUnit.get("gram") != null) {
                perGram = MetalUnits.perGram(JsonValues.decimal(perUnit, "gram", context), "gram");
            } else {
                Object unit = body.get("unit");
                perGram = MetalUnits.perGram(JsonValues.decimal(body, "price", context),
                        unit == null ? "troy_ounce" : unit.toString());
            }
            rates.add(new FetchedRate(metal, perGram, JsonValues.time(body.get("timestamp"), Instant.now())));
        }
        return rates;
    }

    private Map<String, Object> price(String metal, String currency) {
        String uri = "/api/price/" + metal + "/" + currency + "?melt_price=false&currency_info=false";
        try {
            Map<String, Object> body = client.get().uri(uri).retrieve().body(JSON);
            if (body == null) {
                throw new RateFetchException("goldapi.io " + uri + " answered with an empty body.");
            }
            if (body.get("error") != null) {
                throw new RateFetchException("goldapi.io " + metal + "/" + currency + ": " + body.get("error"));
            }
            return body;
        } catch (RestClientException ex) {
            throw new RateFetchException("goldapi.io " + uri + " failed: " + ex.getMessage(), ex);
        }
    }
}
