import axios from 'axios'

// 后端地址，和你启动的Java项目对应
const request = axios.create({
  baseURL: 'http://localhost:8085',
  timeout: 5000
})

// 登录请求
export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username: username,
      password: password
    }
  })
}

// 注册请求
export function register(username, password) {
  return request({
    url: '/auth/register',
    method: 'post',
    data: {
      username: username,
      password: password
    }
  })
}