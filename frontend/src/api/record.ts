import request from '@/utils/request'

export interface RecordVO {
  id: number
  userId: number
  type: string
  categoryId: number
  categoryName?: string
  amount: number
  recordDate: string
  remark?: string
}

export interface StatisticsVO {
  totalIncome: number
  totalExpense: number
  balance: number
}

export interface RecordQuery {
  pageNum: number
  pageSize: number
  dateType?: string
  startDate?: string
  endDate?: string
  type?: string
  categoryId?: number
  remark?: string
}

export function listRecords(params: RecordQuery) {
  return request({
    url: '/finance/records',
    method: 'get',
    params,
  })
}

export function getStatistics(params: Partial<RecordQuery>) {
  return request({
    url: '/finance/records/statistics',
    method: 'get',
    params,
  })
}

export function addRecord(data: Partial<RecordVO>) {
  return request({
    url: '/finance/records',
    method: 'post',
    data,
  })
}

export function updateRecord(id: number, data: Partial<RecordVO>) {
  return request({
    url: `/finance/records/${id}`,
    method: 'put',
    data,
  })
}

export function deleteRecord(id: number) {
  return request({
    url: `/finance/records/${id}`,
    method: 'delete',
  })
}