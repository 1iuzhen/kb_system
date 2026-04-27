package com.kbsystem.backend.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应对象。
 *
 * @param <T> 数据体类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "业务状态码，0 表示成功")
    private int code;

    @Schema(description = "业务提示信息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 构建成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 构建失败响应。
     *
     * @param code 业务错误码
     * @param msg  错误信息
     * @param <T>  数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}

