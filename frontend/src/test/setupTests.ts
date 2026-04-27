import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll } from "vitest";
import { setupServer } from "msw/node";
import { http, HttpResponse } from "msw";

/**
 * antd 在测试环境依赖 matchMedia，这里提供最小实现。
 */
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false
  })
});

/**
 * rc-table 依赖 getComputedStyle，这里补一个稳定实现。
 */
Object.defineProperty(window, "getComputedStyle", {
  value: () =>
    ({
      getPropertyValue: () => ""
    }) as unknown as CSSStyleDeclaration
});

/**
 * 全局 MSW 服务。
 */
export const server = setupServer(
  http.post("/api/v1/auth/login", async () => {
    return HttpResponse.json({
      code: 0,
      msg: "success",
      data: {
        accessToken: "access-token",
        refreshToken: "refresh-token",
        userInfo: {
          userId: 1,
          username: "admin"
        }
      }
    });
  }),
  http.get("/api/v1/workspaces", async () => {
    return HttpResponse.json({
      code: 0,
      msg: "success",
      data: [
        {
          id: 1001,
          name: "测试知识库",
          role: "owner"
        }
      ]
    });
  }),
  http.post("/api/v1/workspaces", async () => {
    return HttpResponse.json({
      code: 0,
      msg: "success",
      data: {
        id: 1002,
        name: "新建知识库",
        role: "owner"
      }
    });
  }),
  http.get("/api/v1/workspaces/1001/documents", async () => {
    return HttpResponse.json({
      code: 0,
      msg: "success",
      data: [
        {
          id: 5001,
          workspaceId: 1001,
          title: "文档一",
          status: "draft",
          latestVersionNo: 1
        }
      ]
    });
  }),
  http.post("/api/v1/workspaces/1001/documents", async () => {
    return HttpResponse.json({
      code: 0,
      msg: "success",
      data: {
        id: 5002,
        workspaceId: 1001,
        title: "新文档",
        status: "draft",
        latestVersionNo: 1
      }
    });
  })
);

/**
 * 在所有测试前启动 MSW。
 */
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));

/**
 * 每个测试后重置 handler。
 */
afterEach(() => server.resetHandlers());

/**
 * 所有测试完成后关闭服务。
 */
afterAll(() => server.close());

