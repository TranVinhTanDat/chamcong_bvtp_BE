package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KyHieuChamCong;
import com.hospital.attendance.Repository.KyHieuChamCongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KyHieuChamCongService {

    @Autowired
    private KyHieuChamCongRepository kyHieuChamCongRepository;

    public List<KyHieuChamCong> getAllKyHieuChamCong() {
        return kyHieuChamCongRepository.findAll();
    }
}