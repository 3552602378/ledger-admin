import request from '@/utils/request'

export interface CategoryVO {
  id: number
  type: string
  name: string
  status: number
}

export function listCategories(type?: string) {
  return request({
    url: '/finance/categories',
    method: 'get',
    params: { type },
  })
}

export function addCategory(data: { type: string; name: string; status: number }) {
  return request({
    url: '/finance/categories',
    method: 'post',
    data,
  })
}

export function updateCategory(id: number, data: { type: string; name: string; status: number }) {
  return request({
    url: `/finance/categories/${id}`,
    method: 'put',
    data,
  })
}

export function deleteCategory(id: number) {
  return request({
    url: `/finance/categories/${id}`,
    method: 'delete',
  })
}