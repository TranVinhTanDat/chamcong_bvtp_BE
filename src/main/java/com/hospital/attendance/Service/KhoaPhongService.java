package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KhoaPhongService {

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    public List<KhoaPhong> getAllKhoaPhongs() {
        return khoaPhongRepository.findAll();
    }

    public Page<KhoaPhong> getKhoaPhongsWithPagination(Pageable pageable, String searchTerm) {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return khoaPhongRepository.findAllByTenKhoaPhongContainingIgnoreCaseOrMaKhoaPhongContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        }
        return khoaPhongRepository.findAll(pageable);
    }

    public KhoaPhong saveKhoaPhong(KhoaPhong khoaPhong) {
        // Kiểm tra trùng lặp (có thể thêm logic kiểm tra cụ thể hơn)
        Optional<KhoaPhong> existingByTen = khoaPhongRepository.findByTenKhoaPhong(khoaPhong.getTenKhoaPhong());
        Optional<KhoaPhong> existingByMa = khoaPhongRepository.findByMaKhoaPhong(khoaPhong.getMaKhoaPhong());
        if (existingByTen.isPresent() || existingByMa.isPresent()) {
            throw new IllegalArgumentException("Tên hoặc mã khoa phòng đã tồn tại!");
        }
        return khoaPhongRepository.save(khoaPhong);
    }

    public KhoaPhong updateKhoaPhong(Long id, KhoaPhong khoaPhong) {
        KhoaPhong existingKhoaPhong = khoaPhongRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khoa phòng không tồn tại!"));
        existingKhoaPhong.setTenKhoaPhong(khoaPhong.getTenKhoaPhong());
        existingKhoaPhong.setMaKhoaPhong(khoaPhong.getMaKhoaPhong());
        return khoaPhongRepository.save(existingKhoaPhong);
    }

    public void deleteKhoaPhong(Long id) {
        KhoaPhong khoaPhong = khoaPhongRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khoa phòng không tồn tại!"));
        khoaPhongRepository.delete(khoaPhong);
    }
}