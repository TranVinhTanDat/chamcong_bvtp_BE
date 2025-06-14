package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "loai_nghi")
public class LoaiNghi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_loai_nghi", nullable = false, unique = true)
    private String maLoaiNghi;

    @Column(name = "ten_loai_nghi", nullable = false)
    private String tenLoaiNghi;

    @Column(name = "trang_thai", nullable = false)
    private boolean trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;

    public LoaiNghi(String maLoaiNghi, String tenLoaiNghi, boolean trangThai, String ghiChu) {
        this.maLoaiNghi = maLoaiNghi;
        this.tenLoaiNghi = tenLoaiNghi;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }
}