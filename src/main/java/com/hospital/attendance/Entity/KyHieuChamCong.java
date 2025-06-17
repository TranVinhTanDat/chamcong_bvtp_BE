package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ky_hieu_cham_cong")
public class KyHieuChamCong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ky_hieu", nullable = false, unique = true)
    private String maKyHieu;

    @Column(name = "ten_ky_hieu", nullable = false)
    private String tenKyHieu;

    @Column(name = "trang_thai", nullable = false)
    private boolean trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;

    public KyHieuChamCong(String maKyHieu, String tenKyHieu, boolean trangThai, String ghiChu) {
        this.maKyHieu = maKyHieu;
        this.tenKyHieu = tenKyHieu;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }
}