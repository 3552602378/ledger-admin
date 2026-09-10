import request from '@/utils/request'

export interface LoginData {
  username: string
  password: string
}

export function login(data: LoginData) {
  return request({
    url: '/auth/login',
    method: 'post',
    data,
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get',
  })
}
