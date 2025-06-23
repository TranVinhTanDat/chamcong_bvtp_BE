package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Entity.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {
    List<ChamCong> findByNhanVienId(Long id);
    Optional<ChamCong> findByNhanVienAndThoiGianCheckInBetween(NhanVien nhanVien, Date start, Date end);

    // Kiểm tra trùng lặp ca làm việc cụ thể trong ngày
    @Query("SELECT c FROM ChamCong c WHERE c.nhanVien = :nhanVien AND c.caLamViec.id = :caLamViecId " +
            "AND c.thoiGianCheckIn BETWEEN :start AND :end")
    Optional<ChamCong> findByNhanVienAndCaLamViecAndThoiGianCheckInBetween(
            @Param("nhanVien") NhanVien nhanVien,
            @Param("caLamViecId") Long caLamViecId,
            @Param("start") Date start,
            @Param("end") Date end);

    // THÊM MỚI: Đếm tổng số lần chấm công của nhân viên trong ngày
    @Query("SELECT COUNT(c) FROM ChamCong c WHERE c.nhanVien = :nhanVien " +
            "AND c.thoiGianCheckIn BETWEEN :start AND :end")
    Long countByNhanVienAndThoiGianCheckInBetween(
            @Param("nhanVien") NhanVien nhanVien,
            @Param("start") Date start,
            @Param("end") Date end);

    // THÊM MỚI: Lấy danh sách chấm công của nhân viên trong ngày (để kiểm tra chi tiết)
    @Query("SELECT c FROM ChamCong c WHERE c.nhanVien = :nhanVien " +
            "AND c.thoiGianCheckIn BETWEEN :start AND :end " +
            "ORDER BY c.thoiGianCheckIn ASC")
    List<ChamCong> findByNhanVienAndDateRange(
            @Param("nhanVien") NhanVien nhanVien,
            @Param("start") Date start,
            @Param("end") Date end);

    @Query("SELECT c FROM ChamCong c WHERE (:khoaPhongId IS NULL OR c.nhanVien.khoaPhong.id = :khoaPhongId) " +
            "AND c.nhanVien.trangThai = 1 " +
            "AND (:year IS NULL OR YEAR(c.thoiGianCheckIn) = :year) " +
            "AND (:month IS NULL OR MONTH(c.thoiGianCheckIn) = :month) " +
            "AND (:day IS NULL OR DAY(c.thoiGianCheckIn) = :day)")
    Page<ChamCong> findByKhoaPhongAndDateFilters(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("day") Integer day,
            Pageable pageable);

    @Query("SELECT c FROM ChamCong c WHERE c.nhanVien.id = :nhanVienId AND c.caLamViec IS NOT NULL " +
            "ORDER BY c.thoiGianCheckIn DESC LIMIT 1")
    Optional<ChamCong> findLatestWithCaLamViecByNhanVienId(@Param("nhanVienId") Long nhanVienId);
}