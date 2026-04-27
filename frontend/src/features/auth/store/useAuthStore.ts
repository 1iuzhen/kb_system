import { create } from "zustand";
import type { UserInfo } from "../types";

/**
 * 认证状态定义。
 */
interface AuthState {
  accessToken: string;
  refreshToken: string;
  userInfo: UserInfo | null;
  setLogin: (token: string, refreshToken: string, userInfo: UserInfo) => void;
  logout: () => void;
}

/**
 * 认证状态仓库。
 */
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: "",
  refreshToken: "",
  userInfo: null,
  setLogin: (token, refreshToken, userInfo) =>
    set({
      accessToken: token,
      refreshToken,
      userInfo
    }),
  logout: () =>
    set({
      accessToken: "",
      refreshToken: "",
      userInfo: null
    })
}));

