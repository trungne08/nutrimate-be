# 📚 Hướng Dẫn Sử Dụng API Authentication - Nutrimate

## 🔐 Tổng Quan

Hệ thống sử dụng **AWS Cognito** để xác thực người dùng thông qua OAuth2/OpenID Connect. Sau khi đăng nhập, session được lưu trong cookie và có thể sử dụng cho các API khác.

---

## 🚀 API Endpoints

### 1. **Đăng Nhập (Login)**

#### Cách 1: Redirect trực tiếp (Đơn giản nhất)

```javascript
// React/Vue/Angular
const handleLogin = () => {
    // Redirect user đến Cognito login page
    window.location.href = "http://localhost:8080/oauth2/authorization/cognito";
};
```

#### Cách 2: Dùng API endpoint

```javascript
// Lấy login URL từ API
const login = async () => {
    try {
        const response = await fetch('http://localhost:8080/api/auth/login');
        const data = await response.json();
        
        // Redirect đến login URL
        window.location.href = `http://localhost:8080${data.loginUrl}`;
    } catch (error) {
        console.error('Login error:', error);
    }
};
```

**Response:**
```json
{
  "loginUrl": "/oauth2/authorization/cognito",
  "message": "Redirect to this URL to start login"
}
```

#### Flow đăng nhập hoàn chỉnh:

1. **User click "Đăng nhập"** → Frontend redirect đến:
   ```
   http://localhost:8080/oauth2/authorization/cognito
   ```

2. **Spring Security redirect** → User đến Cognito login page

3. **User nhập email/password** trên Cognito

4. **Cognito xác thực thành công** → Redirect về:
   ```
   http://localhost:8080/login/oauth2/code/cognito
   ```

5. **Backend xử lý** → Tạo/cập nhật user trong database

6. **Redirect về Frontend với token trong URL**:
   ```
   http://localhost:5173?token=eyJraWQiOiJ...&access_token=eyJraWQiOiJ...&token_type=Bearer
   ```
   
   **Frontend cần lấy token từ URL:**
   ```javascript
   // Sau khi redirect về, lấy token từ URL
   const urlParams = new URLSearchParams(window.location.search);
   const token = urlParams.get('token'); // ID Token
   const accessToken = urlParams.get('access_token'); // Access Token
   
   if (token || accessToken) {
       // Lưu token vào localStorage hoặc state
       localStorage.setItem('token', token || accessToken);
       // Xóa token khỏi URL để bảo mật
       window.history.replaceState({}, document.title, window.location.pathname);
   }
   ```

---

### 2. **Kiểm Tra Trạng Thái Đăng Nhập**

```javascript
const checkAuthStatus = async () => {
    try {
        const response = await fetch('http://localhost:8080/api/auth/status', {
            credentials: 'include' // Quan trọng: gửi cookie
        });
        
        const data = await response.json();
        
        if (data.authenticated) {
            console.log('User đã đăng nhập:', data.email);
            return true;
        } else {
            console.log('User chưa đăng nhập');
            return false;
        }
    } catch (error) {
        console.error('Error:', error);
        return false;
    }
};
```

**Response khi đã đăng nhập:**
```json
{
  "authenticated": true,
  "email": "user@example.com"
}
```

**Response khi chưa đăng nhập:**
```json
{
  "authenticated": false
}
```

---

### 3. **Lấy Thông Tin User Hiện Tại**

```javascript
const getCurrentUser = async () => {
    try {
        const response = await fetch('http://localhost:8080/api/auth/me', {
            credentials: 'include' // Quan trọng: gửi cookie
        });
        
        const data = await response.json();
        
        if (data.authenticated && data.user) {
            console.log('User info:', data.user);
            return data.user;
        } else {
            console.log('User not found');
            return null;
        }
    } catch (error) {
        console.error('Error:', error);
        return null;
    }
};
```

**Response:**
```json
{
  "authenticated": true,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "username": "nguyenvana",
    "role": "MEMBER",
    "avatarUrl": "https://..."
  }
}
```

---

### 4. **Kiểm Tra Trạng Thái Profile (Sau khi đăng nhập)**

```javascript
const checkProfileStatus = async () => {
    try {
        const response = await fetch('http://localhost:8080/api/auth/profile/status', {
            credentials: 'include'
        });
        
        const data = await response.json();
        
        if (!data.allComplete) {
            // Kiểm tra user profile
            if (!data.userProfile.complete) {
                console.log('Thiếu thông tin user:', data.userProfile.missingFields);
                // Hiển thị form nhập: fullName, phoneNumber
            }
            
            // Kiểm tra health profile
            if (!data.healthProfile.complete) {
                console.log('Thiếu thông tin health:', data.healthProfile.missingFields);
                // Hiển thị form nhập: gender, dateOfBirth, heightCm, weightKg
            }
        } else {
            console.log('Profile đã đầy đủ!');
        }
        
        return data;
    } catch (error) {
        console.error('Error:', error);
    }
};
```

**Response khi thiếu thông tin:**
```json
{
  "success": true,
  "allComplete": false,
  "message": "Profile is incomplete. Please complete missing fields.",
  "userProfile": {
    "complete": false,
    "missingFields": ["fullName", "phoneNumber"]
  },
  "healthProfile": {
    "exists": false,
    "complete": false,
    "missingFields": ["gender", "dateOfBirth", "heightCm", "weightKg"]
  }
}
```

---

### 5. **Đăng Xuất (Logout)**

#### Cách 1: Redirect trực tiếp

```javascript
const handleLogout = () => {
    window.location.href = "http://localhost:8080/logout";
};
```

#### Cách 2: Dùng API endpoint

```javascript
const logout = async () => {
    try {
        const response = await fetch('http://localhost:8080/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
        
        const data = await response.json();
        
        // Redirect đến logout URL
        window.location.href = `http://localhost:8080${data.logoutUrl}`;
    } catch (error) {
        console.error('Logout error:', error);
    }
};
```

**Response:**
```json
{
  "logoutUrl": "/logout",
  "message": "Redirect to this URL to logout"
}
```

**Sau khi logout**, user sẽ được redirect về trang chủ hoặc login page.

---

## 📝 Ví Dụ Hoàn Chỉnh - React Component

```javascript
import { useState, useEffect } from 'react';

function AuthExample() {
    const [user, setUser] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [profileStatus, setProfileStatus] = useState(null);
    
    const API_BASE_URL = 'http://localhost:8080';
    
    // Kiểm tra trạng thái đăng nhập khi component mount
    useEffect(() => {
        checkAuth();
    }, []);
    
    // Kiểm tra đăng nhập
    const checkAuth = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/status`, {
                credentials: 'include'
            });
            const data = await response.json();
            
            setIsAuthenticated(data.authenticated);
            
            if (data.authenticated) {
                // Lấy thông tin user
                await getUserInfo();
                // Kiểm tra trạng thái profile
                await checkProfileStatus();
            }
        } catch (error) {
            console.error('Auth check error:', error);
        }
    };
    
    // Lấy thông tin user
    const getUserInfo = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
                credentials: 'include'
            });
            const data = await response.json();
            
            if (data.authenticated && data.user) {
                setUser(data.user);
            }
        } catch (error) {
            console.error('Get user error:', error);
        }
    };
    
    // Kiểm tra trạng thái profile
    const checkProfileStatus = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/profile/status`, {
                credentials: 'include'
            });
            const data = await response.json();
            
            setProfileStatus(data);
            
            if (!data.allComplete) {
                // Hiển thị thông báo hoặc form nhập thông tin
                alert('Vui lòng hoàn thiện thông tin profile!');
            }
        } catch (error) {
            console.error('Profile status error:', error);
        }
    };
    
    // Đăng nhập
    const handleLogin = () => {
        window.location.href = `${API_BASE_URL}/oauth2/authorization/cognito`;
    };
    
    // Đăng xuất
    const handleLogout = () => {
        window.location.href = `${API_BASE_URL}/logout`;
    };
    
    return (
        <div>
            {!isAuthenticated ? (
                <div>
                    <h2>Chưa đăng nhập</h2>
                    <button onClick={handleLogin}>Đăng Nhập</button>
                </div>
            ) : (
                <div>
                    <h2>Đã đăng nhập</h2>
                    {user && (
                        <div>
                            <p>Email: {user.email}</p>
                            <p>Họ tên: {user.fullName || 'Chưa có'}</p>
                            <p>Role: {user.role}</p>
                        </div>
                    )}
                    
                    {profileStatus && !profileStatus.allComplete && (
                        <div style={{ color: 'orange' }}>
                            <h3>⚠️ Profile chưa đầy đủ</h3>
                            {!profileStatus.userProfile.complete && (
                                <p>Thiếu: {profileStatus.userProfile.missingFields.join(', ')}</p>
                            )}
                            {!profileStatus.healthProfile.complete && (
                                <p>Thiếu: {profileStatus.healthProfile.missingFields.join(', ')}</p>
                            )}
                        </div>
                    )}
                    
                    <button onClick={handleLogout}>Đăng Xuất</button>
                </div>
            )}
        </div>
    );
}

export default AuthExample;
```

---

## 🔑 Lấy Access Token (Để test trên Swagger)

### Bước 1: Đăng nhập
```
http://localhost:8080/oauth2/authorization/cognito
```

### Bước 2: Lấy token
```javascript
const getToken = async () => {
    const response = await fetch('http://localhost:8080/api/auth/token', {
        credentials: 'include'
    });
    const data = await response.json();
    
    console.log('Access Token:', data.access_token);
    // Copy token này vào Swagger "Authorize" button
};
```

**Response:**
```json
{
  "access_token": "eyJraWQiOiJ...",
  "token_type": "Bearer",
  "id_token": "eyJraWQiOiJ...",
  "expires_at": "2026-01-27T12:00:00Z"
}
```

### Bước 3: Dùng token trong Swagger
1. Mở Swagger UI: `http://localhost:8080/swagger-ui/index.html`
2. Click nút **"Authorize"** (ổ khóa)
3. Dán `access_token` vào ô "Value"
4. Click **"Authorize"**
5. Giờ có thể test các API protected

---

## ⚠️ Lưu Ý Quan Trọng

### 1. **Credentials: 'include'**
Luôn dùng `credentials: 'include'` khi gọi API để gửi cookie/session:
```javascript
fetch('http://localhost:8080/api/auth/me', {
    credentials: 'include' // ⚠️ QUAN TRỌNG!
});
```

### 2. **CORS**
Backend đã cấu hình CORS cho `http://localhost:5173`. Nếu dùng port khác, cần cập nhật trong `AuthController`:
```java
@CrossOrigin(origins = "http://localhost:5173")
```

### 3. **Session Management**
- Session được lưu trong cookie (JSESSIONID)
- Cookie tự động được gửi khi dùng `credentials: 'include'`
- Session hết hạn khi user logout hoặc timeout

### 4. **Redirect sau Login - Lấy Token từ URL**
Sau khi login thành công, user được redirect về:
```
http://localhost:5173?token=eyJraWQiOiJ...&access_token=eyJraWQiOiJ...&token_type=Bearer
```

**Frontend cần xử lý:**
```javascript
// Component hoặc hook để lấy token sau khi redirect
useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token'); // ID Token (OIDC)
    const accessToken = urlParams.get('access_token'); // Access Token
    
    if (token || accessToken) {
        // Lưu token
        const finalToken = accessToken || token;
        localStorage.setItem('authToken', finalToken);
        
        // Xóa token khỏi URL để bảo mật
        window.history.replaceState({}, document.title, window.location.pathname);
        
        // Tiếp tục xử lý (kiểm tra profile, etc.)
        checkProfileStatus();
    }
}, []);
```

**Lưu ý:** Token trong URL chỉ tồn tại trong lần redirect đầu tiên. Sau đó nên xóa khỏi URL để bảo mật.

---

## 🎯 Flow Hoàn Chỉnh Cho User Mới

1. **User đăng nhập lần đầu** → Redirect về `http://localhost:5173`
2. **Frontend gọi** `GET /api/auth/profile/status`
3. **Backend trả về** các trường còn thiếu
4. **Frontend hiển thị form** nhập thông tin:
   - Form 1: User Profile (fullName, phoneNumber)
   - Form 2: Health Profile (gender, dateOfBirth, heightCm, weightKg)
5. **User điền thông tin** → Gọi API cập nhật
6. **Sau khi cập nhật** → Gọi lại `/api/auth/profile/status`
7. **Khi `allComplete: true`** → Cho phép sử dụng app

---

## 📞 API Endpoints Tóm Tắt

| Method | Endpoint | Mô tả | Auth Required |
|--------|----------|-------|---------------|
| GET | `/oauth2/authorization/cognito` | Redirect đến Cognito login | ❌ |
| GET | `/api/auth/login` | Lấy login URL | ❌ |
| GET | `/api/auth/status` | Kiểm tra trạng thái đăng nhập | ❌ |
| GET | `/api/auth/me` | Lấy thông tin user | ✅ |
| GET | `/api/auth/profile/status` | Kiểm tra trạng thái profile | ✅ |
| GET | `/api/auth/profile` | Lấy user profile | ✅ |
| PUT | `/api/auth/profile` | Cập nhật user profile | ✅ |
| GET | `/api/auth/token` | Lấy access token | ✅ |
| POST | `/api/auth/logout` | Lấy logout URL | ✅ |
| GET | `/logout` | Thực hiện logout | ✅ |

---

## 🐛 Troubleshooting

### Lỗi: "Unauthorized" khi gọi API
- **Nguyên nhân**: Chưa đăng nhập hoặc session hết hạn
- **Giải pháp**: Đăng nhập lại hoặc kiểm tra `credentials: 'include'`

### Lỗi: CORS khi gọi từ Frontend
- **Nguyên nhân**: Port frontend không được cho phép
- **Giải pháp**: Cập nhật `@CrossOrigin` trong Controller

### Redirect không hoạt động
- **Nguyên nhân**: `defaultSuccessUrl` không đúng
- **Giải pháp**: Kiểm tra `SecurityConfig.java`

---

Chúc bạn code vui vẻ! 🚀
