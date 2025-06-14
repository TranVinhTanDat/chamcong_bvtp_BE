package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhoaPhongService {

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    public List<KhoaPhong> getAllKhoaPhongs() {
        return khoaPhongRepository.findAll();
    }
}