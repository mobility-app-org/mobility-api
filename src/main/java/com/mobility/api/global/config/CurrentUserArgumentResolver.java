package com.mobility.api.global.config;

import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.repository.ManagerRepository;
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
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final Environment env;
    private final TransporterRepository transporterRepository;
    private final ManagerRepository managerRepository; // 👈 Manager 조회를 위해 추가!

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Class<?> parameterType = parameter.getParameterType(); // 컨트롤러가 원하는 타입 (Manager? Transporter?)

        // ========================================================================
        // 1. [DEV/LOCAL] 개발 환경용 "프리패스" 로직 (헤더로 로그인 흉내내기)
        // ========================================================================
        if (env.acceptsProfiles(Profiles.of("dev", "local"))) {
            HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
            String tempUserIdHeader = request.getHeader("X-Temp-User-Id");

            // 헤더 없으면 ID 1번으로 간주
            Long targetUserId = (tempUserIdHeader == null || tempUserIdHeader.isBlank())
                    ? 1L
                    : Long.parseLong(tempUserIdHeader);

            // (A) 컨트롤러가 "기사(Transporter)"를 원할 때
            if (Transporter.class.isAssignableFrom(parameterType)) {
                return transporterRepository.findById(targetUserId)
                        .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
            }

            // (B) 컨트롤러가 "매니저(Manager)"를 원할 때
            if (Manager.class.isAssignableFrom(parameterType)) {
                return managerRepository.findById(targetUserId)
                        .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
            }
        }

        // ========================================================================
        // 2. [PROD] 실제 운영 환경 인증 로직 (Spring Security / JWT)
        // ========================================================================
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails)) {
            throw new GlobalException(ResultCode.UNAUTHORIZED);
        }

        // SecurityContext에 저장된 "통합 유저 객체"를 꺼냄
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        // (A) 컨트롤러가 "기사(Transporter)"를 원할 때
        if (Transporter.class.isAssignableFrom(parameterType)) {
            if (principalDetails.getTransporter() == null) {
                // 로그인한 사람은 매니저인데, 기사 정보를 달라고 하면 에러!
                throw new GlobalException(ResultCode.FORBIDDEN);
            }
            return principalDetails.getTransporter();
        }

        // (B) 컨트롤러가 "매니저(Manager)"를 원할 때
        if (Manager.class.isAssignableFrom(parameterType)) {
            if (principalDetails.getManager() == null) {
                // 로그인한 사람은 기사인데, 매니저 정보를 달라고 하면 에러!
                throw new GlobalException(ResultCode.FORBIDDEN);
            }
            return principalDetails.getManager();
        }

        // (C) 그냥 ID(Long)만 원할 때 (잘 안 쓰지만 혹시 몰라 유지)
        if (Long.class.isAssignableFrom(parameterType)) {
            if (principalDetails.getManager() != null) return principalDetails.getManager().getId();
            if (principalDetails.getTransporter() != null) return principalDetails.getTransporter().getId();
        }

        // (D) PrincipalDetails 자체를 원할 때
        if (PrincipalDetails.class.isAssignableFrom(parameterType)) {
            return principalDetails;
        }

        throw new IllegalArgumentException("지원하지 않는 @CurrentUser 파라미터 타입입니다: " + parameterType);
    }
}