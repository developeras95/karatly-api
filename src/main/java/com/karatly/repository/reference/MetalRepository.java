package com.karatly.repository.reference;

import java.util.List;

import com.karatly.entity.reference.Metal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetalRepository extends JpaRepository<Metal, String> {

    List<Metal> findByActiveTrueOrderBySortOrderAsc();
}
