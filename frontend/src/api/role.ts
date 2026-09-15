import request from '@/utils/request'

export interface RoleVO {
  id: number
  roleName: string
  roleCode: string
  status: number
  remark?: string
  createTime?: string
}

export interface RoleSaveDTO {
  id?: number
  roleName: string
  roleCode: string
  status: number
  remark?: string
  menuIds: number[]
}

export function listRoles() {
  return request({
    url: '/system/roles',
    method: 'get',
  })
}

export function addRole(data: RoleSaveDTO) {
  return request({
    url: '/system/roles',
    method: 'post',
    data,
  })
}

export function updateRole(id: number, data: RoleSaveDTO) {
  return request({
    url: `/system/roles/${id}`,
    method: 'put',
    data,
  })
}

export function deleteRole(id: number) {
  return request({
    url: `/system/roles/${id}`,
    method: 'delete',
  })
}

export function getRoleMenus(id: number) {
  return request({
    url: `/system/roles/${id}/menus`,
    method: 'get',
  })
}