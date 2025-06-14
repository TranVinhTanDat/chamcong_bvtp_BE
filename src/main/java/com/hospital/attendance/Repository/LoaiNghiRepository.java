package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.LoaiNghi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoaiNghiRepository extends JpaRepository<LoaiNghi, Long> {
    Optional<LoaiNghi> findByMaLoaiNghi(String maLoaiNghi);
}