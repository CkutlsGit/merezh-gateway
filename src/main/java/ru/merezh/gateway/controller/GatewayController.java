package ru.merezh.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Enumeration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${service.users.url}")
    private String userUrl;

    @Value("${service.auth.url}")
    private String authUrl;

    @RequestMapping("/api/v1/users/**")
    public ResponseEntity<?> proxyUsers(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    )
    {
        return forwardRequest(userUrl, request, body);
    }

    @RequestMapping("/api/v1/auth/**")
    public ResponseEntity<?> proxyAuth(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    )
    {
        return forwardRequest(authUrl, request, body);
    }

    private ResponseEntity<?> forwardRequest(
            String baseUrl,
            HttpServletRequest request,
            String body
    )
    {
        try {
            String path = request.getRequestURI();
            String fullUrl = baseUrl + path.substring(path.indexOf("/api"));
            log.info("Путь - {}, полный путь - {}", path, fullUrl);

            HttpHeaders headers = new HttpHeaders();
            copyHeaders(request, headers);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                String userId = (String) request.getAttribute("userId");
                String userRole = (String) request.getAttribute("userRole");

                if (userId != null) {
                    headers.add("X-User-Id", userId);
                    headers.add("X-User-Role", userRole);
                }
            }

            HttpEntity<String> httpEntity = new HttpEntity(body, headers);

            return restTemplate.exchange(
                    fullUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    httpEntity,
                    String.class
            );
        }
        catch (HttpClientErrorException e) {
            log.error("Ошибка на стороне клиента - {}: {}", e.getClass(), e.getMessage());

            JsonNode jsonNode = objectMapper.readTree(e.getResponseBodyAsString());

            return ResponseEntity.status(e.getStatusCode()).body(jsonNode);
        }
        catch (HttpServerErrorException e) {
            log.error("Ошибка на стороне сервера - {}, {}", e.getClass(), e.getMessage());

            JsonNode jsonNode = objectMapper.readTree(e.getResponseBodyAsString());

            return ResponseEntity.status(e.getStatusCode()).body(jsonNode);
        }
        catch (ResourceAccessException e) {
            log.error("Ошибка с подключением - {}, {}", e.getClass(), e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Не удалось подключиться к сервису");
        }
        catch (Exception e) {
            log.error("Ошибка при адресации - {} : {}", e.getClass(), e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при отправке запроса");
        }
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            if (!headerName.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                String headerValue = request.getHeader(headerName);
                headers.add(headerName, headerValue);
            }
        }
    }
}
