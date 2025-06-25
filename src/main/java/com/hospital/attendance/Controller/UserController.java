package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // *** ENDPOINT MỚI: Lấy thông tin user hiện tại (tất cả role đều truy cập được) ***
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String tenDangNhap = authentication.getName();
            User user = userService.findByUsername(tenDangNhap);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("tenDangNhap", user.getTenDangNhap());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole().getTenVaiTro());
            userInfo.put("khoaPhongId", user.getKhoaPhong().getId());
            userInfo.put("tenKhoaPhong", user.getKhoaPhong().getTenKhoaPhong());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin user: " + e.getMessage());
        }
    }

    // Thêm endpoint này vào UserController
    @PutMapping("/current/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> changeCurrentUserPassword(
            Authentication authentication,
            @RequestBody Map<String, String> passwordRequest) {
        try {
            String tenDangNhap = authentication.getName();
            User currentUser = userService.findByUsername(tenDangNhap);

            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (oldPassword == null || oldPassword.isEmpty()) {
                return ResponseEntity.badRequest().body("Mật khẩu cũ không được để trống");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body("Mật khẩu mới không được để trống");
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            // Kiểm tra mật khẩu cũ
            if (!passwordEncoder.matches(oldPassword, currentUser.getMatKhau())) {
                return ResponseEntity.badRequest().body("Mật khẩu cũ không đúng");
            }

            // Cập nhật mật khẩu mới
            userService.changeUserPassword(currentUser.getId(), newPassword);
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đổi mật khẩu: " + e.getMessage());
        }
    }

    // *** CÁC ENDPOINT DÀNH CHO ADMIN (giữ nguyên) ***
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User newUser = userService.createUser(user);
            return ResponseEntity.ok(newUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Xóa user thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserPassword(@PathVariable Long id, @RequestBody Map<String, String> passwordRequest) {
        try {
            String newPassword = passwordRequest.get("newPassword");
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body("Mật khẩu mới không được để trống");
            }
            userService.changeUserPassword(id, newPassword);
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.searchUsers(search, pageable);
        return ResponseEntity.ok(users);
    }
}