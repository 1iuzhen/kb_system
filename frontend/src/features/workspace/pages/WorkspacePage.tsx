import { Button, Card, Dropdown, Form, Input, List, Modal, Select, Space, Tag, Typography, message } from "antd";
import type { MenuProps } from "antd";
import { MoreOutlined, PlusOutlined } from "@ant-design/icons";
import { useEffect, useMemo, useState } from "react";
import { useWorkspaceStore } from "../store/useWorkspaceStore";
import type { Workspace } from "../types";
import { MemberModal } from "../components/MemberModal";
import { useNavigate } from "react-router-dom";
import { useDocStore } from "../../doc/store/useDocStore";
import { listDocumentsApi } from "../../doc/api/docApi";
import type { DocumentItem } from "../../doc/types";

/**
 * 知识库页面。
 */
export function WorkspacePage(): JSX.Element {
  const navigate = useNavigate();
  const { workspaces, loading, loadWorkspaces, createWorkspace, updateWorkspace, deleteWorkspace } = useWorkspaceStore();
  const { createDocument } = useDocStore();
  const [memberWorkspaceId, setMemberWorkspaceId] = useState<number | null>(null);
  const [activeWorkspaceId, setActiveWorkspaceId] = useState<number | null>(null);
  const [workspaceDocPreviewMap, setWorkspaceDocPreviewMap] = useState<Record<number, DocumentItem[]>>({});
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createType, setCreateType] = useState<"workspace" | "document">("workspace");
  const [submitting, setSubmitting] = useState(false);
  const [createForm] = Form.useForm<{ workspaceName?: string; documentTitle?: string; workspaceId?: number }>();

  useEffect(() => {
    void loadWorkspaces();
  }, [loadWorkspaces]);

  useEffect(() => {
    if (workspaces.length === 0) {
      setActiveWorkspaceId(null);
      return;
    }
    if (activeWorkspaceId === null || !workspaces.some((workspace) => workspace.id === activeWorkspaceId)) {
      setActiveWorkspaceId(workspaces[0].id);
    }
  }, [workspaces, activeWorkspaceId]);

  useEffect(() => {
    if (workspaces.length === 0) {
      setWorkspaceDocPreviewMap({});
      return;
    }
    let cancelled = false;
    /**
     * 预加载每个知识库文档列表，只截取前三篇用于卡片预览。
     * 失败时按空列表处理，避免某个知识库接口异常影响整页渲染。
     */
    const loadWorkspaceDocPreviews = async (): Promise<void> => {
      const pairs = await Promise.all(
        workspaces.map(async (workspace) => {
          try {
            const documents = await listDocumentsApi(workspace.id);
            return [workspace.id, documents.slice(0, 3)] as const;
          } catch {
            return [workspace.id, []] as const;
          }
        })
      );
      if (cancelled) {
        return;
      }
      setWorkspaceDocPreviewMap(Object.fromEntries(pairs));
    };
    void loadWorkspaceDocPreviews();
    return () => {
      cancelled = true;
    };
  }, [workspaces]);

  /**
   * 根据当前创建类型打开创建弹窗。
   *
   * @param type 创建类型（知识库或文档）
   */
  const openCreateModal = (type: "workspace" | "document"): void => {
    setCreateType(type);
    setCreateModalOpen(true);
    createForm.resetFields();
    // 创建文档时默认选中当前激活知识库，减少一次选择动作。
    if (type === "document" && activeWorkspaceId !== null) {
      createForm.setFieldValue("workspaceId", activeWorkspaceId);
    }
  };

  /**
   * 编辑知识库名称。
   *
   * @param row 当前行
   */
  const onEdit = (row: Workspace): void => {
    Modal.confirm({
      title: "重命名知识库",
      content: (
        <Input
          id={`rename-${row.id}`}
          defaultValue={row.name}
          placeholder="请输入新名称"
        />
      ),
      onOk: async () => {
        const input = document.getElementById(`rename-${row.id}`) as HTMLInputElement | null;
        const value = input?.value?.trim() ?? "";
        if (!value) {
          message.error("名称不能为空");
          return;
        }
        try {
          await updateWorkspace(row.id, value);
          message.success("更新成功");
        } catch (error) {
          const msg = error instanceof Error ? error.message : "更新失败";
          message.error(msg);
        }
      }
    });
  };

  /**
   * 删除知识库。
   *
   * @param row 当前行
   */
  const onDelete = (row: Workspace): void => {
    Modal.confirm({
      title: "确认删除该知识库？",
      onOk: async () => {
        try {
          await deleteWorkspace(row.id);
          message.success("删除成功");
        } catch (error) {
          const msg = error instanceof Error ? error.message : "删除失败";
          message.error(msg);
        }
      }
    });
  };

  /**
   * 提交创建操作。
   * - 创建知识库：仅需名称
   * - 创建文档：必须选择已有知识库 + 文档标题
   */
  const onSubmitCreate = async (): Promise<void> => {
    try {
      const values = await createForm.validateFields();
      setSubmitting(true);
      if (createType === "workspace") {
        const workspaceName = (values.workspaceName ?? "").trim();
        await createWorkspace(workspaceName);
        message.success("知识库创建成功");
        setCreateModalOpen(false);
        return;
      }
      const targetWorkspaceId = values.workspaceId;
      const documentTitle = (values.documentTitle ?? "").trim();
      if (!targetWorkspaceId) {
        message.error("请选择知识库");
        return;
      }
      await createDocument(targetWorkspaceId, documentTitle);
      message.success("文档创建成功");
      setCreateModalOpen(false);
      // 按产品交互要求：在当前页直接完成新建，不自动跳转到文档编辑页。
      setActiveWorkspaceId(targetWorkspaceId);
      // 刷新当前知识库卡片中的文档预览。
      const refreshedDocs = await listDocumentsApi(targetWorkspaceId);
      setWorkspaceDocPreviewMap((prev) => ({
        ...prev,
        [targetWorkspaceId]: refreshedDocs.slice(0, 3)
      }));
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || "创建失败");
      }
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * 顶部创建下拉菜单。
   */
  const createMenuItems = useMemo<MenuProps["items"]>(
    () => [
      { key: "workspace", label: "创建知识库" },
      { key: "document", label: "创建文档" }
    ],
    []
  );

  /**
   * 处理创建菜单点击。
   *
   * @param info 菜单点击信息
   */
  const onCreateMenuClick: MenuProps["onClick"] = (info) => {
    if (info.key === "workspace" || info.key === "document") {
      openCreateModal(info.key);
    }
  };

  /**
   * 进入知识库文档页（左侧目录与右侧卡片共用）。
   *
   * @param workspaceId 知识库 ID
   */
  const enterWorkspace = (workspaceId: number): void => {
    setActiveWorkspaceId(workspaceId);
    navigate(`/workspaces/${workspaceId}/documents`);
  };

  /**
   * 卡片右上角三点菜单点击处理。
   *
   * @param workspace 当前知识库
   * @param info 菜单点击信息
   */
  const onWorkspaceActionMenuClick = (workspace: Workspace, info: { key: string }): void => {
    if (info.key === "createDocument") {
      openCreateDocumentForWorkspace(workspace.id);
      return;
    }
    if (info.key === "rename") {
      onEdit(workspace);
      return;
    }
    if (info.key === "member") {
      setMemberWorkspaceId(workspace.id);
      return;
    }
    if (info.key === "delete") {
      onDelete(workspace);
    }
  };

  /**
   * 在指定知识库下直接创建文档。
   * 入口位于知识库卡片右上角，减少用户操作路径。
   *
   * @param workspaceId 知识库 ID
   */
  const openCreateDocumentForWorkspace = (workspaceId: number): void => {
    setCreateType("document");
    setCreateModalOpen(true);
    createForm.resetFields();
    // 从卡片入口进入时，预填当前知识库，用户只需输入文档标题。
    createForm.setFieldValue("workspaceId", workspaceId);
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 16 }}>
      <Card title="知识库目录" loading={loading} styles={{ body: { padding: 8 } }}>
        <List
          dataSource={workspaces}
          locale={{ emptyText: "暂无知识库，请先创建" }}
          renderItem={(workspace) => (
            <List.Item style={{ padding: "4px 6px" }}>
              <Button
                type={workspace.id === activeWorkspaceId ? "primary" : "text"}
                style={{ width: "100%", textAlign: "left" }}
                onClick={() => enterWorkspace(workspace.id)}
              >
                {workspace.name}
              </Button>
            </List.Item>
          )}
        />
      </Card>

      <Space direction="vertical" style={{ width: "100%" }} size={16}>
        <Card>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Typography.Title level={4} style={{ margin: 0 }}>
              我的知识库
            </Typography.Title>
            <Dropdown menu={{ items: createMenuItems, onClick: onCreateMenuClick }} trigger={["click"]}>
              <Button type="primary" icon={<PlusOutlined />}>
                创建
              </Button>
            </Dropdown>
          </Space>
        </Card>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
            gap: 16
          }}
        >
          {workspaces.map((workspace) => (
            <Card
              key={workspace.id}
              title={workspace.name}
              style={{
                borderColor: workspace.id === activeWorkspaceId ? "#1677ff" : undefined,
                cursor: "pointer"
              }}
              onClick={() => enterWorkspace(workspace.id)}
              extra={
                <Space size={8}>
                  <Tag color={workspace.role === "owner" ? "blue" : workspace.role === "editor" ? "gold" : "default"}>
                    {workspace.role}
                  </Tag>
                  <Dropdown
                    trigger={["click"]}
                    menu={{
                      items: [
                        { key: "createDocument", label: "创建文档", disabled: workspace.role === "viewer" },
                        { key: "rename", label: "重命名", disabled: workspace.role === "viewer" },
                        { key: "member", label: "成员管理" },
                        { key: "delete", label: "删除", danger: true, disabled: workspace.role !== "owner" }
                      ],
                      onClick: (info) => {
                        onWorkspaceActionMenuClick(workspace, info);
                      }
                    }}
                  >
                    <Button
                      size="small"
                      type="text"
                      icon={<MoreOutlined />}
                      onClick={(event) => {
                        event.stopPropagation();
                      }}
                    />
                  </Dropdown>
                </Space>
              }
            >
              <List
                dataSource={workspaceDocPreviewMap[workspace.id] ?? []}
                locale={{ emptyText: "暂无文档" }}
                renderItem={(doc) => (
                  <List.Item style={{ paddingInline: 0 }}>
                    <Button
                      type="link"
                      style={{ paddingInline: 0 }}
                      onClick={(event) => {
                        event.stopPropagation();
                        navigate(`/workspaces/${workspace.id}/documents/${doc.id}`);
                      }}
                    >
                      {doc.title}
                    </Button>
                  </List.Item>
                )}
              />
            </Card>
          ))}
        </div>
      </Space>

      {memberWorkspaceId ? (
        <MemberModal workspaceId={memberWorkspaceId} open={memberWorkspaceId !== null} onClose={() => setMemberWorkspaceId(null)} />
      ) : null}

      <Modal
        title={createType === "workspace" ? "创建知识库" : "创建文档"}
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => void onSubmitCreate()}
        okText="确认创建"
        cancelText="取消"
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={createForm} layout="vertical">
          {createType === "workspace" ? (
            <Form.Item
              label="知识库名称"
              name="workspaceName"
              rules={[{ required: true, message: "请输入知识库名称" }]}
            >
              <Input placeholder="例如：后端架构设计" maxLength={64} />
            </Form.Item>
          ) : (
            <>
              <Form.Item
                label="选择知识库"
                name="workspaceId"
                rules={[{ required: true, message: "请选择一个已有知识库" }]}
              >
                <Select
                  placeholder="请选择知识库"
                  options={workspaces.map((workspace) => ({
                    value: workspace.id,
                    label: workspace.name
                  }))}
                />
              </Form.Item>
              <Form.Item
                label="文档标题"
                name="documentTitle"
                rules={[{ required: true, message: "请输入文档标题" }]}
              >
                <Input placeholder="例如：接口设计说明" maxLength={128} />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
}

