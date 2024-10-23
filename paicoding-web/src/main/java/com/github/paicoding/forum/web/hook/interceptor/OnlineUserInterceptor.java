package com.github.paicoding.forum.web.hook.interceptor;

import com.github.paicoding.forum.service.statistics.service.statistic.UserStatisticService;
import com.github.paicoding.forum.service.statistics.service.statistic.UserStatisticServiceProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.UUID;

/**
 * @program: pai_coding
 * @description: 多配置下实现的在线人数统计监听器
 * @author: XuYifei
 * @create: 2024-10-22
 */

@Slf4j
@Configuration
@EnableConfigurationProperties(UserStatisticServiceProperties.class)
public class OnlineUserInterceptor {

    @Autowired
    private UserStatisticService userStatisticService;

    private static final String SESSION_COOKIE_NAME = "SESSION_ID";

    @Bean
    public AsyncHandlerInterceptor onlineUserStatisticInterceptor(UserStatisticServiceProperties userStatisticServiceProperties) {
        if(UserStatisticServiceProperties.UserStatisticServiceType.CAFFEINE.equals(userStatisticServiceProperties.getType())) {
            return new OnlineUserByCookieInterceptor();
        }else{
            return new OnlineUserBySessionInterceptor();
        }
    }


    private class OnlineUserByCookieInterceptor implements AsyncHandlerInterceptor {

        public OnlineUserByCookieInterceptor() {
            log.info("【OnlineUserByCookieInterceptor】 init");
        }


        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 检查请求中是否包含 SESSION_ID Cookie
            String sessionId = getSessionIdFromCookies(request);

            if (sessionId == null) {
                // 如果没有 sessionId，则生成新的 sessionId
                sessionId = UUID.randomUUID().toString();

                // 将 sessionId 存入缓存，标记用户为在线
                userStatisticService.incrOnlineUserCnt(1);

                // 将 sessionId 添加到 Cookie 返回给浏览器
                Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
                // 防止脚本访问
                sessionCookie.setHttpOnly(true);
                sessionCookie.setPath("/");
                // 30分钟有效期
                sessionCookie.setMaxAge(30 * 60);
                response.addCookie(sessionCookie);
            } else {
                // 如果携带了 sessionId，检查是否存在于缓存中
                if (!userStatisticService.isOnline(sessionId)) {
                    // 如果缓存中不存在该 sessionId，则标记用户为在线并存入缓存
                    userStatisticService.invalidateSession(sessionId);
                }
                userStatisticService.updateSessionExpireTime(sessionId);
            }
            // 继续处理请求
            return true;

        }

        // 从请求的 Cookie 中获取 sessionId
        private String getSessionIdFromCookies(HttpServletRequest request) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }
    }


    private class OnlineUserBySessionInterceptor implements AsyncHandlerInterceptor {

        public OnlineUserBySessionInterceptor() {
            log.info("【OnlineUserBySessionInterceptor】 init");
        }


        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30);


            // 检查请求中是否包含 SESSION_ID Cookie
//        String sessionId = getSessionIdFromCookies(request);

            if (session.isNew()) {
                // 如果没有 sessionId，则生成新的 sessionId
                String sessionId = session.getId();

                // 将 sessionId 存入缓存，标记用户为在线
//            userStatisticService.incrOnlineUserCnt(1);

                // 将 sessionId 添加到 Cookie 返回给浏览器
                Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
                // 防止脚本访问
                sessionCookie.setHttpOnly(true);
                sessionCookie.setPath("/");
                // 30分钟有效期
                sessionCookie.setMaxAge(30);
                response.addCookie(sessionCookie);
            }
            // 继续处理请求
            return true;

        }

        // 从请求的 Cookie 中获取 sessionId
        private String getSessionIdFromCookies(HttpServletRequest request) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

    }

}