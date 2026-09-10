<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">Ledger Admin</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <sub-menu v-for="menu in menus" :key="menu.id" :menu="menu" />
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span class="username">{{ userStore.userInfo?.nickname }}</span>
          <el-button type="danger" size="small" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import SubMenu from './SubMenu.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => userStore.menus)

function logout() {
  userStore.clearToken()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.username {
  color: #606266;
}
.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
