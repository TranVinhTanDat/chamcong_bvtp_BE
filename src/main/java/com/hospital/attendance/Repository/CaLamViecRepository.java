package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {
    Optional<CaLamViec> findById(Long id);
}