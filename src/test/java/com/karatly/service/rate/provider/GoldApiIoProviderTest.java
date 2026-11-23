package com.karatly.service.rate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.karatly.config.RatesProperties;
import com.karatly.entity.rate.RateSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoldApiIoProviderTest {

    private static final String BASE = "https://www.goldapi.io";
    private static final String QUERY = "?melt_price=false&currency_info=false";

    private MockRestServiceServer server;
    private GoldApiIoProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GoldApiIoProvider(builder.build(), settings(true, "key"));
    }

    @Test
    void readsThePricePerGramWhenGiven() {
        server.expect(requestTo(BASE + "/api/price/XPT/INR" + QUERY)).andRespond(withSuccess("""
                {"timestamp":1795400000,"metal":"XPT","currency":"INR","price":95000.5,"unit":"troy_ounce",
                 "price_per_unit":{"troy_ounce":95000.5,"gram":3054.34}}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/price/XPD/INR" + QUERY)).andRespond(withSuccess("""
                {"timestamp":1795400000,"metal":"XPD","currency":"INR","price":95000.5,"unit":"troy_ounce"}
                """, MediaType.APPLICATION_JSON));

        List<FetchedRate> rates = provider.fetch("INR");

        server.verify();
        assertThat(rates).extracting(FetchedRate::metalCode).containsExactly("XPT", "XPD");
        assertThat(rates.get(0).spotPricePerGram()).isEqualByComparingTo("3054.34");
        // Without price_per_unit the troy-ounce price is converted.
        assertThat(rates.get(1).spotPricePerGram()).isEqualByComparingTo("3054.336999");
        assertThat(rates.get(0).pricedAt()).isEqualTo(Instant.ofEpochSecond(1795400000));
    }

    @Test
    void anErrorInTheBodyIsAFetchError() {
        server.expect(requestTo(BASE + "/api/price/XPT/INR" + QUERY))
                .andRespond(withSuccess("{\"error\":\"Invalid API key\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetch("INR"))
                .isInstanceOf(RateFetchException.class)
                .hasMessageContaining("Invalid API key");
    }

    @Test
    void quotaExhaustionIsAFetchError() {
        server.expect(requestTo(BASE + "/api/price/XPT/INR" + QUERY)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> provider.fetch("INR"))
                .isInstanceOf(RateFetchException.class)
                .hasMessageContaining("goldapi.io");
    }

    @Test
    void needsAKeyAndCanBeSwitchedOff() {
        GoldApiIoProvider noKey = new GoldApiIoProvider(RestClient.create(), settings(true, " "));
        GoldApiIoProvider off = new GoldApiIoProvider(RestClient.create(), settings(false, "key"));

        assertThat(provider.source()).isEqualTo(RateSource.GOLDAPI_IO);
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.unavailableReason()).isNull();
        assertThat(provider.requestsPerFetch("INR")).isEqualTo(2);
        assertThat(provider.monthlyBudget()).isEqualTo(140);
        assertThat(noKey.isAvailable()).isFalse();
        assertThat(noKey.unavailableReason()).contains("KARATLY_GOLDAPI_IO_API_KEY");
        assertThat(off.isAvailable()).isFalse();
        assertThat(off.unavailableReason()).contains("Switched off");
    }

    private static RatesProperties.Provider settings(boolean enabled, String key) {
        return new RatesProperties.Provider(enabled, BASE, key, 140, Duration.ofSeconds(5));
    }
}
