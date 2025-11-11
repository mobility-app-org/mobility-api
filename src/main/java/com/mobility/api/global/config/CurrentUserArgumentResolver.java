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

        // 'dev' 또는 'local' 프로필이 활성화되어 있을 때만 이 로직을 실행합니다.
        if (env.acceptsProfiles(Profiles.of("dev", "local"))) {
            HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
            String tempUserIdHeader = request.getHeader("X-Temp-User-Id"); // Postman에서 보낸 헤더

            if (tempUserIdHeader != null && !tempUserIdHeader.isBlank()) {
                try {
                    Long tempUserId = Long.parseLong(tempUserIdHeader);

                    // 컨트롤러가 요청한 타입에 맞춰 임시 값을 반환합니다.
                    if (Long.class.isAssignableFrom(parameterType)) {
                        // @CurrentUser Long transporterId
                        return tempUserId; // DB 조회 없이 ID만 반환
                    }
                    if (Transporter.class.isAssignableFrom(parameterType)) {
                        // @CurrentUser Transporter transporter
                        // DB에서 임시 ID로 기사를 조회해서 반환
                        return transporterRepository.findById(tempUserId)
                                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));
                    }

                } catch (NumberFormatException e) {
                    throw new GlobalException(ResultCode.DEV_BAD_REQUEST);
                }
            }
            // 'dev' 프로필이지만 헤더가 없는 경우, Postman 테스트 편의를 위해 1L을 기본값으로 사용할 수 있습니다.
            // (이 부분이 필요 없다면 삭제)
            // if (Long.class.isAssignableFrom(parameterType)) return 1L;
            // if (Transporter.class.isAssignableFrom(parameterType)) return transporterRepository.findById(1L).get();
        }

        // --- 11. [운영(prod) 환경 또는 dev 바이패스 실패 시의 실제 인증 로직] ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails)) {
            // 'prod' 환경에서 인증 실패 시 null 대신 명시적인 예외를 발생시킵니다.
            throw new GlobalException(ResultCode.UNAUTHORIZED);
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        if (Long.class.isAssignableFrom(parameterType)) {
            // @CurrentUser Long transporterId
            return principalDetails.getTransporterId();
        }

//        if (Transporter.class.isAssignableFrom(parameterType)) {
            // @CurrentUser Transporter transporter (컨트롤러에서 사용 중이므로 주석 해제)
            // (PrincipalDetails에 getTransporter() 메서드가 반드시 있어야 함)
//            return principalDetails.getTransporter();
//        }

        if (PrincipalDetails.class.isAssignableFrom(parameterType)) {
            // @CurrentUser PrincipalDetails principalDetails
            return principalDetails;
        }

        throw new IllegalArgumentException("지원하지 않는 파라미터 타입입니다: " + parameterType);
    }
}