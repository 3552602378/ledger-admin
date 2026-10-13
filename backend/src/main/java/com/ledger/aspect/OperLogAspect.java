package com.ledger.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.annotation.OperLog;
import com.ledger.entity.SysOperLog;
import com.ledger.mapper.SysOperLogMapper;
import com.ledger.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 操作日志切面：拦截标注 @OperLog 的方法，成功与失败的操作均记录入库 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    /** 参数与错误信息最大记录长度 */
    private static final int MAX_LENGTH = 512;

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 异常继续向上抛给全局异常处理器，只负责记录
            error = ex;
            throw ex;
        } finally {
            try {
                save(joinPoint, operLog, System.currentTimeMillis() - start, error);
            } catch (Exception ignore) {
                // 日志写入失败不能影响业务
            }
        }
    }

    private void save(ProceedingJoinPoint joinPoint, OperLog operLog, long cost, Throwable error) {
        SysOperLog log = new SysOperLog();
        log.setTitle(operLog.title());
        log.setBusinessType(operLog.businessType());
        log.setUsername(resolveUsername(joinPoint));
        log.setCostTime(cost);
        log.setStatus(error == null ? 1 : 0);
        if (error != null) {
            log.setResponseResult(truncate(error.getMessage(), MAX_LENGTH));
        }

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(truncate(request.getRequestURI(), MAX_LENGTH));
            log.setIp(resolveIp(request));
        }

        log.setRequestParams(truncate(toJson(joinPoint.getArgs()), MAX_LENGTH));
        log.setCreateTime(LocalDateTime.now());
        operLogMapper.insert(log);
    }

    /** 登录等无认证上下文的场景，从入参反射取 username */
    private String resolveUsername(ProceedingJoinPoint joinPoint) {
        String username = SecurityUtils.getUsername();
        if (username != null) {
            return username;
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null) {
                continue;
            }
            try {
                Object value = arg.getClass().getMethod("getUsername").invoke(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (Exception ignore) {
                // 入参没有 getUsername 方法，继续找下一个
            }
        }
        return null;
    }

    /** 逐个序列化入参，可序列化对象转 JSON 并对密码脱敏 */
    private String toJson(Object[] args) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(mask(args[i]));
        }
        return sb.append("]").toString();
    }

    private String mask(Object arg) {
        if (arg == null) {
            return "null";
        }
        String className = arg.getClass().getName();
        // 请求/响应对象等无业务含义参数只记录类型名
        if (className.startsWith("jakarta.servlet") || className.startsWith("org.springframework")) {
            return "<" + className.substring(className.lastIndexOf('.') + 1) + ">";
        }
        try {
            return objectMapper.writeValueAsString(arg)
                    .replaceAll("(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        } catch (Exception e) {
            return "<unserializable>";
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return truncate(ip.split(",")[0].trim(), 50);
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
