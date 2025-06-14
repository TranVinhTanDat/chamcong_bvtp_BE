package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {
    Optional<NhanVien> findByEmail(String email);
    Optional<NhanVien> findByHoTen(String hoTen);
    List<NhanVien> findByKhoaPhongId(Long khoaPhongId);
    Optional<NhanVien> findByMaNV(String maNV); // Tìm theo mã nhân viên
}