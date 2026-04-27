/**
 * 知识库信息。
 */
export interface Workspace {
  id: number;
  name: string;
  role: "owner" | "editor" | "viewer";
}

/**
 * 成员信息。
 */
export interface WorkspaceMember {
  userId: number;
  username: string;
  role: "owner" | "editor" | "viewer";
}

