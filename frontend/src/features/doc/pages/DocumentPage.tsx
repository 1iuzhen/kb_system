import { Badge, Button, Card, Drawer, Form, Input, List, Modal, Select, Space, Spin, Table, Tag, Typography, message } from "antd";
import type { MenuProps } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { DocumentItem, DocumentVersion } from "../types";
import { useDocStore } from "../store/useDocStore";
import { useWorkspaceStore } from "../../workspace/store/useWorkspaceStore";
import { VditorEditor } from "../components/VditorEditor";
import { DeleteOutlined, EditOutlined, MoreOutlined } from "@ant-design/icons";
import { Dropdown } from "antd";
import { getDocumentApi } from "../api/docApi";

/**
 * 文档列表页面。
 */
/**
 * 获取文档状态对应的标签颜色。
 *
 * @param status 状态值
 * @returns antd Tag 颜色
 */
function statusColor(status: string): string {
  if (status === "failed") {
    return "error";
  }
  if (status === "parsed") {
    return "success";
  }
  if (status === "parsing" || status === "uploading") {
    return "processing";
  }
  return "default";
}

export function DocumentPage(): JSX.Element {
  const { workspaceId } = useParams();
  const navigate = useNavigate();
  const workspaceIdNum = Number(workspaceId);
  const {
    documents,
    detail,
    loading,
    loadDocuments,
    createDocument,
    uploadDocument,
    retryParseDocument,
    extractVector,
    loadDetail,
    saveDocument,
    deleteDocument
  } = useDocStore();
  const { workspaces, loadWorkspaces } = useWorkspaceStore();
  const [versionDocId, setVersionDocId] = useState<number | null>(null);
  const { versions, loadVersions, rollbackVersion, loadVersionDetail, versionDetail } = useDocStore();
  const [versionPreviewOpen, setVersionPreviewOpen] = useState(false);
  const [quickCreateTitle, setQuickCreateTitle] = useState("");
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);
  const [quickCreating, setQuickCreating] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [retryingDocIds, setRetryingDocIds] = useState<number[]>([]);
  const [activeDocumentId, setActiveDocumentId] = useState<number | null>(null);
  const [activeTitle, setActiveTitle] = useState("");
  const [activeContent, setActiveContent] = useState("");
  const [activeLoading, setActiveLoading] = useState(false);
  const [activeSaving, setActiveSaving] = useState(false);
  const [activeExtracting, setActiveExtracting] = useState(false);
  const [deletingDocIds, setDeletingDocIds] = useState<number[]>([]);
  const [renamingDocIds, setRenamingDocIds] = useState<number[]>([]);

  useEffect(() => {
    if (!Number.isNaN(workspaceIdNum)) {
      void loadDocuments(workspaceIdNum);
    }
  }, [workspaceIdNum, loadDocuments]);

  useEffect(() => {
    if (Number.isNaN(workspaceIdNum)) {
      return;
    }
    const hasParsingDoc = documents.some((doc) => doc.status === "uploading" || doc.status === "parsing");
    if (!hasParsingDoc) {
      return;
    }
    /**
     * 解析中状态轮询：2 秒刷新一次文档列表，直到状态落定。
     */
    const timer = window.setInterval(() => {
      void loadDocuments(workspaceIdNum);
    }, 2000);
    return () => window.clearInterval(timer);
  }, [documents, workspaceIdNum, loadDocuments]);

  useEffect(() => {
    void loadWorkspaces();
  }, [loadWorkspaces]);

  /**
   * 当前知识库展示名称。
   */
  const currentWorkspaceName = useMemo(() => {
    const hit = workspaces.find((workspace) => workspace.id === workspaceIdNum);
    return hit?.name ?? `知识库 ${workspaceIdNum}`;
  }, [workspaces, workspaceIdNum]);

  /**
   * 创建文档。
   *
   * @param values 表单值
   */
  const onCreate = async (values: { title: string }): Promise<void> => {
    try {
      await createDocument(workspaceIdNum, values.title);
      message.success("文档创建成功");
    } catch (error) {
      const msg = error instanceof Error ? error.message : "文档创建失败";
      message.error(msg);
    }
  };

  /**
   * 在当前页面打开指定文档。
   * 右侧主区域会从“首页卡片”切换到“文档编辑”。
   *
   * @param documentId 文档 ID
   */
  const openDocumentInCurrentPage = async (documentId: number): Promise<void> => {
    try {
      setActiveLoading(true);
      setActiveDocumentId(documentId);
      await loadDetail(workspaceIdNum, documentId);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "文档加载失败";
      message.error(msg);
      setActiveDocumentId(null);
    } finally {
      setActiveLoading(false);
    }
  };

  /**
   * 保存当前抽屉内文档。
   */
  const saveActiveDocument = async (): Promise<void> => {
    if (!activeDocumentId || !detail) {
      return;
    }
    try {
      setActiveSaving(true);
      await saveDocument(workspaceIdNum, activeDocumentId, activeTitle, activeContent, detail.latestVersionNo);
      message.success("保存成功");
      await loadDocuments(workspaceIdNum);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "保存失败";
      message.error(msg);
    } finally {
      setActiveSaving(false);
    }
  };

  /**
   * 手动提取当前文档到向量知识库。
   * 规则：若已存在旧向量，后端会先删除再重建。
   */
  const extractActiveDocumentVector = async (): Promise<void> => {
    if (!activeDocumentId) {
      return;
    }
    try {
      setActiveExtracting(true);
      await extractVector(workspaceIdNum, activeDocumentId);
      message.success("已提取到向量知识库（旧向量已自动替换）");
      await loadDocuments(workspaceIdNum);
      await loadDetail(workspaceIdNum, activeDocumentId);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "提取向量失败";
      message.error(msg);
    } finally {
      setActiveExtracting(false);
    }
  };

  /**
   * 将已加载的详情同步到抽屉本地草稿。
   */
  useEffect(() => {
    if (!detail || activeDocumentId === null || detail.id !== activeDocumentId) {
      return;
    }
    setActiveTitle(detail.title ?? "");
    setActiveContent(detail.content ?? "");
  }, [detail, activeDocumentId]);

  /**
   * 侧边栏快捷创建文档。
   */
  const onQuickCreate = async (): Promise<void> => {
    const trimmedTitle = quickCreateTitle.trim();
    if (!trimmedTitle) {
      message.warning("请输入文档标题");
      return;
    }
    try {
      setQuickCreating(true);
      const created = await createDocument(workspaceIdNum, trimmedTitle);
      setQuickCreateTitle("");
      setQuickCreateOpen(false);
      message.success("文档创建成功");
      await openDocumentInCurrentPage(created.id);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "文档创建失败";
      message.error(msg);
    } finally {
      setQuickCreating(false);
    }
  };

  /**
   * 打开版本抽屉。
   *
   * @param documentId 文档 ID
   */
  const openVersions = async (documentId: number): Promise<void> => {
    setVersionDocId(documentId);
    await loadVersions(workspaceIdNum, documentId);
  };

  /**
   * 执行回滚。
   *
   * @param versionNo 目标版本号
   */
  const onRollback = (versionNo: number): void => {
    if (!versionDocId) {
      return;
    }
    Modal.confirm({
      title: `确认回滚到版本 ${versionNo}？`,
      onOk: async () => {
        try {
          await rollbackVersion(workspaceIdNum, versionDocId, versionNo);
          message.success("回滚成功");
        } catch (error) {
          const msg = error instanceof Error ? error.message : "回滚失败";
          message.error(msg);
        }
      }
    });
  };

  /**
   * 删除文档。
   *
   * @param documentId 文档 ID
   */
  const onDeleteDocument = (documentId: number): void => {
    Modal.confirm({
      title: "确认删除该文档？",
      content: "删除后无法恢复，且会同时删除该文档的版本与向量数据。",
      okText: "删除",
      cancelText: "取消",
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          setDeletingDocIds((prev) => (prev.includes(documentId) ? prev : [...prev, documentId]));
          await deleteDocument(workspaceIdNum, documentId);
          if (activeDocumentId === documentId) {
            setActiveDocumentId(null);
          }
          message.success("文档已删除");
        } catch (error) {
          const msg = error instanceof Error ? error.message : "删除文档失败";
          message.error(msg);
        } finally {
          setDeletingDocIds((prev) => prev.filter((id) => id !== documentId));
        }
      }
    });
  };

  /**
   * 重命名文档。
   * 复用保存接口：只更新标题，正文保持当前最新内容不变。
   *
   * @param doc 当前文档
   */
  const onRenameDocument = (doc: DocumentItem): void => {
    Modal.confirm({
      title: "重命名文档",
      content: (
        <Input
          id={`rename-doc-${doc.id}`}
          defaultValue={doc.title}
          placeholder="请输入新标题"
          maxLength={128}
        />
      ),
      okText: "确定",
      cancelText: "取消",
      onOk: async () => {
        const input = document.getElementById(`rename-doc-${doc.id}`) as HTMLInputElement | null;
        const nextTitle = input?.value?.trim() ?? "";
        if (!nextTitle) {
          message.error("标题不能为空");
          return;
        }
        if (nextTitle === doc.title) {
          return;
        }
        try {
          setRenamingDocIds((prev) => (prev.includes(doc.id) ? prev : [...prev, doc.id]));
          const detailResp = await getDocumentDetailForRename(doc.id);
          await saveDocument(workspaceIdNum, doc.id, nextTitle, detailResp.content ?? "", detailResp.latestVersionNo);
          // 若当前正在编辑该文档，需要同步右侧标题展示。
          if (activeDocumentId === doc.id) {
            setActiveTitle(nextTitle);
          }
          message.success("重命名成功");
        } catch (error) {
          const msg = error instanceof Error ? error.message : "重命名失败";
          message.error(msg);
        } finally {
          setRenamingDocIds((prev) => prev.filter((id) => id !== doc.id));
        }
      }
    });
  };

  /**
   * 为重命名获取文档详情快照。
   * 说明：重命名时需要带上当前 content 与 baseVersion 调用保存接口。
   *
   * @param documentId 文档 ID
   * @returns 文档详情
   */
  const getDocumentDetailForRename = async (documentId: number): Promise<{ content: string; latestVersionNo: number }> => {
    // 直接请求详情，避免依赖 store 异步更新时序造成 baseVersion 不准确。
    const resp = await getDocumentApi(workspaceIdNum, documentId);
    if (activeDocumentId === documentId) {
      // 当前正在编辑时优先使用本地草稿正文，避免覆盖用户未保存编辑。
      return {
        content: activeContent,
        latestVersionNo: resp.latestVersionNo
      };
    }
    return {
      content: resp.content ?? "",
      latestVersionNo: resp.latestVersionNo
    };
  };

  /**
   * 左侧文档行的三点菜单项。
   *
   * @param doc 文档
   * @returns 菜单项
   */
  const docActionMenuItems = (doc: DocumentItem): MenuProps["items"] => [
    {
      key: "rename",
      label: "重命名文档",
      icon: <EditOutlined />
    },
    {
      key: "delete",
      label: "删除文档",
      danger: true,
      icon: <DeleteOutlined />
    }
  ];

  /**
   * 处理文档三点菜单点击。
   *
   * @param doc 文档
   * @param key 菜单 key
   */
  const onDocActionMenuClick = (doc: DocumentItem, key: string): void => {
    if (key === "rename") {
      onRenameDocument(doc);
      return;
    }
    if (key === "delete") {
      onDeleteDocument(doc.id);
    }
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: 16, alignItems: "start" }}>
      <Card title="同知识库文档" size="small" styles={{ body: { padding: 0 } }}>
        <div style={{ padding: 12, borderBottom: "1px solid #f0f0f0" }}>
          <Space.Compact style={{ width: "100%" }}>
            <Button
              type="primary"
              onClick={() => setQuickCreateOpen(true)}
              disabled={uploading}
              loading={quickCreating}
            >
              新建文档
            </Button>
            <input
              type="file"
              accept=".md,.pdf,.docx"
              style={{ display: "none" }}
              id="doc-upload-input"
              onChange={async (event) => {
                const file = event.target.files?.[0];
                if (!file) {
                  return;
                }
                try {
                  setUploading(true);
                  const created = await uploadDocument(workspaceIdNum, file);
                  message.success("上传并解析成功");
                  await openDocumentInCurrentPage(created.id);
                } catch (error) {
                  const msg = error instanceof Error ? error.message : "上传解析失败";
                  message.error(msg);
                } finally {
                  setUploading(false);
                  // 清空 value，支持同文件重复上传。
                  event.target.value = "";
                }
              }}
            />
            <Button
              loading={uploading}
              disabled={uploading}
              onClick={() => {
                const input = document.getElementById("doc-upload-input") as HTMLInputElement | null;
                input?.click();
              }}
            >
              上传文档
            </Button>
          </Space.Compact>
        </div>
        <List
          loading={loading}
          dataSource={documents}
          locale={{ emptyText: "暂无文档" }}
          header={
            <Button
              type="text"
              style={{ width: "100%", textAlign: "left", paddingInline: 12 }}
              onClick={() => setActiveDocumentId(null)}
            >
              首页
            </Button>
          }
          renderItem={(doc) => (
            <List.Item
              style={{
                padding: "8px 12px",
                cursor: "pointer",
                backgroundColor: doc.id === activeDocumentId ? "#e6f4ff" : "transparent"
              }}
              onClick={() => void openDocumentInCurrentPage(doc.id)}
              actions={[
                <Dropdown
                  key="more"
                  trigger={["click"]}
                  menu={{
                    items: docActionMenuItems(doc),
                    onClick: (info) => {
                      onDocActionMenuClick(doc, info.key);
                    }
                  }}
                >
                  <Button
                    type="text"
                    icon={<MoreOutlined />}
                    loading={deletingDocIds.includes(doc.id) || renamingDocIds.includes(doc.id)}
                    onClick={(event) => {
                      event.stopPropagation();
                    }}
                  />
                </Dropdown>
              ]}
            >
              <List.Item.Meta
                title={
                  <Space size={8}>
                    <Badge dot color="#ff4d4f" />
                    <Typography.Text ellipsis={{ tooltip: doc.title }}>{doc.title}</Typography.Text>
                  </Space>
                }
                description={
                  <Space direction="vertical" size={2}>
                    <Typography.Text type="secondary">v{doc.latestVersionNo} / {doc.status}</Typography.Text>
                    {doc.status === "failed" && doc.parseErrorMsg ? (
                      <Typography.Text type="danger" ellipsis={{ tooltip: doc.parseErrorMsg }}>
                        失败原因：{doc.parseErrorMsg}
                      </Typography.Text>
                    ) : null}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      <Space direction="vertical" style={{ width: "100%" }} size={16}>
        <Card>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Typography.Title level={5} style={{ margin: 0 }}>
              {currentWorkspaceName} · {activeDocumentId ? `${detail?.title ?? "文档"} 编辑` : "首页"}
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
                  navigate(`/workspaces/${targetWorkspaceId}/documents`);
                }}
              />
              <Button onClick={() => setActiveDocumentId(null)}>首页</Button>
              <Button onClick={() => navigate(`/workspaces/${workspaceIdNum}/chat`)}>智能问答</Button>
              <Button onClick={() => navigate("/workspaces")}>返回知识库</Button>
            </Space>
          </Space>
        </Card>

        {activeDocumentId === null ? (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
              gap: 16
            }}
          >
            {documents.map((doc: DocumentItem) => (
              <Card
                key={doc.id}
                title={doc.title}
                hoverable
                onClick={() => void openDocumentInCurrentPage(doc.id)}
                extra={
                  <Space>
                    <Tag color={statusColor(doc.status)}>{doc.status}</Tag>
                    <Tag color="blue">v{doc.latestVersionNo}</Tag>
                  </Space>
                }
                actions={[
                  <Button key={`version-${doc.id}`} type="link" onClick={() => void openVersions(doc.id)}>
                    版本历史
                  </Button>,
                  <Button
                    key={`delete-${doc.id}`}
                    type="link"
                    danger
                    icon={<DeleteOutlined />}
                    loading={deletingDocIds.includes(doc.id)}
                    onClick={(event) => {
                      event.stopPropagation();
                      onDeleteDocument(doc.id);
                    }}
                  >
                    删除
                  </Button>
                ]}
              >
                <Space direction="vertical" size={6}>
                  <Typography.Text type="secondary">文档 ID：{doc.id}</Typography.Text>
                  <Typography.Text type="secondary">状态：{doc.status}</Typography.Text>
                  {doc.status === "failed" && doc.parseErrorMsg ? (
                    <Typography.Text type="danger" ellipsis={{ tooltip: doc.parseErrorMsg }}>
                      失败原因：{doc.parseErrorMsg}
                    </Typography.Text>
                  ) : null}
                  {doc.status === "failed" ? (
                    <Button
                      size="small"
                      loading={retryingDocIds.includes(doc.id)}
                      onClick={async () => {
                        try {
                          setRetryingDocIds((prev) => (prev.includes(doc.id) ? prev : [...prev, doc.id]));
                          await retryParseDocument(workspaceIdNum, doc.id);
                          message.success("已触发重试解析，正在更新状态");
                        } catch (error) {
                          const msg = error instanceof Error ? error.message : "重试解析失败";
                          message.error(msg);
                        } finally {
                          setRetryingDocIds((prev) => prev.filter((id) => id !== doc.id));
                        }
                      }}
                    >
                      重试解析
                    </Button>
                  ) : null}
                </Space>
              </Card>
            ))}
          </div>
        ) : (
          <Card>
            {activeLoading ? (
              <div style={{ display: "flex", justifyContent: "center", padding: "48px 0" }}>
                <Spin tip="正在加载文档..." />
              </div>
            ) : (
              <Form layout="vertical">
                <Form.Item label="标题">
                  <Input value={activeTitle} onChange={(event) => setActiveTitle(event.target.value)} disabled={activeSaving} />
                </Form.Item>
                <Form.Item label="Markdown 内容">
                  <VditorEditor value={activeContent} onChange={setActiveContent} disabled={activeSaving} />
                </Form.Item>
                <Space style={{ width: "100%", justifyContent: "space-between" }}>
                  <Typography.Text type="secondary">
                    当前版本：{detail?.latestVersionNo ?? "-"} / 状态：{detail?.status ?? "-"}
                  </Typography.Text>
                  <Space>
                    <Button
                      loading={activeExtracting || detail?.indexStatus === "indexing"}
                      onClick={() => void extractActiveDocumentVector()}
                      disabled={!detail || (detail.latestVersionNo ?? 0) <= 0}
                    >
                      提取到向量知识库
                    </Button>
                    <Button type="primary" loading={activeSaving} onClick={() => void saveActiveDocument()}>
                      保存
                    </Button>
                  </Space>
                </Space>
                <Typography.Text type="secondary">
                  向量索引状态：{detail?.indexStatus ?? "not_indexed"} / 已索引版本：{detail?.indexedVersionNo ?? 0}
                </Typography.Text>
                {detail?.indexStatus === "failed" && detail?.indexErrorMsg ? (
                  <Typography.Text type="danger">索引失败原因：{detail.indexErrorMsg}</Typography.Text>
                ) : null}
              </Form>
            )}
          </Card>
        )}
      </Space>

      <Drawer title="版本历史" open={versionDocId !== null} onClose={() => setVersionDocId(null)} width={600}>
        <Table
          rowKey="id"
          dataSource={versions}
          pagination={false}
          columns={[
            { title: "版本号", dataIndex: "versionNo" },
            { title: "标题快照", dataIndex: "titleSnapshot" },
            { title: "保存人", dataIndex: "saveUsername", width: 120 },
            { title: "状态", dataIndex: "status" },
            { title: "创建时间", dataIndex: "createTime" },
            {
              title: "操作",
              render: (_, row: DocumentVersion) => (
                <Space>
                  <Button
                    onClick={async () => {
                      if (!versionDocId) {
                        return;
                      }
                      await loadVersionDetail(workspaceIdNum, versionDocId, row.versionNo);
                      setVersionPreviewOpen(true);
                    }}
                  >
                    查看
                  </Button>
                  <Button onClick={() => onRollback(row.versionNo)}>回滚到此版本</Button>
                </Space>
              )
            }
          ]}
        />
      </Drawer>

      <Drawer
        title={`版本内容预览（v${versionDetail?.versionNo ?? "-"})`}
        open={versionPreviewOpen}
        onClose={() => setVersionPreviewOpen(false)}
        width={720}
      >
        <Typography.Paragraph>
          <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>{versionDetail?.content ?? ""}</pre>
        </Typography.Paragraph>
      </Drawer>
      <Modal
        title="新建文档"
        open={quickCreateOpen}
        confirmLoading={quickCreating}
        onCancel={() => {
          if (quickCreating) {
            return;
          }
          setQuickCreateOpen(false);
        }}
        onOk={() => void onQuickCreate()}
        okText="创建"
        cancelText="取消"
      >
        <Form layout="vertical">
          <Form.Item label="文档标题" required>
            <Input
              value={quickCreateTitle}
              placeholder="请输入文档标题"
              onChange={(event) => setQuickCreateTitle(event.target.value)}
              onPressEnter={() => void onQuickCreate()}
              disabled={quickCreating}
              autoFocus
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

