/**
 * 用户信息。
 */
export interface UserInfo {
  userId: number;
  username: string;
}

/**
 * 登录返回体。
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userInfo: UserInfo;
}

