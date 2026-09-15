import request from '@/utils/request'

export interface UserVO {
  id: number
  username: string
  nickname: string
  status: number
  createTime: string
}

export interface UserQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

export interface UserSaveDTO {
  id?: number
  username: string
  nickname: string
  password?: string
  status: number
  roleId?: number
}

export interface PageResult<T> {
  total: number
  rows: T[]
}

export function listUsers(params: UserQuery) {
  return request({
    url: '/system/users',
    method: 'get',
    params,
  })
}

export function addUser(data: UserSaveDTO) {
  return request({
    url: '/system/users',
    method: 'post',
    data,
  })
}

export function updateUser(id: number, data: UserSaveDTO) {
  return request({
    url: `/system/users/${id}`,
    method: 'put',
    data,
  })
}

export function updateUserStatus(id: number, status: number) {
  return request({
    url: `/system/users/${id}/status`,
    method: 'put',
    params: { status },
  })
}

export function resetUserPassword(id: number) {
  return request({
    url: `/system/users/${id}/reset-password`,
    method: 'put',
  })
}

export function deleteUser(id: number) {
  return request({
    url: `/system/users/${id}`,
    method: 'delete',
  })
}