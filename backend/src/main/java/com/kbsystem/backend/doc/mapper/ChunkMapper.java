package com.kbsystem.backend.doc.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 向量分块 Mapper。
 */
@Mapper
public interface ChunkMapper {

    /**
     * 删除文档历史向量分块。
     *
     * @param documentId 文档 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 新增向量分块。
     *
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @param versionNo 版本号
     * @param chunkIndex 分块序号
     * @param titleSnapshot 标题快照
     * @param content 分块文本
     * @param embedding 向量字符串（pgvector 格式）
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO kb_chunk(workspace_id, document_id, version_no, chunk_index, title_snapshot, content, embedding)
            VALUES(#{workspaceId}, #{documentId}, #{versionNo}, #{chunkIndex}, #{titleSnapshot}, #{content}, CAST(#{embedding} AS vector))
            """)
    int insertChunk(@Param("workspaceId") Long workspaceId,
                    @Param("documentId") Long documentId,
                    @Param("versionNo") Integer versionNo,
                    @Param("chunkIndex") Integer chunkIndex,
                    @Param("titleSnapshot") String titleSnapshot,
                    @Param("content") String content,
                    @Param("embedding") String embedding);

    /**
     * 按向量相似度检索分块。
     *
     * @param workspaceId 知识库 ID
     * @param embedding 查询向量
     * @param limit 限制条数
     * @return 相似分块
     */
    @Select("""
            SELECT
                c.id AS chunkId,
                c.document_id AS documentId,
                c.title_snapshot AS docTitle,
                c.content AS content,
                1 - (c.embedding <=> CAST(#{embedding} AS vector)) AS score
            FROM kb_chunk c
            WHERE c.workspace_id = #{workspaceId}
            ORDER BY c.embedding <=> CAST(#{embedding} AS vector)
            LIMIT #{limit}
            """)
    List<ChunkSearchRow> searchTopKByWorkspace(@Param("workspaceId") Long workspaceId,
                                               @Param("embedding") String embedding,
                                               @Param("limit") Integer limit);

    /**
     * 向量检索结果行。
     */
    class ChunkSearchRow {

        /**
         * 分块 ID。
         */
        private Long chunkId;

        /**
         * 文档 ID。
         */
        private Long documentId;

        /**
         * 文档标题快照。
         */
        private String docTitle;

        /**
         * 分块正文。
         */
        private String content;

        /**
         * 相似度分数。
         */
        private Double score;

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getDocTitle() {
            return docTitle;
        }

        public void setDocTitle(String docTitle) {
            this.docTitle = docTitle;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }
}

