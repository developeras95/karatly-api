package com.karatly.controller.reference;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.karatly.dto.reference.MetalResponse;
import com.karatly.dto.reference.WeightUnitResponse;
import com.karatly.exception.NotFoundException;
import com.karatly.service.reference.ReferenceDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer test: only the controller, JSON handling, validation and the error handler are
 * loaded. The service is a mock, so no database is needed.
 */
@WebMvcTest(ReferenceDataController.class)
class ReferenceDataControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReferenceDataService service;

    @Test
    void listsMetals() throws Exception {
        when(service.listMetals()).thenReturn(
                List.of(new MetalResponse("XAU", "Gold"), new MetalResponse("XAG", "Silver")));

        mvc.perform(get("/api/v1/metals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("XAU"));
    }

    @Test
    void listsUnits() throws Exception {
        when(service.listUnits()).thenReturn(
                List.of(new WeightUnitResponse("TOLA", "Tola", "tola", new BigDecimal("11.6638038"))));

        mvc.perform(get("/api/v1/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gramsPerUnit").value(11.6638038));
    }

    @Test
    void unknownMetalReturns404Problem() throws Exception {
        when(service.getMetal("XZZ")).thenThrow(new NotFoundException("METAL_NOT_FOUND", "Metal XZZ was not found."));

        mvc.perform(get("/api/v1/metals/XZZ"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METAL_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Metal XZZ was not found."));
    }

    @Test
    void badCodeReturns400WithTheParameterNamed() throws Exception {
        mvc.perform(get("/api/v1/metals/gold"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("code"));
    }

    @Test
    void unexpectedErrorReturns500WithoutDetails() throws Exception {
        when(service.listMetals()).thenThrow(new IllegalStateException("database password is wrong"));

        mvc.perform(get("/api/v1/metals"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side. Please try again later."));
    }
}
