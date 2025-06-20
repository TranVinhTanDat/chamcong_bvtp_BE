package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.ChucVu;
import com.hospital.attendance.Service.ChucVuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chuc-vu")
public class ChucVuController {

    @Autowired
    private ChucVuService chucVuService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<ChucVu>> getAllChucVus() {
        List<ChucVu> chucVus = chucVuService.getAllChucVus();
        return ResponseEntity.ok(chucVus);
    }
}