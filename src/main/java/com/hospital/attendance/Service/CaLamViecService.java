package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Repository.CaLamViecRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CaLamViecService {

    @Autowired
    private CaLamViecRepository caLamViecRepository;

    public List<CaLamViec> getAllCaLamViec() {
        return caLamViecRepository.findAll();
    }
}