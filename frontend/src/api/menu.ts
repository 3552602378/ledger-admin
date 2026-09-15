import request from '@/utils/request'

export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path?: string | null
  component?: string | null
  perms?: string | null
  icon?: string | null
  sortOrder?: number
  status?: number
  children?: MenuNode[]
}

export function getMenuTree() {
  return request({
    url: '/system/menus/tree',
    method: 'get',
  })
}