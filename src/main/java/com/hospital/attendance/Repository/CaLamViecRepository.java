package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Entity.KyHieuChamCong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {
    Optional<CaLamViec> findById(Long id);

    @Query("SELECT c FROM CaLamViec c WHERE (:search IS NULL OR c.tenCaLamViec LIKE %:search%)")
    Page<CaLamViec> findBySearch(@Param("search") String search, Pageable pageable);

    Optional<CaLamViec> findByKyHieuChamCong(KyHieuChamCong kyHieuChamCong);

}