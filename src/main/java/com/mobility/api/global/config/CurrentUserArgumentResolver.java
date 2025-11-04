package com.mobility.api.global.config;

import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.global.annotation.CurrentUser;
import com.mobility.api.global.security.PrincipalDetails;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 이 Resolver가 어떤 파라미터를 지원(support)할 것인지 결정합
     * @CurrentUser 어노테이션이 붙어있는 파라미터라면 true를 반환
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    /**
     * supportsParameter가 true를 반환했을 때,
     * 파라미터에 실제로 주입할 값(Object)을 결정(resolve)하여 반환합니다.
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. 인증 정보가 없거나, principal이 PrincipalDetails 타입이 아닌 경우 (예: 익명 사용자)
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails)) {
            // 5. 혹은 인증이 필수인 경우 여기서 예외(Exception)를 던져도 됩니다.
            return null;
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        // 2. 컨트롤러가 요청한 파라미터의 타입을 확인합니다.
        Class<?> parameterType = parameter.getParameterType();

        // 3. 타입에 따라 원하는 값을 반환합니다.
        if (Long.class.isAssignableFrom(parameterType)) {
            // @CurrentUser Long transporterId
            return principalDetails.getTransporterId();
        }

//        if (Transporter.class.isAssignableFrom(parameterType)) {
//            // @CurrentUser Transporter transporter
//            // 6. PrincipalDetails에 getTransporter() 메서드가 있다고 가정
//            return principalDetails.getTransporter();
//        }

        if (PrincipalDetails.class.isAssignableFrom(parameterType)) {
            // @CurrentUser PrincipalDetails principalDetails
            return principalDetails;
        }

        // 4. 지원하지 않는 타입이 요청된 경우
        throw new IllegalArgumentException("지원하지 않는 파라미터 타입입니다: " + parameterType);
    }
}