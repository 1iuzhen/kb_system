import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { WorkspacePage } from "./WorkspacePage";
import { useWorkspaceStore } from "../store/useWorkspaceStore";
import { MemoryRouter } from "react-router-dom";

/**
 * 知识库页面测试。
 */
describe("WorkspacePage", () => {
  /**
   * 每个用例前重置 store 数据，避免互相影响。
   */
  beforeEach(() => {
    useWorkspaceStore.setState({ workspaces: [], loading: false });
  });

  /**
   * 测试：页面初始化应加载并展示知识库列表。
   */
  it("should render workspace list after load", async () => {
    render(
      <MemoryRouter>
        <WorkspacePage />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getAllByText("测试知识库").length).toBeGreaterThan(0);
    });
  });
});

