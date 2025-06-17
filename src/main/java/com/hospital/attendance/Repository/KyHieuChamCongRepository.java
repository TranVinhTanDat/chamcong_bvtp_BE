package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.KyHieuChamCong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KyHieuChamCongRepository extends JpaRepository<KyHieuChamCong, Long> {
    Optional<KyHieuChamCong> findByMaKyHieu(String maKyHieu);
}