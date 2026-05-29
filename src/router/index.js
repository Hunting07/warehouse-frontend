import Vue from 'vue'
import Router from 'vue-router'

// 引入登录、注册、用户管理页面
import UserLogin from '@/views/UserLogin.vue'
import UserRegister from '@/views/UserRegister.vue'
import UserManage from '@/views/UserManage.vue'

Vue.use(Router)

export default new Router({
  routes: [
    // 原来的登录路由（保留不动）
    {
      path: '/login',
      name: 'UserLogin',
      component: UserLogin
    },
    // 原来的注册路由（保留不动）
    {
      path: '/register',
      name: 'UserRegister',
      component: UserRegister
    },
    // 新增：用户管理页面路由（这就是关键！）
    {
      path: '/user-manage', // 浏览器里访问的地址后缀
      name: 'UserManage',
      component: UserManage
    },
    // 可选：默认打开登录页（如果需要的话）
    {
      path: '/',
      redirect: '/login'
    }
  ]
})