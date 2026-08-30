package ru.merezh.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.merezh.gateway.security.dto.JwtUserDto;
import ru.merezh.gateway.security.service.JwtValidateService;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtValidateService jwtValidateService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException
    {
        String token = getTokenFromRequest(request);

        if (token != null && jwtValidateService.checkValidateAccessToken(token)) {
            JwtUserDto userAuth = addUserInContext(token);

            request.setAttribute("userId", String.valueOf(userAuth.id()));
            request.setAttribute("userRole", userAuth.role());
        }

        doFilter(request, response, filterChain);
    }

    private JwtUserDto addUserInContext(String token) {
        JwtUserDto userDto = jwtValidateService.getUserDataFromToken(token);

        UsernamePasswordAuthenticationToken userAuth =  new UsernamePasswordAuthenticationToken(
                userDto.id(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role()))
        );

        SecurityContextHolder.getContext().setAuthentication(userAuth);
        log.info("id - {}, role - {}", userDto.id(), userDto.role());

        return userDto;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        return null;
    }
}
