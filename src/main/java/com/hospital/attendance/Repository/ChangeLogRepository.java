package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.ChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    // Tìm kiếm theo nhiều tiêu chí
    @Query("SELECT c FROM ChangeLog c WHERE " +
            "(:tieuDe IS NULL OR LOWER(c.tieuDe) LIKE LOWER(CONCAT('%', :tieuDe, '%'))) AND " +
            "(:loaiThayDoi IS NULL OR c.loaiThayDoi = :loaiThayDoi) AND " +
            "(:trangThai IS NULL OR c.trangThai = :trangThai) AND " +
            "(:version IS NULL OR c.version = :version) AND " +
            "(:tuNgay IS NULL OR c.ngayTao >= :tuNgay) AND " +
            "(:denNgay IS NULL OR c.ngayTao <= :denNgay)")
    Page<ChangeLog> searchChangeLogs(@Param("tieuDe") String tieuDe,
                                     @Param("loaiThayDoi") String loaiThayDoi,
                                     @Param("trangThai") String trangThai,
                                     @Param("version") String version,
                                     @Param("tuNgay") Date tuNgay,
                                     @Param("denNgay") Date denNgay,
                                     Pageable pageable);

    // Lấy version mới nhất
    @Query("SELECT MAX(c.version) FROM ChangeLog c")
    Optional<String> findLatestVersion();

    // Lấy danh sách theo loại thay đổi
    List<ChangeLog> findByLoaiThayDoiOrderByNgayTaoDesc(String loaiThayDoi);

    // Lấy danh sách theo trạng thái
    List<ChangeLog> findByTrangThaiOrderByNgayTaoDesc(String trangThai);

    // Lấy danh sách theo version
    List<ChangeLog> findByVersionOrderByNgayTaoDesc(String version);

    // Lấy danh sách theo người tạo
    @Query("SELECT c FROM ChangeLog c WHERE c.nguoiTao.id = :nguoiTaoId ORDER BY c.ngayTao DESC")
    List<ChangeLog> findByNguoiTaoId(@Param("nguoiTaoId") Long nguoiTaoId);

    // Thống kê theo loại thay đổi
    @Query("SELECT c.loaiThayDoi, COUNT(c) FROM ChangeLog c GROUP BY c.loaiThayDoi")
    List<Object[]> getStatisticsByLoaiThayDoi();

    // Thống kê theo trạng thái
    @Query("SELECT c.trangThai, COUNT(c) FROM ChangeLog c GROUP BY c.trangThai")
    List<Object[]> getStatisticsByTrangThai();
}