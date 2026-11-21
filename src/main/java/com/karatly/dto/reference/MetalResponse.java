package com.karatly.dto.reference;

import com.karatly.entity.reference.Metal;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Metal")
public record MetalResponse(
        @Schema(description = "ISO 4217 metal code", example = "XAU") String code,
        @Schema(example = "Gold") String name) {

    public static MetalResponse from(Metal metal) {
        return new MetalResponse(metal.getCode(), metal.getName());
    }
}
