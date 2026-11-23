package com.karatly.repository.reference;

import java.util.List;

import com.karatly.entity.reference.Purity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurityRepository extends JpaRepository<Purity, Short> {

    @Query("SELECT p FROM Purity p WHERE p.active = true ORDER BY p.metalCode, p.sortOrder")
    List<Purity> findAllActive();

    @Query("SELECT p FROM Purity p WHERE p.active = true AND p.metalCode = :metalCode ORDER BY p.sortOrder")
    List<Purity> findActiveByMetal(String metalCode);
}
