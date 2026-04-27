import { Button, Card, Form, Input, message, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { loginApi } from "../api/authApi";
import { useAuthStore } from "../store/useAuthStore";

/**
 * 登录页面。
 */
export function LoginPage(): JSX.Element {
  const navigate = useNavigate();
  const setLogin = useAuthStore((s) => s.setLogin);

  /**
   * 提交登录表单。
   *
   * @param values 表单值
   */
  const onFinish = async (values: { username: string; password: string }): Promise<void> => {
    try {
      const data = await loginApi(values);
      setLogin(data.accessToken, data.refreshToken, data.userInfo);
      message.success("登录成功");
      navigate("/workspaces");
    } catch (error) {
      const msg = error instanceof Error ? error.message : "登录失败";
      message.error(msg);
    }
  };

  return (
    <Card style={{ width: 420, margin: "80px auto" }}>
      <Typography.Title level={4}>登录</Typography.Title>
      <Form layout="vertical" onFinish={onFinish} initialValues={{ username: "admin", password: "123456" }}>
        <Form.Item label="用户名" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
          <Input />
        </Form.Item>
        <Form.Item label="密码" name="password" rules={[{ required: true, message: "请输入密码" }]}>
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>
          登录
        </Button>
      </Form>
    </Card>
  );
}

