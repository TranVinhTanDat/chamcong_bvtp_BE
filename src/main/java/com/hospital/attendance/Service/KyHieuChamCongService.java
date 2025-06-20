package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KyHieuChamCong;
import com.hospital.attendance.Repository.KyHieuChamCongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KyHieuChamCongService {

    @Autowired
    private KyHieuChamCongRepository kyHieuChamCongRepository;

    public List<KyHieuChamCong> getAllKyHieuChamCong() {
        return kyHieuChamCongRepository.findAll();
    }

    public Page<KyHieuChamCong> getPagedKyHieuChamCong(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        return kyHieuChamCongRepository.findAllWithSearch(search, pageable);
    }

    public Optional<KyHieuChamCong> getKyHieuChamCongById(Long id) {
        return kyHieuChamCongRepository.findById(id);
    }

    public KyHieuChamCong createKyHieuChamCong(KyHieuChamCong kyHieuChamCong) {
        if (kyHieuChamCong.getMaKyHieu() == null || kyHieuChamCong.getTenKyHieu() == null) {
            throw new IllegalArgumentException("Mã ký hiệu và tên ký hiệu không được để trống");
        }
        if (kyHieuChamCongRepository.findByMaKyHieu(kyHieuChamCong.getMaKyHieu()).isPresent()) {
            throw new IllegalArgumentException("Mã ký hiệu đã tồn tại");
        }
        return kyHieuChamCongRepository.save(kyHieuChamCong);
    }

    public KyHieuChamCong updateKyHieuChamCong(Long id, KyHieuChamCong kyHieuChamCong) {
        return kyHieuChamCongRepository.findById(id).map(existing -> {
            if (kyHieuChamCong.getMaKyHieu() != null && !kyHieuChamCong.getMaKyHieu().equals(existing.getMaKyHieu())) {
                if (kyHieuChamCongRepository.findByMaKyHieu(kyHieuChamCong.getMaKyHieu()).isPresent()) {
                    throw new IllegalArgumentException("Mã ký hiệu đã tồn tại");
                }
                existing.setMaKyHieu(kyHieuChamCong.getMaKyHieu());
            }
            if (kyHieuChamCong.getTenKyHieu() != null) {
                existing.setTenKyHieu(kyHieuChamCong.getTenKyHieu());
            }
            existing.setTrangThai(kyHieuChamCong.isTrangThai());
            existing.setGhiChu(kyHieuChamCong.getGhiChu());
            return kyHieuChamCongRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy ký hiệu chấm công"));
    }

    public void deleteKyHieuChamCong(Long id) {
        if (!kyHieuChamCongRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy ký hiệu chấm công");
        }
        kyHieuChamCongRepository.deleteById(id);
    }
}