package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.ChangeLogFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChangeLogFileRepository extends JpaRepository<ChangeLogFile, Long> {

    // Lấy danh sách file theo ChangeLog
    List<ChangeLogFile> findByChangeLogIdOrderByNgayUploadDesc(Long changeLogId);

    // Lấy danh sách file theo thư mục cha
    List<ChangeLogFile> findByThuMucChaOrderByVersionFileDesc(String thuMucCha);

    // Lấy version mới nhất của file trong thư mục
    @Query("SELECT MAX(f.versionFile) FROM ChangeLogFile f WHERE f.thuMucCha = :thuMucCha")
    Optional<String> findLatestVersionByThuMucCha(@Param("thuMucCha") String thuMucCha);

    // Kiểm tra file đã tồn tại
    boolean existsByTenFileGocAndThuMucChaAndVersionFile(String tenFileGoc, String thuMucCha, String versionFile);

    // Lấy file theo version và thư mục
    Optional<ChangeLogFile> findByThuMucChaAndVersionFile(String thuMucCha, String versionFile);

    // Đếm số file theo thư mục
    long countByThuMucCha(String thuMucCha);

    // Lấy danh sách thư mục đã có
    @Query("SELECT DISTINCT f.thuMucCha FROM ChangeLogFile f ORDER BY f.thuMucCha")
    List<String> findDistinctThuMucCha();
}