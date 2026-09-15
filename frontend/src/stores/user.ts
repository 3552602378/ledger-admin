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

  // 用户可访问的第一个叶子菜单路径，作为登录后的默认跳转目标
  const homePath = computed(() => {
    const leaves: string[] = []
    const walk = (nodes: any[]) => {
      for (const n of nodes) {
        if (n.children && n.children.length) {
          walk(n.children)
        } else if (n.path) {
          leaves.push(n.path)
        }
      }
    }
    walk(menus.value)
    return leaves[0] || '/dashboard'
  })

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
    homePath,
    setToken,
    clearToken,
    fetchUserInfo,
  }
})
