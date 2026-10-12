package com.casino.platform.provider.security;

import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.exception.ProviderAuthenticationException;
import com.casino.platform.provider.service.ProviderConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication filter for Provider Wallet API requests.
 * Validates HMAC signature and nonce for each request.
 */
public class ProviderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_KEY_ID = "X-Provider-Key-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_PLAYER_ID = "X-Player-Id";

    private final ProviderConfigService providerConfigService;
    private final HmacSignatureValidator signatureValidator;
    private final NonceService nonceService;
    private final ObjectMapper objectMapper;

    public ProviderAuthenticationFilter(
            ProviderConfigService providerConfigService,
            HmacSignatureValidator signatureValidator,
            NonceService nonceService,
            ObjectMapper objectMapper) {
        this.providerConfigService = providerConfigService;
        this.signatureValidator = signatureValidator;
        this.nonceService = nonceService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract authentication headers
            String keyId = request.getHeader(HEADER_KEY_ID);
            String timestampStr = request.getHeader(HEADER_TIMESTAMP);
            String nonce = request.getHeader(HEADER_NONCE);
            String signature = request.getHeader(HEADER_SIGNATURE);
            String playerProviderId = request.getHeader(HEADER_PLAYER_ID);

            // Validate required headers
            if (keyId == null || timestampStr == null || nonce == null || signature == null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing authentication headers");
                return;
            }

            Long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid timestamp format");
                return;
            }

            // Get provider by key ID
            Provider provider = providerConfigService.getProviderByKeyId(keyId);

            // Cache request body for signature validation (body can only be read once)
            ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
            String body = getRequestBody(cachedRequest);

            // Build signature components
            String method = request.getMethod();
            String url = request.getRequestURI();

            // Validate HMAC signature
            signatureValidator.validateSignature(
                    method,
                    url,
                    timestamp,
                    nonce,
                    body,
                    signature,
                    provider.getSecretKey()
            );

            // Validate and store nonce (prevent replay attacks)
            nonceService.validateAndStoreNonce(provider, nonce, timestamp);

            // Create authentication token
            ProviderAuthenticationToken authToken = new ProviderAuthenticationToken(
                    provider,
                    playerProviderId,
                    Collections.emptyList()
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Continue filter chain
            filterChain.doFilter(cachedRequest, response);

        } catch (ProviderAuthenticationException e) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication failed");
        }
    }

    /**
     * Read request body from cached request wrapper.
     */
    private String getRequestBody(ContentCachingRequestWrapper request) throws IOException {
        byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }

    /**
     * Send JSON error response.
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);
        errorBody.put("error", message);
        errorBody.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
