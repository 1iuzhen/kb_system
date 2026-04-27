import { Button, Form, InputNumber, Modal, Select, Space, Table, message } from "antd";
import { useEffect, useState } from "react";
import { listMembersApi, removeMemberApi, upsertMemberApi } from "../api/workspaceApi";
import type { WorkspaceMember } from "../types";

/**
 * 成员管理弹窗属性。
 */
interface MemberModalProps {
  workspaceId: number;
  open: boolean;
  onClose: () => void;
}

/**
 * 成员管理弹窗。
 */
export function MemberModal(props: MemberModalProps): JSX.Element {
  const { workspaceId, open, onClose } = props;
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [loading, setLoading] = useState(false);

  /**
   * 拉取成员数据。
   */
  const loadMembers = async (): Promise<void> => {
    setLoading(true);
    try {
      const data = await listMembersApi(workspaceId);
      setMembers(data);
    } catch (error) {
      const msg = error instanceof Error ? error.message : "加载成员失败";
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      void loadMembers();
    }
  }, [open, workspaceId]);

  /**
   * 提交成员变更。
   *
   * @param values 表单值
   */
  const onFinish = async (values: { userId: number; role: WorkspaceMember["role"] }): Promise<void> => {
    try {
      await upsertMemberApi(workspaceId, values.userId, values.role);
      message.success("保存成员成功");
      await loadMembers();
    } catch (error) {
      const msg = error instanceof Error ? error.message : "保存成员失败";
      message.error(msg);
    }
  };

  /**
   * 删除成员。
   *
   * @param userId 成员用户 ID
   */
  const onRemove = async (userId: number): Promise<void> => {
    try {
      await removeMemberApi(workspaceId, userId);
      message.success("移除成员成功");
      await loadMembers();
    } catch (error) {
      const msg = error instanceof Error ? error.message : "移除成员失败";
      message.error(msg);
    }
  };

  return (
    <Modal open={open} onCancel={onClose} footer={null} title="成员管理" width={720}>
      <Form layout="inline" onFinish={onFinish} style={{ marginBottom: 16 }}>
        <Form.Item name="userId" rules={[{ required: true, message: "请输入用户ID" }]}>
          <InputNumber placeholder="用户ID（如 2 / 3）" />
        </Form.Item>
        <Form.Item name="role" rules={[{ required: true, message: "请选择角色" }]}>
          <Select
            style={{ width: 140 }}
            options={[
              { label: "owner", value: "owner" },
              { label: "editor", value: "editor" },
              { label: "viewer", value: "viewer" }
            ]}
          />
        </Form.Item>
        <Button htmlType="submit" type="primary">
          保存成员
        </Button>
      </Form>
      <Table
        loading={loading}
        rowKey="userId"
        dataSource={members}
        pagination={false}
        columns={[
          { title: "用户ID", dataIndex: "userId" },
          { title: "用户名", dataIndex: "username" },
          { title: "角色", dataIndex: "role" },
          {
            title: "操作",
            render: (_, row: WorkspaceMember) => (
              <Space>
                <Button danger onClick={() => void onRemove(row.userId)}>
                  删除
                </Button>
              </Space>
            )
          }
        ]}
      />
    </Modal>
  );
}

