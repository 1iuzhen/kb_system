import { http, type ApiResult } from "../../../shared/api/http";
import type { LoginResponse } from "../types";

/**
 * 登录请求参数。
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * 调用登录接口。
 *
 * @param payload 登录参数
 * @returns 登录结果
 */
export async function loginApi(payload: LoginRequest): Promise<LoginResponse> {
  const resp = await http.post<ApiResult<LoginResponse>>("/auth/login", payload);
  return resp.data.data;
}

