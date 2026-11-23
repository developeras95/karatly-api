package com.karatly;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.karatly.entity.rate.RateSource;
import com.karatly.service.rate.provider.FetchedRate;
import com.karatly.service.rate.provider.GoldApiIoProvider;
import com.karatly.service.rate.provider.GoldPriceDevProvider;
import com.karatly.service.rate.provider.RateFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rates end to end against a real PostgreSQL. The two outside providers are replaced by mocks, so the
 * tests control what they return and can count how often they are called (the TTL cache).
 */
@AutoConfigureMockMvc
class RateApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    GoldPriceDevProvider goldPriceDev;

    @MockitoBean
    GoldApiIoProvider goldApiIo;

    @BeforeEach
    void resetState() {
        jdbc.update("DELETE FROM metal_rate");
        jdbc.update("DELETE FROM rate_provider_call");
        jdbc.update("UPDATE rate_premium SET premium_percent = 0");

        when(goldPriceDev.source()).thenReturn(RateSource.GOLDPRICE_DEV);
        when(goldPriceDev.metals()).thenReturn(List.of("XAU", "XAG", "XCU"));
        when(goldPriceDev.isAvailable()).thenReturn(true);
        when(goldPriceDev.requestsPerFetch(anyString())).thenReturn(2);
        when(goldPriceDev.monthlyBudget()).thenReturn(4500);
        when(goldPriceDev.fetch("INR")).thenReturn(List.of(
                new FetchedRate("XAU", new BigDecimal("10000"), Instant.now()),
                new FetchedRate("XAG", new BigDecimal("125.5"), Instant.now()),
                new FetchedRate("XCU", new BigDecimal("1.13"), Instant.now())));

        when(goldApiIo.source()).thenReturn(RateSource.GOLDAPI_IO);
        when(goldApiIo.metals()).thenReturn(List.of("XPT", "XPD"));
        when(goldApiIo.isAvailable()).thenReturn(false);
        when(goldApiIo.unavailableReason()).thenReturn("No API key");
    }

    @Test
    void refreshStoresRatesWithThePremiumAndLatestServesThem() throws Exception {
        mvc.perform(put("/api/v1/rates/premiums/xau").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"premiumPercent\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metal").value("XAU"))
                .andExpect(jsonPath("$.premiumPercent").value(10));

        mvc.perform(post("/api/v1/rates/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("FETCHED"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDAPI_IO')].status").value("UNAVAILABLE"));

        mvc.perform(get("/api/v1/rates/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].metal").value("XAU"))
                .andExpect(jsonPath("$[0].metalName").value("Gold"))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].spotPricePerGram").value(10000.00))
                .andExpect(jsonPath("$[0].premiumPercent").value(10))
                .andExpect(jsonPath("$[0].pricePerGram").value(11000.00))
                .andExpect(jsonPath("$[0].source").value("GOLDPRICE_DEV"))
                .andExpect(jsonPath("$[0].stale").value(false))
                .andExpect(jsonPath("$[0].purities", hasSize(5)))
                .andExpect(jsonPath("$[0].purities[2].label").value("22K (916)"))
                .andExpect(jsonPath("$[0].purities[2].pricePerGram").value(10076.00))
                .andExpect(jsonPath("$[1].metal").value("XAG"))
                .andExpect(jsonPath("$[1].pricePerGram").value(125.50))
                .andExpect(jsonPath("$[2].metal").value("XCU"))
                .andExpect(jsonPath("$[2].purities", hasSize(0)));
    }

    @Test
    void ratesWithinTheTtlAreNotFetchedAgain() throws Exception {
        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("FETCHED"));
        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("NOT_DUE"));

        verify(goldPriceDev, times(1)).fetch("INR");
        verify(goldApiIo, never()).fetch(anyString());
    }

    @Test
    void ratesPastTheTtlAreFetchedAgain() throws Exception {
        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"));
        // Age the cache past the TTL (24 h / 2 fetches a day = 12 h).
        jdbc.update("UPDATE metal_rate SET updated_at = updated_at - INTERVAL '13 hours'");

        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("FETCHED"));

        verify(goldPriceDev, times(2)).fetch("INR");
    }

    @Test
    void aFailureIsLoggedAndNotRetriedStraightAway() throws Exception {
        when(goldPriceDev.fetch("INR")).thenThrow(new RateFetchException("goldprice.dev is down"));

        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("FAILED"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].message").value("goldprice.dev is down"));
        mvc.perform(post("/api/v1/rates/refresh").param("force", "false"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("NOT_DUE"));

        verify(goldPriceDev, times(1)).fetch("INR");
        mvc.perform(get("/api/v1/rates/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].requestsThisMonth").value(2))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].lastFetchSucceeded").value(false))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].ttlMinutes").value(720))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDAPI_IO')].available").value(false));
    }

    @Test
    void aFetchThatWouldExceedTheMonthlyBudgetIsRefused() throws Exception {
        when(goldPriceDev.monthlyBudget()).thenReturn(1);

        mvc.perform(post("/api/v1/rates/refresh"))
                .andExpect(jsonPath("$[?(@.provider == 'GOLDPRICE_DEV')].status").value("OVER_BUDGET"));

        verify(goldPriceDev, never()).fetch(anyString());
    }

    @Test
    void aManualRateWinsAndShowsInTheHistory() throws Exception {
        mvc.perform(post("/api/v1/rates/refresh"));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        mvc.perform(post("/api/v1/rates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metal\": \"xau\", \"pricePerGram\": 12850.5}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.pricePerGram").value(12850.50))
                .andExpect(jsonPath("$.spotPricePerGram").doesNotExist());

        mvc.perform(get("/api/v1/rates/latest"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"))
                .andExpect(jsonPath("$[0].pricePerGram").value(12850.50));

        mvc.perform(post("/api/v1/rates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metal\": \"XAU\", \"date\": \"" + today.minusDays(1) + "\", \"pricePerGram\": 12700}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stale").value(true));

        mvc.perform(get("/api/v1/rates/history").param("metal", "XAU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(2)))
                .andExpect(jsonPath("$.points[0].pricePerGram").value(12700))
                .andExpect(jsonPath("$.points[1].source").value("MANUAL"));
    }

    @Test
    void historyChecksTheMetalAndTheRange() throws Exception {
        mvc.perform(get("/api/v1/rates/history").param("metal", "XZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("METAL_NOT_FOUND"));
        mvc.perform(get("/api/v1/rates/history").param("metal", "XAU")
                        .param("from", "2026-05-02").param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        mvc.perform(get("/api/v1/rates/history").param("metal", "XAU")
                        .param("from", "2024-01-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DATE_RANGE_TOO_LONG"));
    }

    @Test
    void premiumsAreListedAndAnUnknownMetalIsRefused() throws Exception {
        mvc.perform(get("/api/v1/rates/premiums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].premiumPercent").value(0));
        mvc.perform(put("/api/v1/rates/premiums/XZZ").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"premiumPercent\": 5}"))
                .andExpect(status().isNotFound());
    }
}
