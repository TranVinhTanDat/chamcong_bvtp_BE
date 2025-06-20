package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.KyHieuChamCong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KyHieuChamCongRepository extends JpaRepository<KyHieuChamCong, Long> {
    Optional<KyHieuChamCong> findByMaKyHieu(String maKyHieu);

    @Query("SELECT k FROM KyHieuChamCong k WHERE " +
            "(:search IS NULL OR k.maKyHieu LIKE %:search% OR k.tenKyHieu LIKE %:search%)")
    Page<KyHieuChamCong> findAllWithSearch(@Param("search") String search, Pageable pageable);
}