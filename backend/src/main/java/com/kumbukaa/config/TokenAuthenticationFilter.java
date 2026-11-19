package com.kumbukaa.config;

import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.UserRoleUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String defaultAdminEmail;

    @Autowired
    public TokenAuthenticationFilter(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.admin.email:admin@localhost}") String defaultAdminEmail) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultAdminEmail = defaultAdminEmail;
    }

    public TokenAuthenticationFilter(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this(userRepository, jwtTokenProvider, "admin@localhost");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return request.getMethod().equalsIgnoreCase("OPTIONS")
                || "/api/auth".equals(path)
                || path.startsWith("/api/auth/")
                || "/api/admin/login".equals(path)
                || "/health".equals(path)
                || "/".equals(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank() || !jwtTokenProvider.validateAccessToken(token)) {
            sendUnauthorized(response);
            return;
        }

        Long userId = jwtTokenProvider.getUserId(token);
        if (userId == null) {
            sendUnauthorized(response);
            return;
        }

        var authentication = userRepository.findById(userId)
            .filter(user -> !"SUSPENDED".equals(user.getAccountStatus()))
            .map(user -> {
                String roles = user.getRoles();
                boolean isDesignatedAdmin = user.getEmail() != null && (
                        user.getEmail().equalsIgnoreCase(defaultAdminEmail)
                                || user.getEmail().equalsIgnoreCase("admin@localhost")
                );
                if (isDesignatedAdmin && (roles == null || !UserRoleUtils.hasRole(roles, "ADMIN"))) {
                    roles = (roles == null || roles.isBlank()) ? "ROLE_ADMIN,ROLE_USER" : roles + ",ROLE_ADMIN";
                    user.setRoles(roles);
                    try {
                        userRepository.save(user);
                    } catch (Exception ignored) {
                    }
                }
                return new UsernamePasswordAuthenticationToken(
                        user, null, UserRoleUtils.authorities(roles));
            })
            .orElse(null);

        if (authentication == null) {
            sendUnauthorized(response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}
