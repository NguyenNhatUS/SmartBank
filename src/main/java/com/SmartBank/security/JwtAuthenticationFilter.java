package com.SmartBank.security;

import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.service.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserAccessValidator userAccessValidator;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            UserAccessValidator userAccessValidator,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userAccessValidator = userAccessValidator;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtTokenProvider.isTokenValid(token) || tokenBlacklistService.isBlacklisted(token)) {
                reject(request, response, ErrorCode.INVALID_TOKEN);
                return;
            }

            String username = jwtTokenProvider.extractUsername(token);
            String role = jwtTokenProvider.extractRole(token);
            userAccessValidator.validate(username, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AppException e) {
            reject(request, response, e.getErrorCode());
            return;
        } catch (DataAccessException e) {
            // Fail closed: a Redis outage must not let revoked tokens through.
            reject(request, response, ErrorCode.AUTH_SERVICE_UNAVAILABLE);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, response, ErrorCode.INVALID_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);

    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode) {

        SecurityContextHolder.clearContext();

        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new AppException(errorCode)
        );
    }
}
