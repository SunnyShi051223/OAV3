import axios from 'axios';
import { authStore } from '../store/auth';

const http = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.msg || error.message || '请求失败';
    if (error.response?.status === 401) {
      authStore.clear();
    }
    return Promise.reject(new Error(message));
  }
);

export default http;
