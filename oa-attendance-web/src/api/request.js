import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('oa_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result.code !== 200) return Promise.reject(new Error(result.msg || '请求失败'))
    return result
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('oa_token')
      window.dispatchEvent(new CustomEvent('oa-unauthorized'))
    }
    return Promise.reject(new Error(error.response?.data?.msg || error.message || '网络请求失败'))
  },
)

export default request
