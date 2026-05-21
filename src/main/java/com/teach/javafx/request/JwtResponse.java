package com.teach.javafx.request;

public class JwtResponse {
    private String tokenType;
    private Integer id;
    private String username;
    private String token; // 兼容前端旧代码
    private String role;

    // 新增：适配 Satoken 返回的字段
    private String tokenValue;
    private String loginId;
    private String loginType;
    private Boolean isLogin;

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // 新增属性的 Getter 和 Setter
    public String getTokenValue() { return tokenValue; }
    public void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getLoginType() { return loginType; }
    public void setLoginType(String loginType) { this.loginType = loginType; }

    public Boolean getIsLogin() { return isLogin; }
    public void setIsLogin(Boolean isLogin) { this.isLogin = isLogin; }
}
