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

export interface MenuSaveDTO {
  id?: number
  parentId: number
  menuName: string
  menuType: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  sortOrder?: number
  status: number
}

export function addMenu(data: MenuSaveDTO) {
  return request({
    url: '/system/menus',
    method: 'post',
    data,
  })
}

export function updateMenu(id: number, data: MenuSaveDTO) {
  return request({
    url: `/system/menus/${id}`,
    method: 'put',
    data,
  })
}

export function deleteMenu(id: number) {
  return request({
    url: `/system/menus/${id}`,
    method: 'delete',
  })
}