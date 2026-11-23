package com.karatly.service.rate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.karatly.config.RatesProperties;
import com.karatly.entity.rate.RateSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Runs the provider against canned goldprice.dev answers (copied from the real API). */
class GoldPriceDevProviderTest {

    private static final String BASE = "https://api.goldprice.dev";
    private static final String PRICES = """
            {"symbols":[
              {"symbol":"HG","quote_currency":"USD","unit":"pound","contract_type":"spot","price":"5.5139",
               "is_stale":false,"computed_at":"2026-11-23T05:51:02.019224+00:00"},
              {"symbol":"XAG","quote_currency":"USD","unit":"troy_ounce","contract_type":"spot","price":"42.689",
               "is_stale":false,"computed_at":"2026-11-23T05:51:02.019224+00:00"},
              {"symbol":"XAU","quote_currency":"AUD","unit":"troy_ounce","contract_type":"spot","price":"5453.24",
               "is_stale":false,"computed_at":"2026-11-23T05:51:02.019224+00:00"},
              {"symbol":"XAU","quote_currency":"INR","unit":"troy_ounce","contract_type":"spot","price":"336544.18",
               "is_stale":false,"computed_at":"2026-11-23T05:51:02.019224+00:00"},
              {"symbol":"XAU","quote_currency":"USD","unit":"troy_ounce","contract_type":"spot","price":"3622.65",
               "is_stale":false,"computed_at":"2026-11-23T05:51:02.019224+00:00"}
            ]}""";

    private MockRestServiceServer server;
    private GoldPriceDevProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GoldPriceDevProvider(builder.build(), settings(true));
    }

    @Test
    void fetchesAllThreeMetalsPerGramInRupees() {
        server.expect(requestTo(BASE + "/v1/prices")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PRICES, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/convert?from=USD&to=INR&amount=1"))
                .andRespond(withSuccess("""
                        {"from":"USD","to":"INR","amount":"1.0","rate":"92.833","result":"92.83","unit":"oz"}
                        """, MediaType.APPLICATION_JSON));

        List<FetchedRate> rates = provider.fetch("INR");

        server.verify();
        assertThat(rates).extracting(FetchedRate::metalCode).containsExactly("XAU", "XAG", "XCU");
        // Gold is quoted in rupees directly; silver and copper are converted from US dollars.
        assertThat(rates.get(0).spotPricePerGram()).isEqualByComparingTo("10820.146640");
        assertThat(rates.get(1).spotPricePerGram()).isEqualByComparingTo("127.411735");
        assertThat(rates.get(2).spotPricePerGram()).isEqualByComparingTo("1.128484");
        assertThat(rates.get(0).pricedAt()).isEqualTo(Instant.parse("2026-11-23T05:51:02.019224Z"));
    }

    @Test
    void usesOneRequestWhenTheCurrencyIsUsDollars() {
        server.expect(requestTo(BASE + "/v1/prices")).andRespond(withSuccess(PRICES, MediaType.APPLICATION_JSON));

        List<FetchedRate> rates = provider.fetch("USD");

        server.verify();
        assertThat(rates.get(0).spotPricePerGram()).isEqualByComparingTo("116.470902");
        assertThat(rates.get(1).spotPricePerGram()).isEqualByComparingTo("1.372483");
        assertThat(rates.get(2).spotPricePerGram()).isEqualByComparingTo("0.012156");
        assertThat(provider.requestsPerFetch("USD")).isEqualTo(1);
        assertThat(provider.requestsPerFetch("INR")).isEqualTo(2);
    }

    @Test
    void anAnswerWithoutSymbolsIsAnError() {
        server.expect(requestTo(BASE + "/v1/prices")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetch("INR"))
                .isInstanceOf(RateFetchException.class)
                .hasMessageContaining("symbols");
    }

    @Test
    void aMissingMetalIsAnError() {
        server.expect(requestTo(BASE + "/v1/prices")).andRespond(withSuccess("""
                {"symbols":[{"symbol":"XAU","quote_currency":"INR","unit":"troy_ounce","price":"336544.18"}]}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetch("INR"))
                .isInstanceOf(RateFetchException.class)
                .hasMessageContaining("XAG");
    }

    @Test
    void aServerErrorIsReportedAsAFetchError() {
        server.expect(requestTo(BASE + "/v1/prices")).andRespond(withServerError());

        assertThatThrownBy(() -> provider.fetch("INR"))
                .isInstanceOf(RateFetchException.class)
                .hasMessageContaining("goldprice.dev");
    }

    @Test
    void describesItself() {
        GoldPriceDevProvider off = new GoldPriceDevProvider(RestClient.create(), settings(false));

        assertThat(provider.source()).isEqualTo(RateSource.GOLDPRICE_DEV);
        assertThat(provider.metals()).containsExactly("XAU", "XAG", "XCU");
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.unavailableReason()).isNull();
        assertThat(provider.monthlyBudget()).isEqualTo(4500);
        assertThat(off.isAvailable()).isFalse();
        assertThat(off.unavailableReason()).contains("Switched off");
    }

    private static RatesProperties.Provider settings(boolean enabled) {
        return new RatesProperties.Provider(enabled, BASE, "", 4500, Duration.ofSeconds(5));
    }
}
