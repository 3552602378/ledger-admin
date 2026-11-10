import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    const { response, config } = error
    const status = response?.status

    // 导出等 blob 请求失败时，响应体是 Blob 包着的 JSON，需要先还原才能读到 message
    const parseMessage = async (): Promise<string | undefined> => {
      const data = response?.data
      if (data instanceof Blob) {
        try {
          return JSON.parse(await data.text())?.message
        } catch {
          return undefined
        }
      }
      return data?.message
    }

    if (status === 401) {
      // 登录接口自身的 401 是凭据错误，直接提示服务端文案，不做登出跳转
      if (config?.url?.includes('/auth/login')) {
        ElMessage.error(response?.data?.message || '用户名或密码错误')
        return Promise.reject(error)
      }
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(error)
    }

    return parseMessage().then((message) => {
      if (status === 403) {
        ElMessage.error(message || '没有权限访问该资源')
      } else {
        ElMessage.error(message || '网络异常')
      }
      return Promise.reject(error)
    })
  },
)

export default request
