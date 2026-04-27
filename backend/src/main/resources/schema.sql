CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';

CREATE TABLE IF NOT EXISTS kb_workspace (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE kb_workspace IS '知识库主表';
COMMENT ON COLUMN kb_workspace.owner_user_id IS '创建者用户ID';

CREATE INDEX IF NOT EXISTS idx_kb_workspace_owner_user_id ON kb_workspace(owner_user_id);

CREATE TABLE IF NOT EXISTS kb_workspace_member (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_workspace_user UNIQUE (workspace_id, user_id)
);

COMMENT ON TABLE kb_workspace_member IS '知识库成员表';
COMMENT ON COLUMN kb_workspace_member.role IS '角色 owner/editor/viewer';

CREATE INDEX IF NOT EXISTS idx_kb_workspace_member_workspace_id ON kb_workspace_member(workspace_id);
CREATE INDEX IF NOT EXISTS idx_kb_workspace_member_user_id ON kb_workspace_member(user_id);

CREATE TABLE IF NOT EXISTS kb_document (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    index_status VARCHAR(32) NOT NULL DEFAULT 'not_indexed',
    indexed_version_no INT NOT NULL DEFAULT 0,
    index_error_msg TEXT,
    parse_error_msg TEXT,
    source_file_name VARCHAR(255),
    source_file_path VARCHAR(512),
    latest_version_no INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE kb_document IS '文档主表';
COMMENT ON COLUMN kb_document.workspace_id IS '所属知识库ID';
COMMENT ON COLUMN kb_document.status IS '文档状态';
COMMENT ON COLUMN kb_document.latest_version_no IS '最新版本号';

ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS index_status VARCHAR(32) NOT NULL DEFAULT 'not_indexed';
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS indexed_version_no INT NOT NULL DEFAULT 0;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS index_error_msg TEXT;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS parse_error_msg TEXT;
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS source_file_name VARCHAR(255);
ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS source_file_path VARCHAR(512);
COMMENT ON COLUMN kb_document.index_status IS '向量索引状态';
COMMENT ON COLUMN kb_document.indexed_version_no IS '已索引版本号';
COMMENT ON COLUMN kb_document.index_error_msg IS '向量索引错误信息';
COMMENT ON COLUMN kb_document.parse_error_msg IS '解析失败错误信息';
COMMENT ON COLUMN kb_document.source_file_name IS '上传源文件名';
COMMENT ON COLUMN kb_document.source_file_path IS '上传源文件存储路径';

CREATE INDEX IF NOT EXISTS idx_kb_document_workspace_id ON kb_document(workspace_id);

CREATE TABLE IF NOT EXISTS kb_document_version (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    save_user_id BIGINT,
    save_username VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_version UNIQUE (document_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_kb_document_version_document_id ON kb_document_version(document_id);

ALTER TABLE kb_document_version ADD COLUMN IF NOT EXISTS title_snapshot VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE kb_document_version ADD COLUMN IF NOT EXISTS save_user_id BIGINT;
ALTER TABLE kb_document_version ADD COLUMN IF NOT EXISTS save_username VARCHAR(64);

COMMENT ON TABLE kb_document_version IS '文档版本表';
COMMENT ON COLUMN kb_document_version.document_id IS '文档ID';
COMMENT ON COLUMN kb_document_version.version_no IS '版本号';
COMMENT ON COLUMN kb_document_version.title_snapshot IS '该版本标题快照';
COMMENT ON COLUMN kb_document_version.content IS 'Markdown内容';
COMMENT ON COLUMN kb_document_version.save_user_id IS '保存人ID';
COMMENT ON COLUMN kb_document_version.save_username IS '保存人用户名';

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kb_chunk (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    chunk_index INT NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE kb_chunk IS '文档分块向量表';
COMMENT ON COLUMN kb_chunk.workspace_id IS '所属知识库ID';
COMMENT ON COLUMN kb_chunk.document_id IS '文档ID';
COMMENT ON COLUMN kb_chunk.version_no IS '文档版本号';
COMMENT ON COLUMN kb_chunk.chunk_index IS '分块序号';
COMMENT ON COLUMN kb_chunk.title_snapshot IS '文档标题快照';
COMMENT ON COLUMN kb_chunk.content IS '分块文本';
COMMENT ON COLUMN kb_chunk.embedding IS '向量';

CREATE INDEX IF NOT EXISTS idx_kb_chunk_workspace_id ON kb_chunk(workspace_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_document_id ON kb_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_document_version ON kb_chunk(document_id, version_no);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_embedding ON kb_chunk USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT,
    action VARCHAR(64) NOT NULL,
    operation_detail TEXT,
    ip VARCHAR(64),
    user_agent VARCHAR(255),
    request_method VARCHAR(16),
    request_uri VARCHAR(255),
    request_params TEXT,
    status VARCHAR(16) NOT NULL,
    error_msg TEXT,
    execution_time_ms BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    update_version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_operation_log IS '操作审计日志表';
COMMENT ON COLUMN sys_operation_log.user_id IS '操作用户ID';
COMMENT ON COLUMN sys_operation_log.username IS '操作用户名';
COMMENT ON COLUMN sys_operation_log.target_type IS '操作目标类型';
COMMENT ON COLUMN sys_operation_log.target_id IS '操作目标ID';
COMMENT ON COLUMN sys_operation_log.action IS '操作动作';
COMMENT ON COLUMN sys_operation_log.operation_detail IS '操作详情JSON';
COMMENT ON COLUMN sys_operation_log.ip IS '请求IP';
COMMENT ON COLUMN sys_operation_log.user_agent IS '用户代理';
COMMENT ON COLUMN sys_operation_log.request_method IS '请求方法';
COMMENT ON COLUMN sys_operation_log.request_uri IS '请求URI';
COMMENT ON COLUMN sys_operation_log.request_params IS '请求参数JSON';
COMMENT ON COLUMN sys_operation_log.status IS '操作结果状态 success/failed';
COMMENT ON COLUMN sys_operation_log.error_msg IS '错误信息';
COMMENT ON COLUMN sys_operation_log.execution_time_ms IS '执行耗时毫秒';

CREATE INDEX IF NOT EXISTS idx_sys_operation_log_user_id ON sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_operation_log_target_type_target_id ON sys_operation_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_sys_operation_log_create_time ON sys_operation_log(create_time);
