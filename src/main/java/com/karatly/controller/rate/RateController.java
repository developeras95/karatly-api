package com.karatly.controller.rate;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import com.karatly.dto.rate.ManualRateRequest;
import com.karatly.dto.rate.PremiumRequest;
import com.karatly.dto.rate.PremiumResponse;
import com.karatly.dto.rate.ProviderStatusResponse;
import com.karatly.dto.rate.RateHistoryResponse;
import com.karatly.dto.rate.RateResponse;
import com.karatly.dto.rate.RefreshResult;
import com.karatly.service.rate.RateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Rates", description = "Metal rates per gram: latest, history, manual entry, premiums and providers")
public class RateController {

    private static final String METAL_CODE = "^[A-Za-z]{3}$";
    private static final String METAL_CODE_MESSAGE = "must be a three-letter code such as XAU";

    private final RateService service;

    public RateController(RateService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    @Operation(summary = "Latest rate of every metal",
            description = "Per gram of pure metal, plus the rate for each purity. Served from Karatly's own store; "
                    + "providers that are due are fetched first.")
    public List<RateResponse> latest() {
        return service.latest();
    }

    @GetMapping("/history")
    @Operation(summary = "Rate history of one metal",
            description = "One rate per day, oldest first. Defaults to the last 30 days; at most 366 days.")
    public RateHistoryResponse history(
            @Parameter(example = "XAU") @RequestParam @Pattern(regexp = METAL_CODE, message = METAL_CODE_MESSAGE) String metal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.history(metal, from, to);
    }

    @PostMapping
    @Operation(summary = "Enter a rate by hand",
            description = "For example the day's IBJA rate. Wins over fetched rates for that day.")
    public ResponseEntity<RateResponse> addManual(@Valid @RequestBody ManualRateRequest request) {
        RateResponse created = service.addManual(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rates/history")
                .queryParam("metal", created.metal())
                .queryParam("from", created.date())
                .queryParam("to", created.date())
                .build().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Fetch rates now",
            description = "force=true (default) calls every available provider even if it is not due; "
                    + "force=false only those that are. Monthly budgets always apply.")
    public List<RefreshResult> refresh(@RequestParam(defaultValue = "true") boolean force) {
        return service.refresh(force);
    }

    @GetMapping("/providers")
    @Operation(summary = "Rate providers", description = "Settings, requests used this month and next fetch time.")
    public List<ProviderStatusResponse> providers() {
        return service.providerStatus();
    }

    @GetMapping("/premiums")
    @Operation(summary = "Premium over spot per metal", description = "Percent added to the international spot "
            + "price when a rate is fetched, e.g. import duty.")
    public List<PremiumResponse> premiums() {
        return service.listPremiums();
    }

    @PutMapping("/premiums/{metal}")
    @Operation(summary = "Change a metal's premium", description = "Applies from the next fetch; stored rates keep "
            + "the premium they were fetched with.")
    public PremiumResponse updatePremium(
            @PathVariable @Pattern(regexp = METAL_CODE, message = METAL_CODE_MESSAGE) String metal,
            @Valid @RequestBody PremiumRequest request) {
        return service.updatePremium(metal, request.premiumPercent());
    }
}
