package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.office.dto.response.DispatchFeedRes;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.enums.ManagerRole;
import com.mobility.api.domain.office.repository.ManagerRepository;
import com.mobility.api.domain.office.service.OfficeService;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.jwt.JwtProvider;
import com.mobility.api.global.security.CustomUserDetailsService;
import com.mobility.api.global.security.PrincipalDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfficeV1Controller.class)
class OfficeV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfficeService officeService;

    @MockitoBean
    private ManagerRepository managerRepository;

    @MockitoBean
    private TransporterRepository transporterRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Manager testManager;
    private Office testOffice;
    private PrincipalDetails testPrincipalDetails;

    @BeforeEach
    void setUp() throws Exception {
        // Mock Office 생성
        testOffice = Office.builder()
                .officeName("테스트 사무실")
                .officeRegistrationNumber("123-45-67890")
                .officeAddress("서울시 강남구")
                .officeTelNumber("02-1234-5678")
                .build();

        // Reflection으로 id 설정
        java.lang.reflect.Field officeIdField = Office.class.getDeclaredField("id");
        officeIdField.setAccessible(true);
        officeIdField.set(testOffice, 1L);

        // Mock Manager 생성
        testManager = Manager.builder()
                .loginId("testmanager")
                .password("encodedPassword")
                .name("테스트 관리자")
                .phone("010-1234-5678")
                .email("test@example.com")
                .role(ManagerRole.OWNER)
                .office(testOffice)
                .build();

        // Reflection으로 id 설정
        java.lang.reflect.Field managerIdField = Manager.class.getDeclaredField("id");
        managerIdField.setAccessible(true);
        managerIdField.set(testManager, 1L);

        // PrincipalDetails 생성
        testPrincipalDetails = new PrincipalDetails(testManager);
    }

    @Test
    @DisplayName("[API] 배차 피드 조회 - 성공 (200 OK)")
    @WithMockUser
    void getDispatchFeed_Success() throws Exception {
        // given
        Integer limit = 20;

        List<DispatchFeedRes> mockFeedList = List.of(
                DispatchFeedRes.builder()
                        .id("feed-01")
                        .type("assigned")
                        .dispatchId(101L)
                        .dispatchNumber("2024-0001")
                        .transporterName("김철수")
                        .message("김철수 기사가 콜 #2024-0001을 배차 받았습니다")
                        .timestamp(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                        .build(),
                DispatchFeedRes.builder()
                        .id("feed-02")
                        .type("open")
                        .dispatchId(102L)
                        .dispatchNumber("2024-0002")
                        .transporterName(null)
                        .message("배차 #2024-0002가 등록되었습니다")
                        .timestamp(LocalDateTime.of(2024, 1, 15, 10, 25, 0))
                        .build(),
                DispatchFeedRes.builder()
                        .id("feed-03")
                        .type("completed")
                        .dispatchId(103L)
                        .dispatchNumber("2024-0003")
                        .transporterName("이영희")
                        .message("이영희 기사가 콜 #2024-0003을 완료했습니다")
                        .timestamp(LocalDateTime.of(2024, 1, 15, 10, 20, 0))
                        .build()
        );

        // Stubbing
        given(officeService.getDispatchFeed(eq(limit), any(Manager.class)))
                .willReturn(mockFeedList);

        // when & then
        mockMvc.perform(get("/api/v1/office/dispatch/feed")
                        .param("limit", String.valueOf(limit))
                        .with(user(testPrincipalDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())

                // CommonResponse 구조 검증
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.message").value("정상 처리 되었습니다."))

                // 데이터(List) 검증
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))

                // 첫 번째 피드 검증 (assigned)
                .andExpect(jsonPath("$.data[0].id").value("feed-01"))
                .andExpect(jsonPath("$.data[0].type").value("assigned"))
                .andExpect(jsonPath("$.data[0].dispatchId").value(101L))
                .andExpect(jsonPath("$.data[0].dispatchNumber").value("2024-0001"))
                .andExpect(jsonPath("$.data[0].transporterName").value("김철수"))
                .andExpect(jsonPath("$.data[0].message").value("김철수 기사가 콜 #2024-0001을 배차 받았습니다"))

                // 두 번째 피드 검증 (open)
                .andExpect(jsonPath("$.data[1].id").value("feed-02"))
                .andExpect(jsonPath("$.data[1].type").value("open"))
                .andExpect(jsonPath("$.data[1].transporterName").doesNotExist())

                // 세 번째 피드 검증 (completed)
                .andExpect(jsonPath("$.data[2].id").value("feed-03"))
                .andExpect(jsonPath("$.data[2].type").value("completed"))
                .andExpect(jsonPath("$.data[2].transporterName").value("이영희"));
    }

    @Test
    @DisplayName("[API] 배차 피드 조회 - 기본 limit 값 사용 (limit 미지정 시 20)")
    @WithMockUser
    void getDispatchFeed_DefaultLimit() throws Exception {
        // given
        List<DispatchFeedRes> mockFeedList = List.of(
                DispatchFeedRes.builder()
                        .id("feed-01")
                        .type("open")
                        .dispatchId(101L)
                        .dispatchNumber("2024-0001")
                        .transporterName(null)
                        .message("배차 #2024-0001가 등록되었습니다")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        // limit이 20으로 호출되어야 함
        given(officeService.getDispatchFeed(eq(20), any(Manager.class)))
                .willReturn(mockFeedList);

        // when & then
        mockMvc.perform(get("/api/v1/office/dispatch/feed")
                        .with(user(testPrincipalDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("[API] 배차 피드 조회 - 빈 결과 (배차가 없는 경우)")
    @WithMockUser
    void getDispatchFeed_EmptyResult() throws Exception {
        // given
        Integer limit = 20;
        List<DispatchFeedRes> emptyList = List.of();

        given(officeService.getDispatchFeed(eq(limit), any(Manager.class)))
                .willReturn(emptyList);

        // when & then
        mockMvc.perform(get("/api/v1/office/dispatch/feed")
                        .param("limit", String.valueOf(limit))
                        .with(user(testPrincipalDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("[API] 배차 피드 조회 - 인증 없이 접근 시 실패 (401 Unauthorized)")
    void getDispatchFeed_Unauthorized() throws Exception {
        // given - 인증 정보 없이 요청

        // when & then
        mockMvc.perform(get("/api/v1/office/dispatch/feed")
                        .param("limit", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[API] 배차 피드 조회 - 커스텀 limit 값 사용 (50)")
    @WithMockUser
    void getDispatchFeed_CustomLimit() throws Exception {
        // given
        Integer customLimit = 50;
        List<DispatchFeedRes> mockFeedList = List.of(
                DispatchFeedRes.builder()
                        .id("feed-01")
                        .type("canceled")
                        .dispatchId(101L)
                        .dispatchNumber("2024-0001")
                        .transporterName(null)
                        .message("배차 #2024-0001이 취소되었습니다")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        given(officeService.getDispatchFeed(eq(customLimit), any(Manager.class)))
                .willReturn(mockFeedList);

        // when & then
        mockMvc.perform(get("/api/v1/office/dispatch/feed")
                        .param("limit", String.valueOf(customLimit))
                        .with(user(testPrincipalDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("canceled"));
    }
}