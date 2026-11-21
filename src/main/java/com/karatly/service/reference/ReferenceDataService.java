package com.karatly.service.reference;

import java.util.List;
import java.util.Locale;

import com.karatly.dto.reference.MetalResponse;
import com.karatly.dto.reference.WeightUnitResponse;
import com.karatly.exception.NotFoundException;
import com.karatly.repository.reference.MetalRepository;
import com.karatly.repository.reference.WeightUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to reference data: metals and weight units. */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final MetalRepository metals;
    private final WeightUnitRepository units;

    public ReferenceDataService(MetalRepository metals, WeightUnitRepository units) {
        this.metals = metals;
        this.units = units;
    }

    /** Active metals in display order (gold, silver, platinum, palladium). */
    public List<MetalResponse> listMetals() {
        return metals.findByActiveTrueOrderBySortOrderAsc().stream().map(MetalResponse::from).toList();
    }

    /** One metal by its code, case-insensitive ("xau" finds gold). */
    public MetalResponse getMetal(String code) {
        String normalised = code.trim().toUpperCase(Locale.ROOT);
        return metals.findById(normalised)
                .map(MetalResponse::from)
                .orElseThrow(() -> new NotFoundException("METAL_NOT_FOUND", "Metal " + normalised + " was not found."));
    }

    /** Active weight units in display order. */
    public List<WeightUnitResponse> listUnits() {
        return units.findByActiveTrueOrderBySortOrderAsc().stream().map(WeightUnitResponse::from).toList();
    }
}
