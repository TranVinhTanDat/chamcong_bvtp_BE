package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.MoKhoaChamCong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MoKhoaChamCongRepository extends JpaRepository<MoKhoaChamCong, Long> {

    // Kiểm tra khoa phòng có được mở khóa cho ngày cụ thể không
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "AND m.trangThai = true " +
            "AND :ngayKiemTra BETWEEN m.tuNgay AND m.denNgay")
    List<MoKhoaChamCong> findActiveUnlockForKhoaPhongAndDate(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("ngayKiemTra") Date ngayKiemTra);

    // Lấy danh sách tất cả mở khóa của khoa phòng
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "ORDER BY m.ngayTao DESC")
    Page<MoKhoaChamCong> findByKhoaPhongId(@Param("khoaPhongId") Long khoaPhongId, Pageable pageable);

    // Kiểm tra trùng lặp khoảng thời gian mở khóa
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "AND m.trangThai = true " +
            "AND ((:tuNgay BETWEEN m.tuNgay AND m.denNgay) " +
            "OR (:denNgay BETWEEN m.tuNgay AND m.denNgay) " +
            "OR (m.tuNgay BETWEEN :tuNgay AND :denNgay))")
    List<MoKhoaChamCong> findOverlappingUnlocks(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("tuNgay") Date tuNgay,
            @Param("denNgay") Date denNgay);


    // Lấy danh sách mở khóa đang hoạt động
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.trangThai = true " +
            "ORDER BY m.ngayTao DESC")
    Page<MoKhoaChamCong> findActiveMoKhoa(Pageable pageable);

    // Tìm theo ID và trạng thái
    Optional<MoKhoaChamCong> findByIdAndTrangThai(Long id, Boolean trangThai);


    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "AND m.trangThai = true " +
            "AND :hienTai BETWEEN m.tuNgay AND m.denNgay " +
            "ORDER BY m.ngayTao DESC")
    List<MoKhoaChamCong> findCurrentActiveUnlocks(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("hienTai") Date hienTai);

    // *** SỬA LẠI: Lấy TẤT CẢ mở khóa đang hoạt động, không giới hạn thời gian ***
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "AND m.trangThai = true " +
            "ORDER BY m.ngayTao DESC")
    List<MoKhoaChamCong> findCurrentActiveUnlocksForKhoaPhong(@Param("khoaPhongId") Long khoaPhongId);

    // *** THÊM MỚI: Kiểm tra trùng lặp khoảng thời gian mở khóa (với exclusion) ***
    @Query("SELECT m FROM MoKhoaChamCong m WHERE m.khoaPhong.id = :khoaPhongId " +
            "AND m.trangThai = true " +
            "AND (:excludeId IS NULL OR m.id != :excludeId) " +
            "AND ((DATE(:tuNgay) BETWEEN DATE(m.tuNgay) AND DATE(m.denNgay)) " +
            "OR (DATE(:denNgay) BETWEEN DATE(m.tuNgay) AND DATE(m.denNgay)) " +
            "OR (DATE(m.tuNgay) BETWEEN DATE(:tuNgay) AND DATE(:denNgay)))")
    List<MoKhoaChamCong> findOverlappingUnlocksWithExclusion(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("tuNgay") Date tuNgay,
            @Param("denNgay") Date denNgay,
            @Param("excludeId") Long excludeId);
}