import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { LoginPage } from "./LoginPage";
import { useAuthStore } from "../store/useAuthStore";

/**
 * 登录页测试。
 */
describe("LoginPage", () => {
  /**
   * 每个用例前重置认证状态，避免相互污染。
   */
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  /**
   * 测试：用户输入账号密码并提交后，应写入登录状态。
   */
  it("should set auth store when login success", async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    );
    const inputs = screen.getAllByRole("textbox");
    await user.clear(inputs[0]);
    await user.type(inputs[0], "admin");
    const passwordInput = document.querySelector("input[type='password']") as HTMLInputElement;
    await user.clear(passwordInput);
    await user.type(passwordInput, "123456");
    await user.click(screen.getByRole("button", { name: /登\s*录/ }));

    expect(useAuthStore.getState().accessToken).toBe("access-token");
  });
});

