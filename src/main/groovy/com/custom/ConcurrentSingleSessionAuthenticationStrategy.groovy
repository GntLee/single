package com.custom

import org.springframework.security.core.Authentication
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.util.Assert

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * 会话管理类
 * @auther Lee
 * @Date 2017/11/14 18:21
 * return
 */
class ConcurrentSingleSessionAuthenticationStrategy implements SessionAuthenticationStrategy {

    private SessionRegistry sessionRegistry

    /**
     * @param 将新的会话赋值给sessionRegistry
     */
    public ConcurrentSingleSessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        Assert.notNull(sessionRegistry, "SessionRegistry cannot be null")
        this.sessionRegistry = sessionRegistry
    }
    /**
     * 覆盖父类的onAuthentication方法
     * 用新的session替换就的session
     */
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {

        def sessions = sessionRegistry.getAllSessions(authentication.getPrincipal(), false)
        def principals = sessionRegistry.getAllPrincipals()
        sessions.each {
            if (it.principal == authentication.getPrincipal()) {
                it.expireNow()
            }
        }


    }
}
