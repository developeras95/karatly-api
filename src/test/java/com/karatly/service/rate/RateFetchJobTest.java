package com.karatly.service.rate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.karatly.dto.rate.RefreshResult;
import com.karatly.entity.rate.RateSource;
import org.junit.jupiter.api.Test;

class RateFetchJobTest {

    @Test
    void fetchesWhatIsDueAndCarriesOnWhenAProviderFails() {
        RateService service = mock(RateService.class);
        when(service.refreshIfDue()).thenReturn(List.of(
                new RefreshResult(RateSource.GOLDPRICE_DEV, RefreshResult.Status.FETCHED, List.of("XAU"), null),
                new RefreshResult(RateSource.GOLDAPI_IO, RefreshResult.Status.FAILED, List.of("XPT"), "down"),
                new RefreshResult(RateSource.GOLDAPI_IO, RefreshResult.Status.OVER_BUDGET, List.of("XPT"), "used up")));

        new RateFetchJob(service).run();

        verify(service).refreshIfDue();
    }
}
