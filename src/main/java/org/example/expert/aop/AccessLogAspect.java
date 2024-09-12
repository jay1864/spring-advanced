package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.time.LocalDateTime;

@Slf4j
@Aspect
public class AccessLogAspect {
    private final HttpServletRequest servletRequest;

    public AccessLogAspect(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    @Pointcut("execution(* org.example.expert.domain.comment.controller.*(..))")
    private void commentAdmin(){}

    @Pointcut("execution(* org.example.expert.domain.user.controller.*(..))")
    private void userAdmin(){}

    @Before("commentAdmin() || userAdmin()")
    public void recordAccessLog() throws Throwable {
        Long userId = (Long) servletRequest.getAttribute("userId"); // 요청한 사용자의 ID
        LocalDateTime accessTime = LocalDateTime.now();   // API 요청 시각
        String url = servletRequest.getRequestURI();    // API 요청 URL

        log.info("\n::: User ID : {} :::\n::: Access Time : {}:::\n::: URL : {} :::", userId, accessTime, url);
    }

}
