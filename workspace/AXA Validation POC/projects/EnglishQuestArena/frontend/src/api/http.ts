import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// 401 → 清除本地状态，跳转登录
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      // 避免在登录页时循环跳转
      if (!window.location.hash.includes('/login')) {
        window.location.hash = '#/login'
      }
    }
    return Promise.reject(err)
  },
)

export default http
