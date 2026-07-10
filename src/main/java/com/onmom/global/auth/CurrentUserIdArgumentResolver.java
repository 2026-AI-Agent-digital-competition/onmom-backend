package com.onmom.global.auth;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public CurrentUserIdArgumentResolver(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_AUTHORIZATION_HEADER);
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_AUTHORIZATION_HEADER);
        }
        return jwtTokenProvider.getUserId(accessToken);
    }
}
