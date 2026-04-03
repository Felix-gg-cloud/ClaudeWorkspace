import axios from 'axios'
import router from '@/router'
import { showToast } from '@/composables/useToast'

const http = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ll_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      localStorage.removeItem('ll_token')
      localStorage.removeItem('ll_user')
      router.push('/login')
    } else if (!status || status >= 500) {
      // 仅对服务器错误和网络错误显示全局 toast
      // 4xx 业务错误由各页面自行处理
      const msg = err.response?.data?.message || err.message || '网络异常，请稍后重试'
      showToast(msg)
    }
    return Promise.reject(err)
  },
)

export default http
