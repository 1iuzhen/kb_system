import { Input, Spin, Typography } from "antd";
import { useEffect, useRef, useState } from "react";
import "vditor/dist/index.css";

/**
 * 编辑器实例最小能力约束。
 * 这里只声明当前业务会用到的方法，避免使用 any。
 */
interface VditorInstance {
  getValue: () => string;
  setValue: (value: string) => void;
  disabled: () => void;
  enable: () => void;
  destroy: () => void;
}

/**
 * Vditor 编辑器属性。
 */
interface VditorEditorProps {
  value: string;
  disabled: boolean;
  onChange: (value: string) => void;
}

/**
 * Vditor 编辑器组件。
 */
export function VditorEditor(props: VditorEditorProps): JSX.Element {
  const { value, disabled, onChange } = props;
  const containerRef = useRef<HTMLDivElement | null>(null);
  const editorRef = useRef<VditorInstance | null>(null);
  const [ready, setReady] = useState(false);
  const [initError, setInitError] = useState<string | null>(null);
  /**
   * 统一做字符串兜底，避免后端返回 null 时触发编辑器内部异常。
   */
  const safeValue = value ?? "";

  useEffect(() => {
    let mounted = true;
    /**
     * 延迟动态加载 Vditor，避免测试和 SSR 场景直接引用 window。
     */
    const init = async (): Promise<void> => {
      if (!containerRef.current) {
        return;
      }
      try {
        const mod = await import("vditor");
        const Vditor = mod.default;
        if (!mounted) {
          return;
        }
        editorRef.current = new Vditor(containerRef.current, {
          mode: "wysiwyg",
          value: safeValue,
          height: 520,
          cache: {
            enable: false
          },
          input: (markdown: string) => {
            onChange(markdown);
          }
        });
        setReady(true);
      } catch {
        // Vditor 初始化失败时降级为文本框，避免整页白屏。
        setInitError("Vditor 初始化失败，已降级为纯文本编辑");
      }
    };
    void init();
    return () => {
      mounted = false;
      if (editorRef.current) {
        editorRef.current.destroy();
        editorRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!editorRef.current) {
      return;
    }
    const current = editorRef.current.getValue();
    if (current !== safeValue) {
      editorRef.current.setValue(safeValue);
    }
  }, [safeValue]);

  useEffect(() => {
    if (!editorRef.current) {
      return;
    }
    // Vditor 通过 disabled()/enable() 控制编辑权限。
    if (disabled) {
      editorRef.current.disabled();
    } else {
      editorRef.current.enable();
    }
  }, [disabled]);

  if (initError) {
    return (
      <div>
        <Typography.Text type="warning">{initError}</Typography.Text>
        <Input.TextArea
          value={safeValue}
          disabled={disabled}
          rows={18}
          style={{ marginTop: 8 }}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
    );
  }

  return (
    <div>
      {!ready ? <Spin /> : null}
      <div ref={containerRef} />
    </div>
  );
}

