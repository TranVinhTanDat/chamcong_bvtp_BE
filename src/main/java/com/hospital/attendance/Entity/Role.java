package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ten_vai_tro"})
})
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_vai_tro", unique = true, nullable = false)
    private String tenVaiTro;

    public Role(String tenVaiTro) {
        this.tenVaiTro = tenVaiTro;
    }
}