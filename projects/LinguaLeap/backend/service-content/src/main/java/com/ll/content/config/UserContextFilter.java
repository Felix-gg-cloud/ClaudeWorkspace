package com.ll.content.config;

import com.ll.common.util.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String userIdHeader = httpReq.getHeader(UserContext.HEADER_USER_ID);
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                UserContext.setUserId(Long.valueOf(userIdHeader));
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
