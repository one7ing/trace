package com.trace.exception;

import com.trace.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 404 资源未找到的异常（如 favicon.ico 缺失）。
     * 这类异常通常不重要，不需要记录错误日志，直接返回 404 即可。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        // favicon.ico 等缺失的静态资源不记录错误日志
        return ResponseEntity.notFound().build();
    }

    /**
     * 处理请求参数校验失败异常（如 @Valid 校验不通过）。
     * 提取具体的字段错误信息，以友好的方式返回给调用方。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // 收集所有字段的校验错误消息
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, errors));
    }

    /**
     * 处理登录认证失败异常（用户名或密码错误）。
     * 返回 401 Unauthorized 状态码，并提示友好信息。
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
    }

    /**
     * 处理业务逻辑中抛出的非法参数异常。
     * 通常由 Service 层在参数校验不通过时主动抛出。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    /**
     * 处理 SSE（Server-Sent Events）异步请求超时异常。
     * 这种情况在流式对话中属于正常行为（客户端断开或连接超时），
     * 不应作为错误处理，直接返回空响应即可，避免记录大量无关的 ERROR 日志。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.debug("SSE async timeout: {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }

    /**
     * 处理所有未预料到的运行时异常。
     * 记录详细错误日志供开发排查，但对客户端只返回通用的 500 错误。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("运行时异常: GlobalExceptionHandler.handleRuntime", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }

    /**
     * 兜底处理，捕获所有其他异常（如 Checked Exception）。
     * 同样只记录日志，返回通用 500 错误，防止敏感信息泄漏。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("未知异常: GlobalExceptionHandler.handleGeneric", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "未知错误"));
    }
}
