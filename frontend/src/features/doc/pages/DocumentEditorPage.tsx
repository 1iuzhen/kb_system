import { Alert, Badge, Button, Card, Drawer, Form, Input, List, Modal, Select, Space, Spin, Table, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useDocStore } from "../store/useDocStore";
import type { DocumentVersion } from "../types";
import { VditorEditor } from "../components/VditorEditor";
import { useWorkspaceStore } from "../../workspace/store/useWorkspaceStore";

/**
 * 自动保存防抖时间（毫秒）：连续无编辑满 5 分钟后再保存。
 * 仅当标题或正文相对上次已成功保存的快照有差异时才会发起请求。
 */
const AUTO_SAVE_DEBOUNCE_MS = 5 * 60 * 1000;

/**
 * 文档编辑页面。
 */
export function DocumentEditorPage(): JSX.Element {
  const { workspaceId, documentId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const workspaceIdNum = Number(workspaceId);
  const documentIdNum = Number(documentId);
  const {
    detail,
    documents,
    loading,
    loadDocuments,
    createDocument,
    retryParseDocument,
    extractVector,
    loadDetail,
    saveDocument,
    versions,
    loadVersions,
    loadVersionDetail,
    versionDetail
  } = useDocStore();
  const { workspaces, loadWorkspaces } = useWorkspaceStore();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [saving, setSaving] = useState(false);
  const [versionDrawerOpen, setVersionDrawerOpen] = useState(false);
  const [versionPreviewOpen, setVersionPreviewOpen] = useState(false);
  const [quickCreateTitle, setQuickCreateTitle] = useState("");
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);
  const [quickCreating, setQuickCreating] = useState(false);
  const [retryParsing, setRetryParsing] = useState(false);
  const [extractingVector, setExtractingVector] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailLoadError, setDetailLoadError] = useState<string | null>(null);
  const [hydratedFromServer, setHydratedFromServer] = useState(false);
  const [lastSavedTitle, setLastSavedTitle] = useState("");
  const [lastSavedContent, setLastSavedContent] = useState("");
  const chatSnippet = (location.state as { fromChat?: boolean; snippet?: string } | null)?.snippet ?? "";

  /**
   * 引用片段定位结果。
   * 用于在编辑页高亮展示匹配的片段和其前后文。
   */
  const snippetLocateResult = useMemo(() => {
    const snippet = chatSnippet.trim();
    const text = content ?? "";
    if (!snippet || !text) {
      return null;
    }
    const firstIndex = text.indexOf(snippet);
    if (firstIndex < 0) {
      return {
        matched: false,
        hitCount: 0,
        before: "",
        hit: snippet,
        after: ""
      };
    }
    let count = 0;
    let scanIndex = 0;
    while (scanIndex < text.length) {
      const idx = text.indexOf(snippet, scanIndex);
      if (idx < 0) {
        break;
      }
      count++;
      scanIndex = idx + snippet.length;
    }
    const contextSize = 80;
    const beforeStart = Math.max(0, firstIndex - contextSize);
    const afterEnd = Math.min(text.length, firstIndex + snippet.length + contextSize);
    return {
      matched: true,
      hitCount: count,
      before: text.slice(beforeStart, firstIndex),
      hit: snippet,
      after: text.slice(firstIndex + snippet.length, afterEnd)
    };
  }, [chatSnippet, content]);
  /** 始终指向当前编辑框中的标题与正文，供长延迟定时器读取，避免闭包过期。 */
  const titleRef = useRef(title);
  const contentRef = useRef(content);
  /** 上次已成功落库的快照，供定时器结束时再次对比是否仍“有改动”。 */
  const lastSavedTitleRef = useRef("");
  const lastSavedContentRef = useRef("");
  /** 最新的文档详情（含乐观锁版本号、角色），保存时从 ref 读取。 */
  const detailRef = useRef(detail);
  titleRef.current = title;
  contentRef.current = content;
  detailRef.current = detail;

  useEffect(() => {
    if (Number.isNaN(workspaceIdNum) || Number.isNaN(documentIdNum)) {
      return;
    }
    let cancelled = false;
    /**
     * 加载文档详情并暴露错误态，避免请求失败时页面仅表现为空白。
     */
    const fetchDetail = async (): Promise<void> => {
      setDetailLoading(true);
      setDetailLoadError(null);
      try {
        await loadDetail(workspaceIdNum, documentIdNum);
      } catch (error) {
        if (cancelled) {
          return;
        }
        const msg = error instanceof Error ? error.message : "文档加载失败";
        setDetailLoadError(msg);
      } finally {
        if (!cancelled) {
          setDetailLoading(false);
        }
      }
    };
    void fetchDetail();
    return () => {
      cancelled = true;
    };
  }, [workspaceIdNum, documentIdNum]);

  useEffect(() => {
    if (!Number.isNaN(workspaceIdNum)) {
      void loadDocuments(workspaceIdNum);
    }
  }, [workspaceIdNum, loadDocuments]);

  useEffect(() => {
    if (Number.isNaN(workspaceIdNum) || Number.isNaN(documentIdNum)) {
      return;
    }
    const currentDoc = documents.find((doc) => doc.id === documentIdNum);
    if (!currentDoc || (currentDoc.status !== "uploading" && currentDoc.status !== "parsing")) {
      return;
    }
    /**
     * 编辑页解析状态轮询：文档处于 uploading/parsing 时每 2 秒刷新列表与详情。
     */
    const timer = window.setInterval(() => {
      void loadDocuments(workspaceIdNum);
      void loadDetail(workspaceIdNum, documentIdNum);
    }, 2000);
    return () => window.clearInterval(timer);
  }, [documents, workspaceIdNum, documentIdNum, loadDocuments, loadDetail]);

  useEffect(() => {
    void loadWorkspaces();
  }, [loadWorkspaces]);

  useEffect(() => {
    if (detail) {
      const safeTitle = detail.title ?? "";
      const safeContent = detail.content ?? "";
      setTitle(safeTitle);
      setContent(safeContent);
      // 服务端数据加载后同步保存快照，避免初始化阶段触发冗余自动保存。
      setLastSavedTitle(safeTitle);
      setLastSavedContent(safeContent);
      lastSavedTitleRef.current = safeTitle;
      lastSavedContentRef.current = safeContent;
      setHydratedFromServer(true);
    }
  }, [detail]);

  useEffect(() => {
    lastSavedTitleRef.current = lastSavedTitle;
  }, [lastSavedTitle]);

  useEffect(() => {
    lastSavedContentRef.current = lastSavedContent;
  }, [lastSavedContent]);

  /**
   * 是否只读。
   */
  const readonly = useMemo(() => detail?.role === "viewer", [detail]);

  /**
   * 使用指定的标题与正文执行保存（手动保存与自动保存共用）。
   *
   * @param nextTitle 要提交的标题
   * @param nextContent 要提交的正文
   * @param successMessage 成功提示文案；自动保存可使用较短提示
   */
  const saveWithValues = useCallback(
    async (nextTitle: string, nextContent: string, successMessage: string): Promise<void> => {
      if (detailRef.current?.role === "viewer") {
        message.warning("viewer 角色为只读，不可保存");
        return;
      }
      setSaving(true);
      try {
        const baseVersion = detailRef.current?.latestVersionNo ?? 0;
        await saveDocument(workspaceIdNum, documentIdNum, nextTitle, nextContent, baseVersion);
        // 保存成功后更新快照，后续只有再次改动才会触发自动保存。
        setLastSavedTitle(nextTitle);
        setLastSavedContent(nextContent);
        lastSavedTitleRef.current = nextTitle;
        lastSavedContentRef.current = nextContent;
        message.success(successMessage);
      } catch (error) {
        const msg = error instanceof Error ? error.message : "保存失败";
        if (msg.includes("版本冲突")) {
          message.warning("保存失败：当前文档已被他人更新，请刷新后重试");
          await loadDetail(workspaceIdNum, documentIdNum);
        } else {
          message.error(msg);
        }
      } finally {
        setSaving(false);
      }
    },
    [documentIdNum, loadDetail, saveDocument, workspaceIdNum]
  );

  /**
   * 手动保存：使用当前编辑区内容。
   */
  const onSave = async (): Promise<void> => {
    await saveWithValues(title, content, "保存成功，已生成新版本");
  };

  /**
   * 侧边栏快捷新建文档。
   * 创建成功后，直接跳转到新文档编辑页，减少往返列表页的操作。
   */
  const onQuickCreate = async (): Promise<void> => {
    const trimmedTitle = quickCreateTitle.trim();
    if (!trimmedTitle) {
      message.warning("请输入文档标题");
      return;
    }
    setQuickCreating(true);
    try {
      const created = await createDocument(workspaceIdNum, trimmedTitle);
      setQuickCreateTitle("");
      setQuickCreateOpen(false);
      message.success("文档创建成功，已切换到新文档");
      navigate(`/workspaces/${workspaceIdNum}/documents/${created.id}`);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "文档创建失败";
      message.error(msg);
    } finally {
      setQuickCreating(false);
    }
  };

  /**
   * 是否存在未保存草稿。
   */
  const hasUnsavedChange = useMemo(() => title !== lastSavedTitle || content !== lastSavedContent, [
    title,
    content,
    lastSavedTitle,
    lastSavedContent
  ]);

  /**
   * 当前知识库展示名称。
   */
  const currentWorkspaceName = useMemo(() => {
    const hit = workspaces.find((workspace) => workspace.id === workspaceIdNum);
    return hit?.name ?? `知识库 ${workspaceIdNum}`;
  }, [workspaces, workspaceIdNum]);

  /**
   * 当前文档在列表中的最新状态（用于展示解析失败信息与重试入口）。
   */
  const currentDocumentItem = useMemo(
    () => documents.find((doc) => doc.id === documentIdNum),
    [documents, documentIdNum]
  );

  /**
   * 切换到指定文档编辑页。
   * 如果当前存在未保存改动，会先二次确认，避免误切换导致草稿丢失。
   *
   * @param targetDocumentId 目标文档 ID
   */
  const switchDocument = (targetDocumentId: number): void => {
    if (targetDocumentId === documentIdNum) {
      return;
    }
    const doNavigate = (): void => {
      navigate(`/workspaces/${workspaceIdNum}/documents/${targetDocumentId}`);
    };
    if (!hasUnsavedChange || saving) {
      doNavigate();
      return;
    }
    Modal.confirm({
      title: "当前文档有未保存改动，是否继续切换？",
      content: "继续切换将丢失当前未保存内容。",
      okText: "继续切换",
      cancelText: "留在当前页",
      onOk: doNavigate
    });
  };

  useEffect(() => {
    if (readonly || !hydratedFromServer || !detail) {
      return;
    }
    // 未发生实际改动时不注册定时器，只有改动后才进入 5 分钟 idle 倒计时。
    const hasMeaningfulChange = title !== lastSavedTitle || content !== lastSavedContent;
    if (!hasMeaningfulChange) {
      return;
    }
    const timer = setTimeout(() => {
      const draftTitle = titleRef.current;
      const draftContent = contentRef.current;
      // 定时器触发时再次对比：若用户已手动保存或已撤回改动，则不再请求。
      if (
        draftTitle === lastSavedTitleRef.current &&
        draftContent === lastSavedContentRef.current
      ) {
        return;
      }
      void saveWithValues(draftTitle, draftContent, "已自动保存");
    }, AUTO_SAVE_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [
    title,
    content,
    readonly,
    hydratedFromServer,
    detail,
    lastSavedTitle,
    lastSavedContent,
    saveWithValues
  ]);

  return (
    <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: 16, alignItems: "start" }}>
      <Card title="同知识库文档" size="small" styles={{ body: { padding: 0 } }}>
        <div style={{ padding: 12, borderBottom: "1px solid #f0f0f0" }}>
          <Space.Compact style={{ width: "100%" }}>
            <Button type="primary" loading={quickCreating} onClick={() => setQuickCreateOpen(true)}>
              新建文档
            </Button>
          </Space.Compact>
        </div>
        <List
          loading={loading}
          dataSource={documents}
          locale={{ emptyText: "暂无文档" }}
          renderItem={(doc) => (
            <List.Item
              style={{
                padding: "8px 12px",
                backgroundColor: doc.id === documentIdNum ? "#e6f4ff" : "transparent"
              }}
              actions={[
                <Button key="edit" type="link" onClick={() => switchDocument(doc.id)}>
                  切换
                </Button>
              ]}
            >
              <List.Item.Meta
                title={
                  <Space size={8}>
                    <Badge dot={doc.id === documentIdNum && hasUnsavedChange} color="#ff4d4f" />
                    <Typography.Text strong={doc.id === documentIdNum} ellipsis={{ tooltip: doc.title }}>
                      {doc.title}
                    </Typography.Text>
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
        {detailLoadError ? (
          <Alert
            type="error"
            showIcon
            message="文档加载失败"
            description={detailLoadError}
            action={
              <Button
                size="small"
                onClick={async () => {
                  try {
                    setDetailLoadError(null);
                    setDetailLoading(true);
                    await loadDetail(workspaceIdNum, documentIdNum);
                  } catch (error) {
                    const msg = error instanceof Error ? error.message : "文档加载失败";
                    setDetailLoadError(msg);
                  } finally {
                    setDetailLoading(false);
                  }
                }}
              >
                重试
              </Button>
            }
          />
        ) : null}
        {currentDocumentItem?.status === "failed" ? (
          <Alert
            type="warning"
            showIcon
            message="文档解析失败"
            description={currentDocumentItem.parseErrorMsg || "未记录失败原因"}
            action={
              <Button
                size="small"
                loading={retryParsing}
                onClick={async () => {
                  try {
                    setRetryParsing(true);
                    await retryParseDocument(workspaceIdNum, documentIdNum);
                    await loadDetail(workspaceIdNum, documentIdNum);
                    message.success("已触发重试解析，正在更新状态");
                  } catch (error) {
                    const msg = error instanceof Error ? error.message : "重试解析失败";
                    message.error(msg);
                  } finally {
                    setRetryParsing(false);
                  }
                }}
              >
                重试解析
              </Button>
            }
          />
        ) : null}
        {chatSnippet ? (
          <Alert
            type={snippetLocateResult?.matched ? "success" : "info"}
            showIcon
            message={snippetLocateResult?.matched ? "已定位到引用片段并高亮" : "收到引用片段，正文中未精确命中"}
            description={
              <Space direction="vertical" size={4} style={{ width: "100%" }}>
                <Typography.Text type="secondary">
                  {snippetLocateResult?.matched
                    ? `匹配次数：${snippetLocateResult.hitCount}（展示第 1 处）`
                    : "可能因为后续编辑改动，片段与当前正文不完全一致"}
                </Typography.Text>
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}>
                  {snippetLocateResult?.matched ? (
                    <>
                      {snippetLocateResult.before}
                      <mark>{snippetLocateResult.hit}</mark>
                      {snippetLocateResult.after}
                    </>
                  ) : (
                    chatSnippet
                  )}
                </Typography.Paragraph>
              </Space>
            }
          />
        ) : null}
        <Card>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Space>
              <Typography.Title level={5} style={{ margin: 0 }}>
                {currentWorkspaceName}
              </Typography.Title>
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
              <Typography.Text type="secondary">文档 #{documentIdNum}</Typography.Text>
            </Space>
            <Space>
              <Button onClick={() => navigate(`/workspaces/${workspaceIdNum}/documents`)}>返回文档列表</Button>
              <Button
                onClick={async () => {
                  await loadVersions(workspaceIdNum, documentIdNum);
                  setVersionDrawerOpen(true);
                }}
              >
                历史版本
              </Button>
              <Button type="primary" onClick={() => void onSave()} loading={saving} disabled={readonly}>
                手动保存
              </Button>
              <Button
                onClick={async () => {
                  try {
                    setExtractingVector(true);
                    await extractVector(workspaceIdNum, documentIdNum);
                    message.success("已完成向量提取（若有旧向量已自动删除并重建）");
                  } catch (error) {
                    const msg = error instanceof Error ? error.message : "提取向量失败";
                    message.error(msg);
                  } finally {
                    setExtractingVector(false);
                  }
                }}
                loading={extractingVector || detail?.indexStatus === "indexing"}
                disabled={readonly || !detail || detail.latestVersionNo <= 0}
              >
                提取到向量知识库
              </Button>
            </Space>
          </Space>
          <Typography.Text type="secondary">
            向量索引状态：{detail?.indexStatus ?? "not_indexed"} / 已索引版本：{detail?.indexedVersionNo ?? 0}
          </Typography.Text>
          {detail?.indexStatus === "failed" && detail?.indexErrorMsg ? (
            <Typography.Text type="danger">索引失败原因：{detail.indexErrorMsg}</Typography.Text>
          ) : null}
        </Card>
        <Card>
          {detailLoading ? (
            <div style={{ display: "flex", justifyContent: "center", padding: "48px 0" }}>
              <Spin tip="正在加载文档..." />
            </div>
          ) : (
            <Form layout="vertical">
              <Form.Item label="标题">
                <Input value={title} onChange={(e) => setTitle(e.target.value)} disabled={readonly || saving} />
              </Form.Item>
              <Form.Item label="Markdown 内容">
                <VditorEditor value={content} disabled={readonly || saving} onChange={setContent} />
              </Form.Item>
              <Typography.Text type="secondary">
                当前角色：{detail?.role ?? "-"}，当前版本：{detail?.latestVersionNo ?? "-"}
              </Typography.Text>
            </Form>
          )}
        </Card>
      </Space>

      <Drawer title="历史版本" open={versionDrawerOpen} onClose={() => setVersionDrawerOpen(false)} width={640}>
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
                <Button
                  onClick={async () => {
                    await loadVersionDetail(workspaceIdNum, documentIdNum, row.versionNo);
                    setVersionPreviewOpen(true);
                  }}
                >
                  查看内容
                </Button>
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
        <Space>
          <Button
            type="primary"
            onClick={() => {
              if (!versionDetail) {
                return;
              }
              // 一键恢复到编辑区，仅更新本地草稿，不立即触发后端回滚。
              setTitle(versionDetail.titleSnapshot || title);
              setContent(versionDetail.content);
              setVersionPreviewOpen(false);
              message.success("已恢复到编辑区草稿，请确认后保存");
            }}
            disabled={readonly}
          >
            恢复到编辑区
          </Button>
        </Space>
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

