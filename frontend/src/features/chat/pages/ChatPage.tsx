import { Button, Card, Input, List, Select, Space, Tag, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useWorkspaceStore } from "../../workspace/store/useWorkspaceStore";
import { useChatStore } from "../store/useChatStore";

/**
 * RAG 对话页面。
 */
export function ChatPage(): JSX.Element {
  const { workspaceId } = useParams();
  const navigate = useNavigate();
  const workspaceIdNum = Number(workspaceId);
  const { workspaces, loadWorkspaces } = useWorkspaceStore();
  const { messages, sources, streaming, ask, clear, loadWorkspaceConversation } = useChatStore();
  const [question, setQuestion] = useState("");

  useEffect(() => {
    void loadWorkspaces();
  }, [loadWorkspaces]);

  useEffect(() => {
    if (Number.isNaN(workspaceIdNum)) {
      return;
    }
    loadWorkspaceConversation(workspaceIdNum);
  }, [workspaceIdNum, loadWorkspaceConversation]);

  /**
   * 当前知识库名称。
   */
  const currentWorkspaceName = useMemo(() => {
    const hit = workspaces.find((workspace) => workspace.id === workspaceIdNum);
    return hit?.name ?? `知识库 ${workspaceIdNum}`;
  }, [workspaces, workspaceIdNum]);

  return (
    <Space direction="vertical" style={{ width: "100%" }} size={16}>
      <Card>
        <Space style={{ width: "100%", justifyContent: "space-between" }}>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {currentWorkspaceName} · 智能问答
          </Typography.Title>
          <Space>
            <Select
              value={workspaceIdNum}
              style={{ minWidth: 220 }}
              placeholder="切换知识库"
              options={workspaces.map((workspace) => ({
                value: workspace.id,
                label: workspace.name
              }))}
              onChange={(targetWorkspaceId) => {
                navigate(`/workspaces/${targetWorkspaceId}/chat`);
              }}
            />
            <Button onClick={() => navigate(`/workspaces/${workspaceIdNum}/documents`)}>返回文档</Button>
            <Button onClick={() => clear(workspaceIdNum)} disabled={streaming}>
              清空会话
            </Button>
          </Space>
        </Space>
      </Card>

      <Card>
        <Space.Compact style={{ width: "100%" }}>
          <Input.TextArea
            value={question}
            autoSize={{ minRows: 2, maxRows: 6 }}
            placeholder="请输入问题，例如：这个知识库里关于简历优化有哪些建议？"
            onChange={(event) => setQuestion(event.target.value)}
            disabled={streaming}
          />
          <Button
            type="primary"
            loading={streaming}
            onClick={async () => {
              await ask(workspaceIdNum, question);
              setQuestion("");
            }}
          >
            提问
          </Button>
        </Space.Compact>
      </Card>

      <Card title="对话记录">
        <List
          dataSource={messages}
          locale={{ emptyText: "暂无消息，开始提问吧" }}
          renderItem={(item) => (
            <List.Item>
              <Space direction="vertical" style={{ width: "100%" }} size={4}>
                <Tag color={item.role === "user" ? "blue" : "green"}>{item.role === "user" ? "你" : "助手"}</Tag>
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}>
                  {item.content}
                </Typography.Paragraph>
              </Space>
            </List.Item>
          )}
        />
      </Card>

      <Card title="引用来源">
        <List
          dataSource={sources}
          locale={{ emptyText: "暂无引用" }}
          renderItem={(source) => (
            <List.Item>
              <Space direction="vertical" style={{ width: "100%" }} size={2}>
                <Typography.Text strong>
                  {source.docTitle}（doc#{source.docId} / chunk#{source.chunkId}）
                </Typography.Text>
                <Typography.Text type="secondary">score: {source.score?.toFixed(4) ?? "-"}</Typography.Text>
                <Typography.Paragraph style={{ marginBottom: 0 }}>{source.snippet}</Typography.Paragraph>
                <Button
                  type="link"
                  style={{ paddingInline: 0 }}
                  onClick={() =>
                    navigate(`/workspaces/${workspaceIdNum}/documents/${source.docId}`, {
                      state: {
                        fromChat: true,
                        snippet: source.snippet
                      }
                    })
                  }
                >
                  跳转原文
                </Button>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </Space>
  );
}

