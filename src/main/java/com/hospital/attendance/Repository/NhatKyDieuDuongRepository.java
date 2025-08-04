package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.NhatKyDieuDuong;
import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NhatKyDieuDuongRepository extends JpaRepository<NhatKyDieuDuong, Long> {

    // *** TÌM KIẾM THEO CÁC TIÊU CHÍ CƠ BẢN ***
    Optional<NhatKyDieuDuong> findByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
            LocalDate ngay, Long khoaPhongId, LoaiMauNhatKy loaiMau, Integer trangThai);

    List<NhatKyDieuDuong> findByKhoaPhongIdAndTrangThaiOrderByNgayDesc(Long khoaPhongId, Integer trangThai);

    List<NhatKyDieuDuong> findByKhoaPhongIdAndLoaiMauAndTrangThaiOrderByNgayDesc(
            Long khoaPhongId, LoaiMauNhatKy loaiMau, Integer trangThai);

    Optional<NhatKyDieuDuong> findByIdAndTrangThai(Long id, Integer trangThai);

    // *** TÌM KIẾM THEO KHOẢNG THỜI GIAN ***
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "(:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId) AND " +
            "(:loaiMau IS NULL OR n.loaiMau = :loaiMau) AND " +
            "n.ngay BETWEEN :tuNgay AND :denNgay AND " +
            "n.trangThai = :trangThai " +
            "ORDER BY n.ngay DESC")
    Page<NhatKyDieuDuong> findByDateRangeAndFilters(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("tuNgay") LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay,
            @Param("trangThai") Integer trangThai,
            Pageable pageable);

    // *** TÌM KIẾM VỚI PAGINATION ***
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "(:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId) AND " +
            "(:loaiMau IS NULL OR n.loaiMau = :loaiMau) AND " +
            "n.trangThai = :trangThai " +
            "ORDER BY n.ngay DESC")
    Page<NhatKyDieuDuong> findByFiltersWithPagination(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("trangThai") Integer trangThai,
            Pageable pageable);

    // *** TÌM KIẾM THEO THÁNG ***
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "(:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId) AND " +
            "(:loaiMau IS NULL OR n.loaiMau = :loaiMau) AND " +
            "EXTRACT(YEAR FROM n.ngay) = :nam AND " +
            "EXTRACT(MONTH FROM n.ngay) = :thang AND " +
            "n.trangThai = :trangThai " +
            "ORDER BY n.ngay DESC")
    List<NhatKyDieuDuong> findByThangNamAndFilters(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("thang") Integer thang,
            @Param("nam") Integer nam,
            @Param("trangThai") Integer trangThai);

    // *** THỐNG KÊ THEO THÁNG ***
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "n.khoaPhong.id = :khoaPhongId AND " +
            "n.loaiMau = :loaiMau AND " +
            "EXTRACT(YEAR FROM n.ngay) = :nam AND " +
            "EXTRACT(MONTH FROM n.ngay) = :thang AND " +
            "n.trangThai = :trangThai " +
            "ORDER BY n.ngay ASC")
    List<NhatKyDieuDuong> findForMonthlyReport(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("thang") Integer thang,
            @Param("nam") Integer nam,
            @Param("trangThai") Integer trangThai);

    // *** KIỂM TRA SỰ TỒN TẠI ***
    boolean existsByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
            LocalDate ngay, Long khoaPhongId, LoaiMauNhatKy loaiMau, Integer trangThai);

    // *** TÌM KIẾM THEO NGƯỜI TẠO ***
    List<NhatKyDieuDuong> findByNguoiTaoAndTrangThaiOrderByNgayDesc(String nguoiTao, Integer trangThai);

    // *** TÌM BẢN GHI GẦN NHẤT ***
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "n.khoaPhong.id = :khoaPhongId AND " +
            "n.loaiMau = :loaiMau AND " +
            "n.trangThai = :trangThai " +
            "ORDER BY n.ngay DESC")
    List<NhatKyDieuDuong> findLatestByKhoaPhongAndLoaiMau(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("trangThai") Integer trangThai,
            Pageable pageable);

    // *** XÓA MỀM (CẬP NHẬT TRẠNG THÁI) ***
    @Query("UPDATE NhatKyDieuDuong n SET n.trangThai = 0, n.ngayCapNhat = CURRENT_TIMESTAMP, n.nguoiCapNhat = :nguoiCapNhat WHERE n.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("nguoiCapNhat") String nguoiCapNhat);

    // *** THỐNG KÊ TỔNG QUAN ***
    @Query("SELECT COUNT(n) FROM NhatKyDieuDuong n WHERE " +
            "(:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId) AND " +
            "n.trangThai = :trangThai")
    long countByKhoaPhongAndTrangThai(@Param("khoaPhongId") Long khoaPhongId, @Param("trangThai") Integer trangThai);

    @Query("SELECT COUNT(n) FROM NhatKyDieuDuong n WHERE " +
            "n.khoaPhong.id = :khoaPhongId AND " +
            "n.loaiMau = :loaiMau AND " +
            "EXTRACT(YEAR FROM n.ngay) = :nam AND " +
            "EXTRACT(MONTH FROM n.ngay) = :thang AND " +
            "n.trangThai = :trangThai")
    long countByKhoaPhongAndLoaiMauAndThangNam(
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau,
            @Param("thang") Integer thang,
            @Param("nam") Integer nam,
            @Param("trangThai") Integer trangThai);

    /**
     * Tìm tất cả bản ghi (bao gồm đã xóa) theo ngày, khoa phòng, loại mẫu
     */
    @Query("SELECT n FROM NhatKyDieuDuong n WHERE " +
            "n.ngay = :ngay AND " +
            "n.khoaPhong.id = :khoaPhongId AND " +
            "n.loaiMau = :loaiMau " +
            "ORDER BY n.trangThai DESC, n.ngayTao DESC")
    List<NhatKyDieuDuong> findAllByNgayAndKhoaPhongIdAndLoaiMau(
            @Param("ngay") LocalDate ngay,
            @Param("khoaPhongId") Long khoaPhongId,
            @Param("loaiMau") LoaiMauNhatKy loaiMau);

    }


