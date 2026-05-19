<template>
  <div class="register-container">
    <div class="register-box">
      <h2 class="register-title">仓储管理系统 - 注册</h2>

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

      <div class="form-group"
      style="position:relative;">
        <label class="form-label">密码</label>
       <input :type="showPwd1 ? 'text' : 'password'" v-model="password" placeholder="请输入密码">
       <span @click="showPwd1 = !showPwd1" style="position:absolute;right:15px;top:12px;cursor:pointer;">
       {{ showPwd1 ? '🙈' : '👁️' }}
       </span>
      </div>

      <div class="form-group"
      style="position:relative;">
        <label class="form-label">确认密码</label>
       <input :type="showPwd2 ? 'text' : 'password'" v-model="confirmPassword" placeholder="请再次输入密码">
       <span @click="showPwd2 = !showPwd2" style="position:absolute;right:15px;top:12px;cursor:pointer;">
       {{ showPwd2 ? '🙈' : '👁️' }}
       </span>
      </div>

      <!-- 注册按钮 -->
      <button
        class="register-btn"
        :disabled="!isFormValid"
        @click="handleRegister"
      >
        注册
      </button>

      <!-- 错误/成功提示 -->
      <p v-if="successMsg" class="success-tip">{{ successMsg }}</p >
      <p v-if="errorMsg" class="error-tip">{{ errorMsg }}</p >
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'UserRegister',
  data() {
    return {
      userRole: '',
      username: '',
      password: '',
      confirmPassword: '',
      errorMsg: '',
      successMsg: '',
      showPwd1:false,
      showPwd2:false
    }
  },
  computed: {
    isFormValid() {
      return this.userRole
        && this.username.trim()
        && this.password.trim()
        && this.confirmPassword.trim()
    }
  },
  methods: {
    async handleRegister() {
      this.errorMsg = ''
      this.successMsg = ''

      // 1. 前端基础校验
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
      if (this.password !== this.confirmPassword) {
        this.errorMsg = '两次输入的密码不一致！'
        return
      }

      try {
        // 2. 发送注册请求（和后端异常处理适配）
        const res = await axios.post('/api/register', {
          role: this.userRole,
          username: this.username,
          password: this.password
        })

        // 3. 处理后端正常响应
        if (res.data.code === 200) {
          this.successMsg = '注册成功！请前往登录页面登录'
          // 注册成功后，清空表单
          this.userRole = ''
          this.username = ''
          this.password = ''
          this.confirmPassword = ''
        } else {
          this.errorMsg = res.data.msg || '注册失败，请稍后重试'
        }
      } catch (err) {
        // 4. 适配后端全局异常处理的错误码和提示
        if (err.response) {
          const code = err.response.data.code
          const msg = err.response.data.msg

          switch (code) {
            case 400:
              this.errorMsg = msg || '非法参数，请检查输入'
              break
            case 404:
              this.errorMsg = msg || '数据不存在，操作失败'
              break
            case 500:
              this.errorMsg = msg || '系统内部错误，请稍后再试'
              break
            default:
              this.errorMsg = msg || '注册失败，请稍后重试'
          }
        } else {
          this.errorMsg = '网络异常，请检查后端服务是否启动'
        }
      }
    }
  }
}
</script>

<style scoped>
/* 整体布局 */
.register-container {
  width: 100vw;
  height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
}

.register-box {
  width: 400px;
  background-color: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.register-title {
  text-align: center;
  color: #303133;
  margin-bottom: 30px;
  font-size: 24px;
}

/* 角色选择 */
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

/* 表单样式 */
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

/* 注册按钮 */
.register-btn {
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

.register-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.register-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}

/* 提示信息 */
.success-tip {
  color: #67c23a;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}

.error-tip {
  color: #f56c6c;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}
</style>
