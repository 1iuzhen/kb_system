package com.kbsystem.backend.common.exception;

/**
 * 业务异常。
 * 用于承载业务层可预期错误，并由全局异常处理器统一转换为 Result。
 */
public class BizException extends RuntimeException {

    private final int code;

    /**
     * 构造业务异常。
     *
     * @param code 业务错误码
     * @param msg  错误信息
     */
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /**
     * 获取业务错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}

