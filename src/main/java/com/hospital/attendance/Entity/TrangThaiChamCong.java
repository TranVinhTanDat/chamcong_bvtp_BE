package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "trang_thai_cham_cong")
public class TrangThaiChamCong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_trang_thai", nullable = false, unique = true)
    private String tenTrangThai;

    public TrangThaiChamCong(String tenTrangThai) {
        this.tenTrangThai = tenTrangThai;
    }
}