package com.anonranker.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Gates every "/s/{adminToken}/**" admin-area request behind the session
 * password (checked once, remembered in {@link AuthAttributes#ADMIN_AUTH_TOKENS}),
 * and additionally gates the announcement pages behind a second, independent
 * re-entry of the same password ({@link AuthAttributes#ANNOUNCE_AUTH_TOKENS}).
 */
public class SessionAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String ANNOUNCE_PATTERN_1 = "/s/*/announce";
    private static final String ANNOUNCE_PATTERN_2 = "/s/*/announce/*/data";

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String adminToken = pathVariables != null ? pathVariables.get("adminToken") : null;
        if (adminToken == null) {
            return true;
        }

        HttpSession session = request.getSession(true);
        Set<String> adminAuth = (Set<String>) session.getAttribute(AuthAttributes.ADMIN_AUTH_TOKENS);
        if (adminAuth == null || !adminAuth.contains(adminToken)) {
            redirectTo(response, "/s/" + adminToken + "/login", request);
            return false;
        }

        String path = request.getServletPath();
        boolean isAnnounceArea = PATH_MATCHER.match(ANNOUNCE_PATTERN_1, path) || PATH_MATCHER.match(ANNOUNCE_PATTERN_2, path);
        if (isAnnounceArea) {
            Set<String> announceAuth = (Set<String>) session.getAttribute(AuthAttributes.ANNOUNCE_AUTH_TOKENS);
            if (announceAuth == null || !announceAuth.contains(adminToken)) {
                redirectTo(response, "/s/" + adminToken + "/announce/login", request);
                return false;
            }
        }

        return true;
    }

    private void redirectTo(HttpServletResponse response, String location, HttpServletRequest request) throws java.io.IOException {
        String redirectParam = URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + location + "?redirect=" + redirectParam);
    }
}
