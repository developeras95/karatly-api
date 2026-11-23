package com.karatly.controller.reference;

import java.util.List;

import com.karatly.dto.reference.MetalResponse;
import com.karatly.dto.reference.PurityResponse;
import com.karatly.dto.reference.WeightUnitResponse;
import com.karatly.service.reference.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reference data", description = "Metals, purities and weight units")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/metals")
    @Operation(summary = "List metals", description = "Active metals in display order.")
    public List<MetalResponse> listMetals() {
        return service.listMetals();
    }

    @GetMapping("/metals/{code}")
    @Operation(summary = "Get one metal by its ISO 4217 code", description = "For example XAU for gold.")
    public MetalResponse getMetal(
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three-letter code such as XAU")
            String code) {
        return service.getMetal(code);
    }

    @GetMapping("/units")
    @Operation(summary = "List weight units", description = "Every weight is stored in grams; "
            + "gramsPerUnit converts from the unit to grams.")
    public List<WeightUnitResponse> listUnits() {
        return service.listUnits();
    }

    @GetMapping("/purities")
    @Operation(summary = "List purities", description = "How much pure metal an item holds, e.g. 22K gold = 0.916. "
            + "Filter by metal, or leave it empty for all.")
    public List<PurityResponse> listPurities(
            @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three-letter code such as XAU")
            String metal) {
        return service.listPurities(metal);
    }
}
