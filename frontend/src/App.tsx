import { Button, Layout, Space, Typography } from "antd";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { LoginPage } from "./features/auth/pages/LoginPage";
import { WorkspacePage } from "./features/workspace/pages/WorkspacePage";
import { useAuthStore } from "./features/auth/store/useAuthStore";
import { DocumentPage } from "./features/doc/pages/DocumentPage";
import { DocumentEditorPage } from "./features/doc/pages/DocumentEditorPage";
import { ChatPage } from "./features/chat/pages/ChatPage";

/**
 * 应用根组件。
 */
export function App(): JSX.Element {
  const token = useAuthStore((s) => s.accessToken);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Layout.Header style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography.Title style={{ color: "#fff", margin: 0 }} level={4}>
          智能知识库系统
        </Typography.Title>
        {token ? (
          <Space>
            <Button
              onClick={() => {
                logout();
                navigate("/login");
              }}
            >
              退出登录
            </Button>
          </Space>
        ) : null}
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/workspaces" element={token ? <WorkspacePage /> : <Navigate to="/login" replace />} />
          <Route path="/workspaces/:workspaceId/documents" element={token ? <DocumentPage /> : <Navigate to="/login" replace />} />
          <Route path="/workspaces/:workspaceId/chat" element={token ? <ChatPage /> : <Navigate to="/login" replace />} />
          <Route
            path="/workspaces/:workspaceId/documents/:documentId"
            element={token ? <DocumentEditorPage /> : <Navigate to="/login" replace />}
          />
          <Route path="*" element={<Navigate to={token ? "/workspaces" : "/login"} replace />} />
        </Routes>
      </Layout.Content>
    </Layout>
  );
}

