package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.KhoaPhong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KhoaPhongRepository extends JpaRepository<KhoaPhong, Long> {
    Optional<KhoaPhong> findByTenKhoaPhong(String tenKhoaPhong);
}