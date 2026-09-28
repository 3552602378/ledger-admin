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

export function batchDeleteRecords(ids: number[]) {
  return request({
    url: '/finance/records/batch',
    method: 'delete',
    data: ids,
  })
}

export interface RecordExportParams extends Partial<RecordQuery> {
  /** 批量导出选中的记录 id，非空时优先级最高 */
  ids?: number[]
  /** true 表示导出全量 */
  exportAll?: boolean
}

export function exportRecords(data: RecordExportParams) {
  return request({
    url: '/export/records',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 60000,
  })
}