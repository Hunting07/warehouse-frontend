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

      <!-- 外层 form-group，样式写在开始标签 -->
      <div class="form-group" style="position: relative;">
        <label class="form-label">密码</label>
        <input
          :type="showPwd ? 'text' : 'password'"
          class="form-input"
          v-model="password"
          placeholder="请输入密码"
        >
        <!-- 眼睛图标，放在输入框右边 -->
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
        :disabled="!isFormValid"
        @click="handleLogin"
      >
        登录
      </button>

      <!-- 错误提示 -->
      <p v-if="errorMsg" class="error-tip">{{ errorMsg }}</p >
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserLogin',
  data() {
    return {
      userRole: '', // 角色：admin/staff
      username: '',
      password: '',
      errorMsg: '',
      showPwd:false
    }
  },
  computed: {
    // 表单是否填写完整（按钮置灰控制）
    isFormValid() {
      return this.userRole && this.username.trim() && this.password.trim()
    }
  },
  methods: {
    handleLogin() {
      this.errorMsg = ''

      // 1. 前端校验（基础校验，可根据需求扩展）
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

      // 2. 打印信息，方便后续对接后端接口
      console.log('登录信息：', {
        role: this.userRole,
        username: this.username,
        password: this.password
      })

      // 3. 这里后续可以替换成后端接口请求
      alert(
        `登录信息：\n角色：${this.userRole === 'admin' ? '管理员' : '普通员工'}\n用户名：${this.username}\n（后续对接后端接口后替换为真实校验）`
      )

      // 4. 后续：接口请求成功后，跳转到对应角色的主页
      // this.$router.push('/home')
    }
  }
}
</script>

<style scoped>
/* 整体布局 */
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

/* 登录按钮 */
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

/* 错误提示 */
.error-tip {
  color: #f56c6c;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}
</style>