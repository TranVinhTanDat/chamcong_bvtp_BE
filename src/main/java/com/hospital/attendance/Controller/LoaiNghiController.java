package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.LoaiNghi;
import com.hospital.attendance.Service.LoaiNghiService; // Giả định có service này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/loai-nghi")
public class LoaiNghiController {

    @Autowired
    private LoaiNghiService loaiNghiService; // Giả định service này tồn tại

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<LoaiNghi>> getAllLoaiNghi() {
        List<LoaiNghi> loaiNghis = loaiNghiService.getAllLoaiNghi();
        return ResponseEntity.ok(loaiNghis);
    }
}