// *** 5. CẬP NHẬT KhoaPhongRepository.java ***

package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.KhoaPhong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhoaPhongRepository extends JpaRepository<KhoaPhong, Long> {

    // *** CÁC METHOD VALIDATION ***
    Optional<KhoaPhong> findByTenKhoaPhong(String tenKhoaPhong);
    Optional<KhoaPhong> findByMaKhoaPhong(String maKhoaPhong);

    // *** METHOD TÌM KIẾM VỚI PAGINATION ***
    @Query("SELECT kp FROM KhoaPhong kp WHERE " +
            "LOWER(kp.tenKhoaPhong) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(kp.maKhoaPhong) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<KhoaPhong> findAllByTenKhoaPhongContainingIgnoreCaseOrMaKhoaPhongContainingIgnoreCase(
            @Param("searchTerm") String searchTerm1,
            @Param("searchTerm") String searchTerm2,
            Pageable pageable);

    // *** THÊM MỚI: Method kiểm tra sự tồn tại ***
    boolean existsByTenKhoaPhong(String tenKhoaPhong);
    boolean existsByMaKhoaPhong(String maKhoaPhong);

    // *** THÊM MỚI: Method đếm nhân viên trong khoa phòng ***
    @Query("SELECT COUNT(nv) FROM NhanVien nv WHERE nv.khoaPhong.id = :khoaPhongId AND nv.trangThai = :trangThai")
    long countActiveEmployeesByKhoaPhongId(@Param("khoaPhongId") Long khoaPhongId, @Param("trangThai") Integer trangThai);
}