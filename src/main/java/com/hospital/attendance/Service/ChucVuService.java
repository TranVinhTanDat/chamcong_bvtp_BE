package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.ChucVu;
import com.hospital.attendance.Repository.ChucVuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChucVuService {

    @Autowired
    private ChucVuRepository chucVuRepository;

    public List<ChucVu> getAllChucVus() {
        return chucVuRepository.findAll();
    }
}