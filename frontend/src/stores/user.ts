import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/auth'

export interface UserInfo {
  id: number
  username: string
  nickname: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const menus = ref<any[]>([])
  const perms = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  function setToken(value: string) {
    token.value = value
    localStorage.setItem('token', value)
  }

  function clearToken() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    perms.value = []
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    const res: any = await getUserInfo()
    userInfo.value = res.data.user
    menus.value = res.data.menus
    perms.value = res.data.perms
    return res.data
  }

  return {
    token,
    userInfo,
    menus,
    perms,
    isLoggedIn,
    setToken,
    clearToken,
    fetchUserInfo,
  }
})
