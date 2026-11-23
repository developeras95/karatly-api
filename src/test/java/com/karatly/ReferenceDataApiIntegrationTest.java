package com.karatly;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end tests for reference data: HTTP request, service and a real PostgreSQL. */
@AutoConfigureMockMvc
class ReferenceDataApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void metalsAreListedInDisplayOrder() throws Exception {
        mvc.perform(get("/api/v1/metals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("XAU", "XAG", "XPT", "XPD", "XCU")));
    }

    @Test
    void metalIsFoundWhateverTheCase() throws Exception {
        mvc.perform(get("/api/v1/metals/xau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gold"));
    }

    @Test
    void unknownMetalReturns404() throws Exception {
        mvc.perform(get("/api/v1/metals/XZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("METAL_NOT_FOUND"));
    }

    @Test
    void unitsCarryTheirSizeInGrams() throws Exception {
        mvc.perform(get("/api/v1/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("G", "MG", "KG", "OZT", "TOLA")))
                .andExpect(jsonPath("$[?(@.code == 'TOLA')].gramsPerUnit").value(11.6638038))
                .andExpect(jsonPath("$[?(@.code == 'KG')].gramsPerUnit").value(1000));
    }

    @Test
    void puritiesAreListedPerMetal() throws Exception {
        mvc.perform(get("/api/v1/purities").param("metal", "xau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label", contains("24K (999)", "24K (995)", "22K (916)", "18K (750)", "14K (585)")))
                .andExpect(jsonPath("$[2].fineness").value(0.916));
        mvc.perform(get("/api/v1/purities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
    }
}
