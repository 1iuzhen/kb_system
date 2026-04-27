import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { DocumentPage } from "./DocumentPage";

/**
 * 文档列表页面测试。
 */
describe("DocumentPage", () => {
  /**
   * 测试：加载后应展示文档列表。
   */
  it("should render document list after load", async () => {
    render(
      <MemoryRouter initialEntries={["/workspaces/1001/documents"]}>
        <Routes>
          <Route path="/workspaces/:workspaceId/documents" element={<DocumentPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getAllByText("文档一").length).toBeGreaterThan(0);
    });
  });
});

