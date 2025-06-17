package com.hospital.attendance.Entity;

public class TokenModel {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String role;
    private Long id;
    private Long khoaPhongId;

    public TokenModel(String accessToken, String refreshToken, Long expiresIn, String role, Long id, Long khoaPhongId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.role = role;
        this.id = id;
        this.khoaPhongId = khoaPhongId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getKhoaPhongId() { return khoaPhongId; }
    public void setKhoaPhongId(Long khoaPhongId) { this.khoaPhongId = khoaPhongId; }
}