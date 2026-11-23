package com.karatly.repository.rate;

import java.util.List;

import com.karatly.entity.rate.RatePremium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePremiumRepository extends JpaRepository<RatePremium, String> {

    List<RatePremium> findAllByOrderByMetalCodeAsc();
}
