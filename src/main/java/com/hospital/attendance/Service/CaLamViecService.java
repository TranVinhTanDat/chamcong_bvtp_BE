package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Entity.KyHieuChamCong;
import com.hospital.attendance.Repository.CaLamViecRepository;
import com.hospital.attendance.Repository.KyHieuChamCongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CaLamViecService {

    @Autowired
    private CaLamViecRepository caLamViecRepository;

    @Autowired
    private KyHieuChamCongRepository kyHieuChamCongRepository;

    public List<CaLamViec> getAllCaLamViec() {
        return caLamViecRepository.findAll();
    }

    public Page<CaLamViec> getPagedCaLamViec(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        return caLamViecRepository.findBySearch(search, pageable);
    }

    public Optional<CaLamViec> getCaLamViecById(Long id) {
        return caLamViecRepository.findById(id);
    }

    public CaLamViec createCaLamViec(CaLamViec caLamViec) {
        if (caLamViec.getKyHieuChamCong() == null || caLamViec.getKyHieuChamCong().getId() == null) {
            throw new IllegalArgumentException("Ký hiệu chấm công không được để trống");
        }
        KyHieuChamCong kyHieu = kyHieuChamCongRepository.findById(caLamViec.getKyHieuChamCong().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ký hiệu chấm công không tồn tại"));
        caLamViec.setKyHieuChamCong(kyHieu);
        return caLamViecRepository.save(caLamViec);
    }

    public CaLamViec updateCaLamViec(Long id, CaLamViec caLamViec) {
        CaLamViec existing = caLamViecRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ca làm việc không tồn tại"));
        if (caLamViec.getKyHieuChamCong() == null || caLamViec.getKyHieuChamCong().getId() == null) {
            throw new IllegalArgumentException("Ký hiệu chấm công không được để trống");
        }
        KyHieuChamCong kyHieu = kyHieuChamCongRepository.findById(caLamViec.getKyHieuChamCong().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ký hiệu chấm công không tồn tại"));
        existing.setTenCaLamViec(caLamViec.getTenCaLamViec());
        existing.setKyHieuChamCong(kyHieu);
        return caLamViecRepository.save(existing);
    }

    public void deleteCaLamViec(Long id) {
        if (!caLamViecRepository.existsById(id)) {
            throw new RuntimeException("Ca làm việc không tồn tại");
        }
        caLamViecRepository.deleteById(id);
    }
}