package com.karatly.repository.reference;

import java.util.List;

import com.karatly.entity.reference.WeightUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightUnitRepository extends JpaRepository<WeightUnit, String> {

    List<WeightUnit> findByActiveTrueOrderBySortOrderAsc();
}
