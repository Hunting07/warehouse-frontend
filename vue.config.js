const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port:8081,
    proxy: {
      '/auth': {
        target: 'http://localhost:8085', // 你的后端地址
        changeOrigin: true,
        pathRewrite:{
        '^/auth':'/auth'
        }
      }
    }
  }
})