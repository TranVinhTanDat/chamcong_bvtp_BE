package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.LoaiNghi;
import com.hospital.attendance.Repository.LoaiNghiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoaiNghiService {

    @Autowired
    private LoaiNghiRepository loaiNghiRepository;

    public List<LoaiNghi> getAllLoaiNghi() {
        return loaiNghiRepository.findAll();
    }
}