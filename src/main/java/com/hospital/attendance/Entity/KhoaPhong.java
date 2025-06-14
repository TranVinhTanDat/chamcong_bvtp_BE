package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "khoa_phong")
public class KhoaPhong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_khoa_phong", nullable = false, unique = true)
    private String tenKhoaPhong;

    @Column(name = "ma_khoa_phong", nullable = false, unique = true, length = 50)
    private String maKhoaPhong;

    public KhoaPhong(String tenKhoaPhong, String maKhoaPhong) {
        this.tenKhoaPhong = tenKhoaPhong;
        this.maKhoaPhong = maKhoaPhong;
    }
}