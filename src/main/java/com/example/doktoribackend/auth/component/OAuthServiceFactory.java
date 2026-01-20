package com.example.doktoribackend.auth.component;

import com.example.doktoribackend.auth.service.OAuthService;
import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthServiceFactory {

    private final Map<OAuthProvider, OAuthService> services;

    public OAuthServiceFactory(List<OAuthService> oauthServices) {
        this.services = oauthServices.stream()
                .collect(Collectors.toMap(
                        OAuthService::getProvider,
                        Function.identity()
                ));
    }

    public OAuthService getService(OAuthProvider provider) {
        OAuthService service = services.get(provider);
        if (service == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
        return service;
    }
}