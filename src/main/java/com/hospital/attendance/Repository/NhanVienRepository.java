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
    Optional<NhanVien> findByEmailAndTrangThai(String email, Integer trangThai);
    Optional<NhanVien> findByHoTenAndTrangThai(String hoTen, Integer trangThai);
    Optional<NhanVien> findByMaNVAndTrangThai(String maNV, Integer trangThai);
    List<NhanVien> findByKhoaPhongIdAndTrangThai(Long khoaPhongId, Integer trangThai);

    @Query("SELECT n FROM NhanVien n WHERE (:khoaPhongId IS NULL OR n.khoaPhong.id = :khoaPhongId) AND n.trangThai = :trangThai")
    Page<NhanVien> findByKhoaPhongIdAndTrangThai(@Param("khoaPhongId") Long khoaPhongId, @Param("trangThai") Integer trangThai, Pageable pageable);

    Optional<NhanVien> findByIdAndTrangThai(Long id, Integer trangThai);
}