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

export function getDashboardStats() {
  return request({
    url: '/dashboard/stats',
    method: 'get',
  })
}