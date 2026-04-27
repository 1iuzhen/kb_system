import axios, { AxiosError } from "axios";
import { useAuthStore } from "../../features/auth/store/useAuthStore";

/**
 * 通用响应结构。
 */
export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}

/**
 * 统一 axios 实例。
 */
export const http = axios.create({
  baseURL: "/api/v1",
  timeout: 10000
});

/**
 * 请求拦截器：自动注入 Bearer token。
 */
http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 响应拦截器：把 Result<T> 业务错误统一抛出。
 */
http.interceptors.response.use(
  (resp) => {
    const payload = resp.data as ApiResult<unknown>;
    if (payload.code !== 0) {
      throw new Error(payload.msg);
    }
    return resp;
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    throw error;
  }
);

