import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layouts/Layout.vue'
import LoginView from '@/views/LoginView.vue'
import { useUserStore } from '@/stores/user'

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '首页', perm: 'dashboard:view' },
      },
      {
        path: '/system/users',
        name: 'User',
        component: () => import('@/views/UserView.vue'),
        meta: { title: '用户管理', perm: 'system:user:view' },
      },
      {
        path: '/system/roles',
        name: 'Role',
        component: () => import('@/views/RoleView.vue'),
        meta: { title: '角色管理', perm: 'system:role:view' },
      },
      {
        path: '/system/menus',
        name: 'Menu',
        component: () => import('@/views/MenuView.vue'),
        meta: { title: '菜单管理', perm: 'system:menu:view' },
      },
      {
        path: '/finance/categories',
        name: 'Category',
        component: () => import('@/views/CategoryView.vue'),
        meta: { title: '收支分类', perm: 'finance:category:view' },
      },
      {
        path: '/finance/records',
        name: 'Record',
        component: () => import('@/views/RecordView.vue'),
        meta: { title: '收支记录', perm: 'finance:record:view' },
      },
      {
        path: '/audit/logs',
        name: 'OperLog',
        component: () => import('@/views/OperLogView.vue'),
        meta: { title: '操作日志', perm: 'audit:log:view' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    if (userStore.isLoggedIn && to.path === '/login') {
      return next(userStore.homePath)
    }
    return next()
  }

  if (!userStore.isLoggedIn) {
    return next('/login')
  }

  if (userStore.menus.length === 0) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.clearToken()
      return next('/login')
    }
  }

  const perm = to.meta.perm as string
  if (perm && !userStore.perms.includes(perm) && !userStore.perms.includes('*:*:*')) {
    // 越权访问时跳转到用户可访问的首个菜单，避免跳回被拦目标造成死循环
    const home = userStore.homePath
    if (home === to.path) return next()
    return next(home)
  }

  next()
})

export default router
