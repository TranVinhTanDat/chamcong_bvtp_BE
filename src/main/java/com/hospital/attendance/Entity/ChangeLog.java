package com.hospital.attendance.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "change_log")
public class ChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "loai_thay_doi", nullable = false)
    private String loaiThayDoi;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "muc_do_quan_trong")
    private String mucDoQuanTrong;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private Date ngayTao;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_cap_nhat")
    private Date ngayCapNhat;

    @ManyToOne
    @JoinColumn(name = "nguoi_tao_id", nullable = false)
    private User nguoiTao;

    @ManyToOne
    @JoinColumn(name = "nguoi_cap_nhat_id")
    private User nguoiCapNhat;

    @Column(name = "trang_thai")
    private String trangThai;

    @OneToMany(mappedBy = "changeLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChangeLogFile> files;

    @PrePersist
    public void prePersist() {
        if (this.ngayTao == null) this.ngayTao = new Date();
        if (this.trangThai == null) this.trangThai = "DANG_TRIEN_KHAI";
    }

    @PreUpdate
    public void preUpdate() {
        this.ngayCapNhat = new Date();
    }
}