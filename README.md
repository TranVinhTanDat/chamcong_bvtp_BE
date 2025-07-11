# Hospital Attendance System

Hệ thống chấm công bệnh viện với Backend (Spring Boot) và Frontend (React).

## 📋 Yêu cầu cài đặt

### Phần mềm bắt buộc:
- **Docker Desktop** - [Tải tại đây](https://www.docker.com/products/docker-desktop/)
- **Git** - [Tải tại đây](https://git-scm.com/download/win)

### Kiểm tra đã cài đặt:
```bash
docker --version
git --version
```

### Phần mềm tùy chọn (cho development):
- **Java 17** - [Tải tại đây](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Node.js 18+** - [Tải tại đây](https://nodejs.org/)
- **Maven 3.6+** - Có sẵn trong Docker

*Lưu ý: Với Docker, không cần cài Java và Node.js trên máy local.*

## 🚀 Cách chạy

### 1. Clone source code
```bash
# Tạo thư mục project
mkdir hospital-attendance
cd hospital-attendance

# Clone Backend (nhánh master)
git clone -b master https://github.com/TranVinhTanDat/chamcong_bvtp_BE.git backend

# Clone Frontend (nhánh master)  
git clone -b master https://github.com/TranVinhTanDat/chamcong_bvtp_FE.git frontend
```

### 2. Chạy Backend
```bash
cd backend

# Tạo các file Docker (tạo 1 lần duy nhất)
# - docker-compose.yml
# - Dockerfile  
# - .env

# Chạy Backend
docker-compose up -d --build

# Kiểm tra
docker-compose ps
```

### 3. Chạy Frontend
```bash
cd ../frontend

# Tạo các file Docker (tạo 1 lần duy nhất)
# - docker-compose.yml
# - Dockerfile
# - nginx.conf
# - .env

# Chạy Frontend
docker-compose up -d --build

# Kiểm tra
docker-compose ps
```

## 🌐 Truy cập hệ thống

| Service | URL | Mô tả |
|---------|-----|-------|
| **Frontend** | http://localhost:3000 | Trang web chấm công |
| **Backend API** | http://localhost:8080 | API backend |
| **phpMyAdmin** | http://localhost:8081 | Quản lý database |

### phpMyAdmin login:
- **Server**: mysql
- **Username**: attendance_user
- **Password**: AttendanceUser2024@UserPassword@Secure

## 🔧 Quản lý

### Dừng services
```bash
# Dừng Backend
cd backend && docker-compose down

# Dừng Frontend
cd frontend && docker-compose down
```

### Khởi động lại
```bash
# Khởi động Backend
cd backend && docker-compose up -d

# Khởi động Frontend
cd frontend && docker-compose up -d
```

### Xem logs
```bash
# Logs Backend
cd backend && docker-compose logs -f

# Logs Frontend
cd frontend && docker-compose logs -f
```

### Update code mới
```bash
# Update Backend
cd backend
git pull origin master
docker-compose up -d --build

# Update Frontend
cd frontend  
git pull origin master
docker-compose up -d --build
```

## ⚙️ Files cần tạo

### Backend files:
- `docker-compose.yml` - Config Docker services
- `Dockerfile` - Build backend
- `.env` - Environment variables

### Frontend files:
- `docker-compose.yml` - Config Docker service
- `Dockerfile` - Build frontend
- `nginx.conf` - Nginx config
- `.env` - Environment variables

*Chi tiết nội dung các files có trong history setup.*

## 🐛 Troubleshooting

### Port đã được sử dụng
```bash
docker-compose down
docker container prune -f
docker-compose up -d --build
```

### Containers không khởi động
```bash
# Xem logs lỗi
docker-compose logs [service-name]

# Restart Docker Desktop
```

### Update IP server (Production)
Thay đổi IP trong:
- `backend/.env`: `VPS_HOST=your_server_ip`
- `frontend/.env`: `REACT_APP_API_URL=http://your_server_ip:8080`

---

**✅ Status: Tested & Working**  
*All containers running successfully with Docker Compose*