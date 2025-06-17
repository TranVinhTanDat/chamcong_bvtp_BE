package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Entity.TokenModel;
import com.hospital.attendance.Repository.UserRepository;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Service.ChamCongService;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChamCongService chamCongService;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    @PostMapping("/dangnhap")
    public ResponseEntity<?> dangNhap(@RequestBody User user) {
        logger.info("Đang thử đăng nhập người dùng: {}", user.getTenDangNhap());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getTenDangNhap(), user.getMatKhau())
            );
            UserDetails userDetails = userService.loadUserByUsername(user.getTenDangNhap());
            User userDaDangNhap = userService.findByUsername(user.getTenDangNhap());
            String jwt = jwtService.generateToken(
                    userDetails,
                    userDaDangNhap.getId(),
                    userDaDangNhap.getRole().getTenVaiTro(),
                    userDaDangNhap.getKhoaPhong().getId()
            );
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            Long hetHanTrong = jwtService.getExpiresIn();
            return ResponseEntity.ok(new TokenModel(
                    jwt,
                    refreshToken,
                    hetHanTrong,
                    userDaDangNhap.getRole().getTenVaiTro(),
                    userDaDangNhap.getId(),
                    userDaDangNhap.getKhoaPhong().getId()
            ));
        } catch (BadCredentialsException e) {
            logger.error("Sai thông tin đăng nhập cho người dùng: {}", user.getTenDangNhap());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tên đăng nhập hoặc mật khẩu không đúng");
        } catch (Exception e) {
            logger.error("Lỗi trong quá trình đăng nhập cho người dùng: {}", user.getTenDangNhap(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Có lỗi xảy ra khi đăng nhập: " + e.getMessage());
        }
    }

    @PostMapping("/dangky")
    public ResponseEntity<?> dangKy(@RequestBody User user) {
        try {
            if (user.getKhoaPhong() == null || user.getKhoaPhong().getId() == null) {
                return ResponseEntity.badRequest().body("Phải cung cấp thông tin khoa/phòng hợp lệ (khoa_phong_id)");
            }
            Long khoaPhongId = user.getKhoaPhong().getId();
            if (!khoaPhongRepository.existsById(khoaPhongId)) {
                throw new IllegalStateException("Khoa/phòng với ID " + khoaPhongId + " không tồn tại");
            }
            user.setKhoaPhong(khoaPhongRepository.findById(khoaPhongId).get());
            user.setMatKhau(passwordEncoder.encode(user.getMatKhau()));
            userService.saveEmployee(user);
            return ResponseEntity.ok("Đăng ký người dùng thành công");
        } catch (IllegalStateException e) {
            logger.error("Đăng ký thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Lỗi xảy ra khi đăng ký người dùng", e);
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}