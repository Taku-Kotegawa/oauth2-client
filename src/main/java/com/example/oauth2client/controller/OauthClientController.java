package com.example.oauth2client.controller;

import com.example.oauth2client.service.OAuth2TokenService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Controller
public class OauthClientController {

    public OauthClientController(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService authorizedClientService, RestTemplate restTemplate, OAuth2TokenService oAuth2TokenService) {
        this.oAuth2TokenService = oAuth2TokenService;
        this.restTemplate = restTemplate;
    }

    private final OAuth2TokenService oAuth2TokenService;

    private final RestTemplate restTemplate;

    @Value("${resource-server.base-uri}")
    private String resourceServerBaseUri;

    @GetMapping("")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("list")
    public String list(Model model, @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {

        String accessToken = oAuth2TokenService.getAccessTokenValue();
        String uri = resourceServerBaseUri + "/resource/list";

        try {
            RequestEntity requestEntity = RequestEntity
                    .get(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(requestEntity, String.class);

        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }

        return "list";
    }



}
