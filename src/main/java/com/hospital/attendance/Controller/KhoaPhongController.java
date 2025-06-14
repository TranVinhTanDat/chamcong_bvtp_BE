package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Service.KhoaPhongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/khoa-phong")
public class KhoaPhongController {

    @Autowired
    private KhoaPhongService khoaPhongService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<KhoaPhong>> getAllKhoaPhongs() {
        List<KhoaPhong> khoaPhongs = khoaPhongService.getAllKhoaPhongs();
        return ResponseEntity.ok(khoaPhongs);
    }
}