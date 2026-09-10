<template>
  <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path || String(menu.id)">
    <template #title>
      <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
      <span>{{ menu.menuName }}</span>
    </template>
    <sub-menu v-for="child in menu.children" :key="child.id" :menu="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="menu.path">
    <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
    <span>{{ menu.menuName }}</span>
  </el-menu-item>
</template>

<script setup lang="ts">
defineProps<{ menu: any }>()
</script>
