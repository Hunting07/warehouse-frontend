<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">仓储管理系统</h2>

      <!-- 角色选择区 -->
      <div class="role-select">
        <label class="role-item">
          <input
            type="radio"
            v-model="userRole"
            value="admin"
            name="role"
          >
          <span>管理员</span>
        </label>
        <label class="role-item">
          <input
            type="radio"
            v-model="userRole"
            value="staff"
            name="role"
          >
          <span>普通员工</span>
        </label>
      </div>

      <!-- 表单区域 -->
      <div class="form-group">
        <label class="form-label">用户名</label>
        <input
          type="text"
          class="form-input"
          v-model="username"
          placeholder="请输入用户名"
        >
      </div>

      <div class="form-group" style="position: relative;">
        <label class="form-label">密码</label>
        <input
          :type="showPwd ? 'text' : 'password'"
          class="form-input"
          v-model="password"
          placeholder="请输入密码"
        >
        <span
          @click="showPwd = !showPwd"
          style="position: absolute; right: 12px; top: 38px; cursor: pointer; color: #909399;"
        >
          {{ showPwd ? '🙈' : '👁️' }}
        </span>
      </div>

      <!-- 登录按钮 -->
      <button
        class="login-btn"
        :disabled="!isFormValid || loading"
        @click="handleLogin"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!-- 错误提示 -->
      <p v-if="errorMsg" class="error-tip">{{ errorMsg }}</p >
    </div>
  </div>
</template>

<script>
// 引入封装好的登录接口
import { login } from '../api/user.js'

export default {
  name: 'UserLogin',
  data() {
    return {
      userRole: '',
      username: '',
      password: '',
      errorMsg: '',
      showPwd: false,
      loading: false // 登录加载状态
    }
  },
  computed: {
    isFormValid() {
      return this.userRole && this.username.trim() && this.password.trim()
    }
  },
  methods: {
    async handleLogin() {
      this.errorMsg = ''

      // 前端基础校验
      if (!this.userRole) {
        this.errorMsg = '请先选择你的角色！'
        return
      }
      if (!this.username.trim()) {
        this.errorMsg = '请输入用户名！'
        return
      }
      if (!this.password.trim()) {
        this.errorMsg = '请输入密码！'
        return
      }

      this.loading = true
      try {
        // 调用后端登录接口
        const res = await login(this.username, this.password)
        // 适配后端返回格式，常规统一状态码200为成功
        if (res.data.code === 200) {
          alert('登录成功')
          // 登录成功根据角色跳转对应页面，自行修改路由地址
          if (this.userRole === 'admin') {
            this.$router.push('/adminHome')
          } else {
            this.$router.push('/staffHome')
          }
        } else {
          this.errorMsg = res.data.message || '用户名或密码错误'
        }
      } catch (err) {
        console.log('登录请求异常', err)
        this.errorMsg = '连接服务器失败，请检查后端是否启动'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
/* 样式完全保留你原有代码，无改动 */
.login-container {
  width: 100vw;
  height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
}

.login-box {
  width: 400px;
  background-color: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  color: #303133;
  margin-bottom: 30px;
  font-size: 24px;
}

.role-select {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 30px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #606266;
  cursor: pointer;
}

.role-item input:checked + span {
  color: #409eff;
  font-weight: bold;
}

.form-group {
  margin-bottom: 25px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #409eff;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.login-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.login-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}

.error-tip {
  color: #f56c6c;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}
</style>