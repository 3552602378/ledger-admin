import request from '@/utils/request'

export interface OperLogQuery {
  pageNum?: number
  pageSize?: number
  /** 操作人（模糊） */
  username?: string
  /** 模块标题（模糊） */
  title?: string
  /** 业务类型 1新增 2修改 3删除 4导出 5登录 */
  businessType?: number | ''
  /** 状态 1成功 0失败 */
  status?: number | ''
  /** yyyy-MM-dd */
  startDate?: string
  /** yyyy-MM-dd */
  endDate?: string
}

export interface OperLogVO {
  id: number
  username: string
  title: string
  businessType: number
  requestMethod: string
  requestUrl: string
  requestParams: string
  responseResult: string
  ip: string
  costTime: number
  status: number
  createTime: string
}

export function listOperLogs(params: OperLogQuery) {
  return request({
    url: '/audit/logs',
    method: 'get',
    params,
  })
}
