import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useUserStore } from './user'

export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore()

  const hasPerm = computed(() => (perm: string) => {
    return userStore.perms.includes(perm) || userStore.perms.includes('*:*:*')
  })

  return {
    hasPerm,
  }
})
