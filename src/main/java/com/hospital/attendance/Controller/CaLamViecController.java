package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Service.CaLamViecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ca-lam-viec")
public class CaLamViecController {

    @Autowired
    private CaLamViecService caLamViecService;

    @GetMapping
    public ResponseEntity<List<CaLamViec>> getAllCaLamViec() {
        List<CaLamViec> caLamViecs = caLamViecService.getAllCaLamViec();
        return ResponseEntity.ok(caLamViecs);
    }
}