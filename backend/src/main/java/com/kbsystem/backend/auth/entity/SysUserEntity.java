package com.kbsystem.backend.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体。
 * 该实体用于承载用户基础信息，模拟数据库中的 sys_user 表结构。
 */
@Data
@TableName("sys_user")
public class SysUserEntity {

    /**
     * 用户主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码（示例阶段使用明文，生产环境必须使用强哈希）。
     */
    private String password;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志。
     */
    @TableLogic
    private Integer deleted;

    /**
     * 乐观锁版本号。
     */
    @Version
    private Long updateVersion;
}

