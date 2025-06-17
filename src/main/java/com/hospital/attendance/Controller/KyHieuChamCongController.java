package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.KyHieuChamCong;
import com.hospital.attendance.Service.KyHieuChamCongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ky-hieu-cham-cong")
public class KyHieuChamCongController {

    @Autowired
    private KyHieuChamCongService kyHieuChamCongService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<KyHieuChamCong>> getAllKyHieuChamCong() {
        List<KyHieuChamCong> kyHieuChamCongs = kyHieuChamCongService.getAllKyHieuChamCong();
        return ResponseEntity.ok(kyHieuChamCongs);
    }
}