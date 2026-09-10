import { useUserStore } from '@/stores/user'
import type { Directive } from 'vue'

export const permission: Directive = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const value = binding.value
    if (!value) return

    const hasPerm = userStore.perms.includes(value) || userStore.perms.includes('*:*:*')
    if (!hasPerm) {
      el.parentNode?.removeChild(el)
    }
  },
}
