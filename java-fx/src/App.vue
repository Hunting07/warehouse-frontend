<template>
  <div id="app">
    <div class="box-wrap">
      <UserLogin></UserLogin>
      <UserRegister></UserRegister>
    </div>
  </div>
</template>

<script>
import UserLogin from './components/UserLogin.vue'
import UserRegister from './components/UserRegister.vue'

export default {
  name: 'App',
  components: {
    UserLogin,
    UserRegister
  }
}
</script>

<style>
#app {
  margin: 0;
  padding: 30px 0;
}
.box-wrap {
  display: flex;
  gap: 80px;
  justify-content: center;
}
</style>
