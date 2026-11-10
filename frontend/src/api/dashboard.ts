import request from '@/utils/request'

export interface DashboardStats {
  months: string[]
  monthIncome: number[]
  monthExpense: number[]
  days: string[]
  dayIncome: number[]
  dayExpense: number[]
  categories: { name: string; type: string; amount: number }[]
}

/** 看板筛选条件：type 为空表示全部，income/expense 限定收支类型 */
export interface DashboardQuery {
  type?: string
}

export function getDashboardStats(params?: DashboardQuery) {
  return request({
    url: '/dashboard/stats',
    method: 'get',
    params,
  })
}
