package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Entity.Role;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Repository.UserRepository;
import com.hospital.attendance.Repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + tenDangNhap));

        String vaiTro = user.getRole().getTenVaiTro();
        return org.springframework.security.core.userdetails.User.withUsername(user.getTenDangNhap())
                .password(user.getMatKhau())
                .roles(vaiTro)
                .build();
    }

    public User findByUsername(String tenDangNhap) {
        return userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + tenDangNhap));
    }

    // Thêm phương thức getUserByTenDangNhap
    public User getUserByTenDangNhap(String tenDangNhap) {
        return userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng với tên đăng nhập: " + tenDangNhap));
    }

    @Transactional
    public User saveEmployee(User user) {
        Optional<User> existingByUsername = userRepository.findByTenDangNhap(user.getTenDangNhap());
        if (existingByUsername.isPresent()) {
            throw new IllegalStateException("Tên đăng nhập '" + user.getTenDangNhap() + "' đã tồn tại");
        }
        Optional<User> existingByEmail = userRepository.findByEmail(user.getEmail());
        if (existingByEmail.isPresent()) {
            throw new IllegalStateException("Email '" + user.getEmail() + "' đã tồn tại");
        }
        if (user.getKhoaPhong() == null || user.getKhoaPhong().getId() == null) {
            Role defaultRole = roleRepository.findByTenVaiTro("NGUOICHAMCONG")
                    .orElseThrow(() -> new IllegalStateException("Vai trò mặc định NGUOICHAMCONG không tồn tại"));
            user.setRole(defaultRole);
        } else {
            Role role = roleRepository.findByTenVaiTro(user.getRole().getTenVaiTro())
                    .orElseThrow(() -> new IllegalStateException("Vai trò '" + user.getRole().getTenVaiTro() + "' không tồn tại"));
            user.setRole(role);
        }
        return userRepository.save(user);
    }

    // Lấy danh sách tất cả user
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Thêm user mới
    @Transactional
    public User createUser(User user) {
        // Kiểm tra tên đăng nhập và email
        if (userRepository.findByTenDangNhap(user.getTenDangNhap()).isPresent()) {
            throw new IllegalStateException("Tên đăng nhập '" + user.getTenDangNhap() + "' đã tồn tại");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email '" + user.getEmail() + "' đã tồn tại");
        }

        // Kiểm tra và gán role
        Role role = roleRepository.findByTenVaiTro(user.getRole().getTenVaiTro())
                .orElseThrow(() -> new IllegalStateException("Vai trò '" + user.getRole().getTenVaiTro() + "' không tồn tại"));
        user.setRole(role);

        // Kiểm tra và gán khoa/phòng
        KhoaPhong khoaPhong = khoaPhongRepository.findById(user.getKhoaPhong().getId())
                .orElseThrow(() -> new IllegalStateException("Khoa/phòng với ID " + user.getKhoaPhong().getId() + " không tồn tại"));
        user.setKhoaPhong(khoaPhong);

        // Mã hóa mật khẩu
        user.setMatKhau(passwordEncoder.encode(user.getMatKhau()));

        return userRepository.save(user);
    }

    // Sửa thông tin user
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user với ID: " + id));

        // Kiểm tra tên đăng nhập trùng (nếu thay đổi)
        if (!user.getTenDangNhap().equals(userDetails.getTenDangNhap()) &&
                userRepository.findByTenDangNhap(userDetails.getTenDangNhap()).isPresent()) {
            throw new IllegalStateException("Tên đăng nhập '" + userDetails.getTenDangNhap() + "' đã tồn tại");
        }

        // Kiểm tra email trùng (nếu thay đổi)
        if (!user.getEmail().equals(userDetails.getEmail()) &&
                userRepository.findByEmail(userDetails.getEmail()).isPresent()) {
            throw new IllegalStateException("Email '" + userDetails.getEmail() + "' đã tồn tại");
        }

        // Cập nhật thông tin
        user.setTenDangNhap(userDetails.getTenDangNhap());
        user.setEmail(userDetails.getEmail());

        // Cập nhật mật khẩu nếu có
        if (userDetails.getMatKhau() != null && !userDetails.getMatKhau().isEmpty()) {
            user.setMatKhau(passwordEncoder.encode(userDetails.getMatKhau()));
        }

        // Cập nhật role
        if (userDetails.getRole() != null && userDetails.getRole().getTenVaiTro() != null) {
            Role role = roleRepository.findByTenVaiTro(userDetails.getRole().getTenVaiTro())
                    .orElseThrow(() -> new IllegalStateException("Vai trò '" + userDetails.getRole().getTenVaiTro() + "' không tồn tại"));
            user.setRole(role);
        }

        // Cập nhật khoa/phòng
        if (userDetails.getKhoaPhong() != null && userDetails.getKhoaPhong().getId() != null) {
            KhoaPhong khoaPhong = khoaPhongRepository.findById(userDetails.getKhoaPhong().getId())
                    .orElseThrow(() -> new IllegalStateException("Khoa/phòng với ID " + userDetails.getKhoaPhong().getId() + " không tồn tại"));
            user.setKhoaPhong(khoaPhong);
        }

        return userRepository.save(user);
    }

    // Xóa user
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user với ID: " + id));
        userRepository.delete(user);
    }

    // Đổi mật khẩu user
    @Transactional
    public void changeUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user với ID: " + id));
        user.setMatKhau(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Page<User> searchUsers(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByTenDangNhapContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
    }
}