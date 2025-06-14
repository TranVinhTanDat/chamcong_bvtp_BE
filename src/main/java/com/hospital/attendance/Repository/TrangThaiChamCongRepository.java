package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.TrangThaiChamCong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrangThaiChamCongRepository extends JpaRepository<TrangThaiChamCong, Long> {
    Optional<TrangThaiChamCong> findByTenTrangThai(String tenTrangThai);
}