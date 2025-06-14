package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {
    Optional<NhanVien> findByEmail(String email);
    Optional<NhanVien> findByHoTen(String hoTen);
    List<NhanVien> findByKhoaPhongId(Long khoaPhongId);
    Optional<NhanVien> findByMaNV(String maNV);

    @Query("SELECT n FROM NhanVien n WHERE (:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId)")
    Page<NhanVien> findByKhoaPhongId(@Param("khoaPhongId") Long khoaPhongId, Pageable pageable);
}