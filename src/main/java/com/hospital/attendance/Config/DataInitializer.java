package com.hospital.attendance.Config;

import com.hospital.attendance.Entity.Role;
import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.RoleRepository;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 Bắt đầu khởi tạo dữ liệu cơ bản...");

        initializeRoles();
        initializeKhoaPhong();
        initializeAdminUser();

        logger.info("✅ Hoàn thành khởi tạo dữ liệu cơ bản!");
    }

    private void initializeRoles() {
        logger.info("📋 Khởi tạo Roles...");

        String[] roles = {"ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP"};

        for (String roleName : roles) {
            if (roleRepository.findByTenVaiTro(roleName).isEmpty()) {
                Role role = new Role(roleName);
                roleRepository.save(role);
                logger.info("✓ Đã tạo role: {}", roleName);
            } else {
                logger.info("→ Role đã tồn tại: {}", roleName);
            }
        }
    }

    private void initializeKhoaPhong() {
        logger.info("🏥 Khởi tạo Khoa Phòng...");

        String[][] khoaPhongs = {
                {"KP01", "Khoa Nội"},
                {"KP02", "Khoa Ngoại"},
                {"KP03", "Khoa Cấp Cứu"},
                {"KP04", "Khoa Sản"},
                {"KP05", "Khoa Nhi"},
                {"KP06", "Khoa Mắt"},
                {"KP07", "Khoa Tai Mũi Họng"},
                {"KP08", "Khoa Da Liễu"},
                {"KP09", "Khoa Thận Nhân Tạo"},
                {"KP10", "Khoa Phục Hồi Chức Năng"},
                {"KP11", "Phòng Xét Nghiệm"},
                {"KP12", "Phòng Chẩn Đoán Hình Ảnh"},
                {"KP13", "Phòng Dược"},
                {"KP14", "Phòng Hành Chính"},
                {"KP15", "Phòng Kế Toán"}
        };

        for (String[] kp : khoaPhongs) {
            String maKhoaPhong = kp[0];
            String tenKhoaPhong = kp[1];

            if (khoaPhongRepository.findByMaKhoaPhong(maKhoaPhong).isEmpty()) {
                KhoaPhong khoaPhong = new KhoaPhong(tenKhoaPhong, maKhoaPhong);
                khoaPhongRepository.save(khoaPhong);
                logger.info("✓ Đã tạo khoa phòng: {} - {}", maKhoaPhong, tenKhoaPhong);
            } else {
                logger.info("→ Khoa phòng đã tồn tại: {}", maKhoaPhong);
            }
        }
    }

    private void initializeAdminUser() {
        logger.info("👤 Khởi tạo User Admin...");

        String adminUsername = "admin";
        String adminPassword = "bvtpitchamcong";
        String adminEmail = "admin@hospital.com";

        if (userRepository.findByTenDangNhap(adminUsername).isEmpty()) {
            try {
                // Lấy role ADMIN
                Role adminRole = roleRepository.findByTenVaiTro("ADMIN")
                        .orElseThrow(() -> new RuntimeException("Role ADMIN không tồn tại"));

                // Lấy khoa phòng đầu tiên
                KhoaPhong defaultKhoaPhong = khoaPhongRepository.findByMaKhoaPhong("KP01")
                        .orElseThrow(() -> new RuntimeException("Khoa phòng KP01 không tồn tại"));

                // Tạo user admin
                User adminUser = new User();
                adminUser.setTenDangNhap(adminUsername);
                adminUser.setMatKhau(passwordEncoder.encode(adminPassword));
                adminUser.setEmail(adminEmail);
                adminUser.setRole(adminRole);
                adminUser.setKhoaPhong(defaultKhoaPhong);
                adminUser.setThoiGianTao(new Date());

                userRepository.save(adminUser);

                logger.info("✅ Đã tạo user admin thành công!");
                logger.info("📧 Username: {}", adminUsername);
                logger.info("🔐 Password: {}", adminPassword);
                logger.info("📨 Email: {}", adminEmail);

            } catch (Exception e) {
                logger.error("❌ Lỗi khi tạo user admin: {}", e.getMessage());
            }
        } else {
            logger.info("→ User admin đã tồn tại");
        }
    }
}