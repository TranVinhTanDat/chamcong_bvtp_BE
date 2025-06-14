package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "chuc_vu")
public class ChucVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_chuc_vu", nullable = false, unique = true)
    private String tenChucVu;

    public ChucVu(String tenChucVu) {
        this.tenChucVu = tenChucVu;
    }
}