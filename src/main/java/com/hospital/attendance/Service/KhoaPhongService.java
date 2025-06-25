package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class KhoaPhongService {

    private static final Logger logger = LoggerFactory.getLogger(KhoaPhongService.class);

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

    @Transactional
    public KhoaPhong saveKhoaPhong(KhoaPhong khoaPhong) {
        logger.info("Saving KhoaPhong: {}", khoaPhong.getTenKhoaPhong());

        // Validate input
        if (khoaPhong.getTenKhoaPhong() == null || khoaPhong.getTenKhoaPhong().trim().isEmpty()) {
            throw new IllegalStateException("Tên khoa/phòng không được để trống");
        }

        if (khoaPhong.getMaKhoaPhong() == null || khoaPhong.getMaKhoaPhong().trim().isEmpty()) {
            throw new IllegalStateException("Mã khoa/phòng không được để trống");
        }

        // Trim whitespace
        String tenKhoaPhong = khoaPhong.getTenKhoaPhong().trim();
        String maKhoaPhong = khoaPhong.getMaKhoaPhong().trim().toUpperCase(); // Chuẩn hóa uppercase

        // Validate mã khoa phòng format
        if (!maKhoaPhong.matches("^[A-Z0-9_-]+$")) {
            throw new IllegalStateException("Mã khoa/phòng chỉ được chứa chữ in hoa, số, dấu gạch dưới (_) và dấu gạch ngang (-)");
        }

        if (maKhoaPhong.length() > 50) {
            throw new IllegalStateException("Mã khoa/phòng không được quá 50 ký tự");
        }

        // Kiểm tra trùng lặp tên khoa phòng
        Optional<KhoaPhong> existingByTen = khoaPhongRepository.findByTenKhoaPhong(tenKhoaPhong);
        if (existingByTen.isPresent()) {
            throw new IllegalStateException("Tên khoa/phòng '" + tenKhoaPhong + "' đã tồn tại");
        }

        // Kiểm tra trùng lặp mã khoa phòng
        Optional<KhoaPhong> existingByMa = khoaPhongRepository.findByMaKhoaPhong(maKhoaPhong);
        if (existingByMa.isPresent()) {
            throw new IllegalStateException("Mã khoa/phòng '" + maKhoaPhong + "' đã tồn tại");
        }

        // Set normalized values
        khoaPhong.setTenKhoaPhong(tenKhoaPhong);
        khoaPhong.setMaKhoaPhong(maKhoaPhong);

        logger.info("Saving KhoaPhong to database with ma: {}", maKhoaPhong);
        return khoaPhongRepository.save(khoaPhong);
    }

    @Transactional
    public KhoaPhong updateKhoaPhong(Long id, KhoaPhong khoaPhongDetails) {
        logger.info("Updating KhoaPhong with ID: {}", id);

        KhoaPhong existingKhoaPhong = khoaPhongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Khoa/phòng với ID " + id + " không tồn tại"));

        // Validate input
        if (khoaPhongDetails.getTenKhoaPhong() == null || khoaPhongDetails.getTenKhoaPhong().trim().isEmpty()) {
            throw new IllegalStateException("Tên khoa/phòng không được để trống");
        }

        if (khoaPhongDetails.getMaKhoaPhong() == null || khoaPhongDetails.getMaKhoaPhong().trim().isEmpty()) {
            throw new IllegalStateException("Mã khoa/phòng không được để trống");
        }

        // Trim whitespace
        String tenKhoaPhong = khoaPhongDetails.getTenKhoaPhong().trim();
        String maKhoaPhong = khoaPhongDetails.getMaKhoaPhong().trim().toUpperCase();

        // Validate mã khoa phòng format
        if (!maKhoaPhong.matches("^[A-Z0-9_-]+$")) {
            throw new IllegalStateException("Mã khoa/phòng chỉ được chứa chữ in hoa, số, dấu gạch dưới (_) và dấu gạch ngang (-)");
        }

        if (maKhoaPhong.length() > 50) {
            throw new IllegalStateException("Mã khoa/phòng không được quá 50 ký tự");
        }

        // Kiểm tra trùng lặp tên khoa phòng (nếu thay đổi)
        if (!existingKhoaPhong.getTenKhoaPhong().equals(tenKhoaPhong)) {
            Optional<KhoaPhong> existingByTen = khoaPhongRepository.findByTenKhoaPhong(tenKhoaPhong);
            if (existingByTen.isPresent()) {
                throw new IllegalStateException("Tên khoa/phòng '" + tenKhoaPhong + "' đã tồn tại");
            }
        }

        // Kiểm tra trùng lặp mã khoa phòng (nếu thay đổi)
        if (!existingKhoaPhong.getMaKhoaPhong().equals(maKhoaPhong)) {
            Optional<KhoaPhong> existingByMa = khoaPhongRepository.findByMaKhoaPhong(maKhoaPhong);
            if (existingByMa.isPresent()) {
                throw new IllegalStateException("Mã khoa/phòng '" + maKhoaPhong + "' đã tồn tại");
            }
        }

        // Update values
        existingKhoaPhong.setTenKhoaPhong(tenKhoaPhong);
        existingKhoaPhong.setMaKhoaPhong(maKhoaPhong);

        logger.info("Updating KhoaPhong in database");
        return khoaPhongRepository.save(existingKhoaPhong);
    }

    @Transactional
    public void deleteKhoaPhong(Long id) {
        logger.info("Deleting KhoaPhong with ID: {}", id);

        KhoaPhong khoaPhong = khoaPhongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Khoa/phòng với ID " + id + " không tồn tại"));

        // TODO: Kiểm tra xem có nhân viên nào đang thuộc khoa phòng này không
        // Optional: Thêm validation để không cho xóa nếu còn nhân viên

        khoaPhongRepository.delete(khoaPhong);
        logger.info("Deleted KhoaPhong successfully");
    }
}