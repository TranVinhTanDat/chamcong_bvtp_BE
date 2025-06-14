package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {
    List<ChamCong> findByNhanVienMa(Long ma);
    Optional<ChamCong> findByNhanVienAndThoiGianCheckInBetween(NhanVien nhanVien, Date start, Date end);
    List<ChamCong> findByNhanVienKhoaPhongId(Long khoaPhongId);
}