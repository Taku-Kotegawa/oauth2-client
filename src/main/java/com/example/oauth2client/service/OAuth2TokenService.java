package com.example.oauth2client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class OAuth2TokenService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final DefaultRefreshTokenTokenResponseClient tokenResponseClient;

    public OAuth2TokenService(OAuth2AuthorizedClientService authorizedClientService, RestTemplateBuilder restTemplateBuilder) {
        this.authorizedClientService = authorizedClientService;
        this.tokenResponseClient = new DefaultRefreshTokenTokenResponseClient();
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(300))
                .setReadTimeout(Duration.ofMillis(300))
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .messageConverters(
                        new OAuth2AccessTokenResponseHttpMessageConverter(),
                        new OAuth2ErrorHttpMessageConverter(),
                        new FormHttpMessageConverter())
                .build();
        tokenResponseClient.setRestOperations(restTemplate);
    }

    /**
     * ????????????????????????????????????????????????
     */
    public String getAccessTokenValue() {
        OAuth2AccessToken accessToken = getAuthorizedClient().getAccessToken();
        // ?????????????????????????????????????????????????????????????????????
        if (isExpired(accessToken)) {
            log.debug("Access token was expired!");
            accessToken = refresh();
        }
        String tokenValue = accessToken.getTokenValue();
        log.debug("access_token = {}", tokenValue);
        return tokenValue;
    }

    /**
     * ??????????????????????????????????????????????????????
     */
    public String getRefreshTokenValue() {
        OAuth2RefreshToken refreshToken = getAuthorizedClient().getRefreshToken();
        String tokenValue = refreshToken.getTokenValue();
        return tokenValue;
    }

    /**
     * ????????????????????????????????????????????????true????????????
     */
    private boolean isExpired(OAuth2AccessToken accessToken) {
        return accessToken.getExpiresAt().isBefore(Instant.now());
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     */
    private OAuth2AccessToken refresh() {
        // ?????????????????????????????????
        OAuth2AuthorizedClient currentAuthorizedClient = getAuthorizedClient();
        ClientRegistration clientRegistration = currentAuthorizedClient.getClientRegistration();
        OAuth2RefreshTokenGrantRequest tokenRequest =
                new OAuth2RefreshTokenGrantRequest(clientRegistration,
                        currentAuthorizedClient.getAccessToken(),
                        currentAuthorizedClient.getRefreshToken());
        OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(tokenRequest);
        // ???????????????????????????????????????????????????
        authorizedClientService.removeAuthorizedClient(
                clientRegistration.getRegistrationId(),
                currentAuthorizedClient.getPrincipalName());
        // ????????????????????????????????????????????????
        OAuth2AuthenticationToken authentication = getAuthentication();
        OAuth2AuthorizedClient newAuthorizedClient = new OAuth2AuthorizedClient(
                clientRegistration, authentication.getName(),
                tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
        authorizedClientService.saveAuthorizedClient(newAuthorizedClient, authentication);
        log.debug("Refreshing token completed");
        return tokenResponse.getAccessToken();
    }

    /**
     * OAuth2AuthorizedClient??????????????????
     */
    public OAuth2AuthorizedClient getAuthorizedClient() {
        // OAuth2AuthenticationToken???Authentication????????????????????????????????????
        OAuth2AuthenticationToken authentication = getAuthentication();
        // OAuth2AuthorizedClient?????????
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());
        return authorizedClient;
    }

    /**
     * OAuth2AuthenticationToken??????????????????
     */
    public OAuth2AuthenticationToken getAuthentication() {
        OAuth2AuthenticationToken authentication =
                (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authentication;
    }

}