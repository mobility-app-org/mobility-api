package com.mobility.api.global.config;

import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.annotation.CurrentUser;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import com.mobility.api.global.security.PrincipalDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor // final 필드 주입을 위한 어노테이션 추가
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    // 9. 'dev' 프로필 확인 및 임시 ID 조회를 위해 Bean 주입
    private final Environment env;
    private final TransporterRepository transporterRepository;

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

        Class<?> parameterType = parameter.getParameterType(); // 컨트롤러가 요청한 파라미터 타입

        // -----------------------------------------------------------
        // 1. [DEV/LOCAL] 개발 환경용 인증 우회 로직
        // -----------------------------------------------------------
        if (env.acceptsProfiles(Profiles.of("dev", "local"))) {
            HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
            String tempUserIdHeader = request.getHeader("X-Temp-User-Id"); // Postman에서 보낸 헤더

            // 헤더가 없으면 기본값 1L 사용, 있으면 파싱
            Long targetUserId = (tempUserIdHeader == null || tempUserIdHeader.isBlank())
                    ? 1L
                    : Long.parseLong(tempUserIdHeader);
            try {
                // (1) ID(Long)만 필요한 경우
                if (Long.class.isAssignableFrom(parameterType)) {
                    return targetUserId;
                }

                // (2) 엔티티(Transporter)가 필요한 경우
                if (Transporter.class.isAssignableFrom(parameterType)) {
                    return transporterRepository.findById(targetUserId)
                            .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
                }
            } catch (NumberFormatException e) {
                throw new GlobalException(ResultCode.DEV_BAD_REQUEST);
            }
        }

        // -----------------------------------------------------------
        // 2. [PROD] 실제 운영 환경 인증 로직 (Spring Security)
        // -----------------------------------------------------------
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails)) {
            throw new GlobalException(ResultCode.UNAUTHORIZED);
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        // (1) ID(Long) 반환
        if (Long.class.isAssignableFrom(parameterType)) {
            return principalDetails.getTransporterId(); // PrincipalDetails에 해당 메서드 필요
        }

        // (2) 엔티티(Transporter) 반환
        // 주의: 세션/토큰에는 보통 엔티티 전체를 담지 않으므로, 여기서 ID로 다시 조회하는 것이 안전합니다.
        if (Transporter.class.isAssignableFrom(parameterType)) {
            return transporterRepository.findById(principalDetails.getTransporterId())
                    .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
        }

        if (PrincipalDetails.class.isAssignableFrom(parameterType)) {
            return principalDetails;
        }

        throw new IllegalArgumentException("지원하지 않는 파라미터 타입입니다: " + parameterType);
    }
}