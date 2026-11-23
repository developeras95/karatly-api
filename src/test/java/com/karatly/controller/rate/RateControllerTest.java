package com.karatly.controller.rate;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karatly.service.rate.RateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Validation of rate requests; the service is a mock and must not be reached for invalid input. */
@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RateService service;

    @Test
    void aManualRateNeedsAMetalAndAPositivePrice() throws Exception {
        mvc.perform(post("/api/v1/rates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metal\": \"gold\", \"pricePerGram\": -5, \"date\": \"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("metal", "pricePerGram", "date")));

        verifyNoInteractions(service);
    }

    @Test
    void aPremiumMustBeBetweenZeroAndAHundred() throws Exception {
        mvc.perform(put("/api/v1/rates/premiums/XAU").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"premiumPercent\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("premiumPercent"));
    }

    @Test
    void historyNeedsAValidMetalCode() throws Exception {
        mvc.perform(get("/api/v1/rates/history").param("metal", "gold"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("metal"));
    }

    @Test
    void refreshIsForcedUnlessToldOtherwise() throws Exception {
        org.mockito.Mockito.when(service.refresh(anyBoolean())).thenReturn(java.util.List.of());

        mvc.perform(post("/api/v1/rates/refresh")).andExpect(status().isOk());

        org.mockito.Mockito.verify(service).refresh(true);
    }
}
