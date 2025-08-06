package com.hospital.attendance.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "change_log_file")
public class ChangeLogFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_file_goc", nullable = false)
    private String tenFileGoc;

    @Column(name = "ten_file_luu", nullable = false)
    private String tenFileLuu;

    @Column(name = "duong_dan", nullable = false)
    private String duongDan;

    @Column(name = "kich_thuoc")
    private Long kichThuoc;

    @Column(name = "loai_file")
    private String loaiFile;

    @Column(name = "version_file", nullable = false)
    private String versionFile;

    @Column(name = "thu_muc_cha")
    private String thuMucCha;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_upload", nullable = false, updatable = false)
    private Date ngayUpload;

    @JsonIgnore // Thêm này để fix circular reference
    @ManyToOne
    @JoinColumn(name = "change_log_id", nullable = false)
    private ChangeLog changeLog;

    @ManyToOne
    @JoinColumn(name = "nguoi_upload_id", nullable = false)
    private User nguoiUpload;

    @PrePersist
    public void prePersist() {
        if (this.ngayUpload == null) this.ngayUpload = new Date();
    }
}