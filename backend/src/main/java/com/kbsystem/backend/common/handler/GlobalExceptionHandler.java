package com.kbsystem.backend.common.handler;

import com.kbsystem.backend.common.Result;
import com.kbsystem.backend.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 统一将异常转为 Result，并确保异常被记录到日志中。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.error("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage(), ex);
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）。
     *
     * @param ex 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception ex) {
        log.error("参数校验异常", ex);
        return Result.fail(40001, "请求参数不合法");
    }

    /**
     * 处理兜底异常，避免异常信息泄露。
     *
     * @param ex 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(50000, "系统繁忙，请稍后重试");
    }
}

