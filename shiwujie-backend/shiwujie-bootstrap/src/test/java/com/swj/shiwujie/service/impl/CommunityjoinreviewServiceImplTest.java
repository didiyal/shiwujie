package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunityjoinreviewMapper;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.community.Communityjoinreview;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityReviewStatusEnum;
import com.swj.shiwujie.model.request.community.communityJoinReview.CommunityJoinReviewUpdateRequest;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CommunityjoinreviewServiceImpl} 单元测试——社区加入审核状态流转。
 *
 * <p>覆盖 updateCommunityJoinReview：审核通过（志愿者分支/盲人分支）/拒绝/记录不存在/审核结果空；
 * getCommunityJoinReviewVOById 的兜底；getCommunityJoinReviewVOList 的脱敏映射。
 *
 * <p>注：{@code updateCommunityJoinReview} 用 {@code synchronized(loginUserPhone.intern())}，
 * 单测传常量字符串不引发锁竞争。@Transactional 注解在纯单元测试下不起作用（无 Spring 上下文），按方法体逻辑测。
 */
@DisplayName("CommunityjoinreviewServiceImpl 审核流转")
@ExtendWith(MockitoExtension.class)
class CommunityjoinreviewServiceImplTest {

    private static final Long REVIEW_ID = 800L;
    private static final Long COMMUNITY_ID = 100L;
    private static final Long LOGIN_VOLUNTEER_ID = 10L;
    private static final String LOGIN_PHONE = "13800000000";

    @Mock
    private CommunityjoinreviewMapper communityjoinreviewMapper;

    @Mock
    private CommunityMapper communityMapper;

    @Mock
    private InnerVolunteerService innerVolunteerService;

    @Mock
    private InnerBlindService innerBlindService;

    @InjectMocks
    private CommunityjoinreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", communityjoinreviewMapper);
    }

    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    // ---------- updateCommunityJoinReview ----------
    @Nested
    @DisplayName("updateCommunityJoinReview 审核流转")
    class Update {

        @Test
        @DisplayName("reviewId 为 null → PARAMS_ERROR")
        void nullReviewId() {
            CommunityJoinReviewUpdateRequest req = new CommunityJoinReviewUpdateRequest();
            req.setReviewId(null);
            req.setReviewResult(true);

            expectCode(() -> service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("审核记录不存在 → PARAMS_ERROR")
        void recordAbsent() {
            CommunityJoinReviewUpdateRequest req = passReq();
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(null);

            expectCode(() -> service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("审核结果为 null → PARAMS_ERROR")
        void nullResult() {
            CommunityJoinReviewUpdateRequest req = new CommunityJoinReviewUpdateRequest();
            req.setReviewId(REVIEW_ID);
            req.setReviewResult(null);
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(pendingReview(null, null));

            expectCode(() -> service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("审核通过 + 志愿者申请 → 更新志愿者 communityId，状态置 已通过")
        void passVolunteer() {
            CommunityJoinReviewUpdateRequest req = passReq();
            Communityjoinreview review = pendingReview(20L, null); // volunteerId=20
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(20L);
            when(innerVolunteerService.getById(20L)).thenReturn(volunteer);
            when(communityjoinreviewMapper.updateById(any(Communityjoinreview.class))).thenReturn(1);

            boolean ok = service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE);

            assertThat(ok).isTrue();
            assertThat(review.getReviewStatus()).isEqualTo(CommunityReviewStatusEnum.PASSED.getReviewStatus());
            assertThat(review.getReviewerId()).isEqualTo(LOGIN_VOLUNTEER_ID);
            assertThat(volunteer.getCommunityId()).isEqualTo(COMMUNITY_ID);
            verify(innerVolunteerService).updateById(volunteer);
            verify(innerBlindService, never()).updateById(any(Blind.class));
        }

        @Test
        @DisplayName("审核通过 + 盲人申请 → 更新盲人 communityId")
        void passBlind() {
            CommunityJoinReviewUpdateRequest req = passReq();
            Communityjoinreview review = pendingReview(null, 30L); // blindId=30
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            Blind blind = new Blind();
            blind.setBlindId(30L);
            when(innerBlindService.getById(30L)).thenReturn(blind);
            when(communityjoinreviewMapper.updateById(any(Communityjoinreview.class))).thenReturn(1);

            boolean ok = service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE);

            assertThat(ok).isTrue();
            assertThat(blind.getCommunityId()).isEqualTo(COMMUNITY_ID);
            verify(innerBlindService).updateById(blind);
            verify(innerVolunteerService, never()).updateById(any(Volunteer.class));
        }

        @Test
        @DisplayName("审核拒绝 → 状态置 已拒绝，不更新任何用户")
        void reject() {
            CommunityJoinReviewUpdateRequest req = new CommunityJoinReviewUpdateRequest();
            req.setReviewId(REVIEW_ID);
            req.setReviewResult(false);
            Communityjoinreview review = pendingReview(20L, null);
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            when(communityjoinreviewMapper.updateById(any(Communityjoinreview.class))).thenReturn(1);

            boolean ok = service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE);

            assertThat(ok).isTrue();
            assertThat(review.getReviewStatus()).isEqualTo(CommunityReviewStatusEnum.REJECTED.getReviewStatus());
            verify(innerVolunteerService, never()).updateById(any(Volunteer.class));
            verify(innerBlindService, never()).updateById(any(Blind.class));
        }

        @Test
        @DisplayName("updateById 失败 → SYSTEM_ERROR")
        void updateFails() {
            CommunityJoinReviewUpdateRequest req = passReq();
            Communityjoinreview review = pendingReview(20L, null);
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            Volunteer volunteer = new Volunteer();
            when(innerVolunteerService.getById(20L)).thenReturn(volunteer);
            when(communityjoinreviewMapper.updateById(any(Communityjoinreview.class))).thenReturn(0);

            expectCode(() -> service.updateCommunityJoinReview(req, LOGIN_VOLUNTEER_ID, LOGIN_PHONE), ErrorCode.SYSTEM_ERROR);
        }
    }

    // ---------- getCommunityJoinReviewVOById ----------
    @Nested
    @DisplayName("getCommunityJoinReviewVOById 详情")
    class GetById {

        @Test
        @DisplayName("ID 非法（<=0） → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.getCommunityJoinReviewVOById(0L), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("记录不存在 → PARAMS_ERROR")
        void absent() {
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(null);
            expectCode(() -> service.getCommunityJoinReviewVOById(REVIEW_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("命中 → 状态名转换正确")
        void found() {
            Communityjoinreview r = pendingReview(20L, null);
            r.setReviewStatus(CommunityReviewStatusEnum.PASSED.getReviewStatus());
            when(communityjoinreviewMapper.selectById(REVIEW_ID)).thenReturn(r);

            var vo = service.getCommunityJoinReviewVOById(REVIEW_ID);

            assertThat(vo.getReviewId()).isEqualTo(REVIEW_ID);
            assertThat(vo.getReviewStatus()).isEqualTo(CommunityReviewStatusEnum.PASSED.getName());
        }
    }

    // ---------- getCommunityJoinReviewVOList ----------
    @Nested
    @DisplayName("getCommunityJoinReviewVOList 列表")
    class GetList {

        @Test
        @DisplayName("管理员未关联社区（communityMapper.selectOne 返 null） → PARAMS_ERROR")
        void noCommunity() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(LOGIN_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getById(LOGIN_VOLUNTEER_ID)).thenReturn(v);
            when(communityMapper.selectOne(any())).thenReturn(null);

            expectCode(() -> service.getCommunityJoinReviewVOList(LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("命中 → 返审核 VO 列表，状态转中文名")
        void list() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(LOGIN_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getById(LOGIN_VOLUNTEER_ID)).thenReturn(v);
            Community c = new Community();
            c.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectOne(any())).thenReturn(c);

            Communityjoinreview r1 = pendingReview(20L, null);
            r1.setReviewStatus(CommunityReviewStatusEnum.WAIT_REVIEW.getReviewStatus());
            when(communityjoinreviewMapper.selectList(any(Wrapper.class)))
                    .thenReturn(List.of(r1));

            var result = service.getCommunityJoinReviewVOList(LOGIN_VOLUNTEER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReviewStatus()).isEqualTo(CommunityReviewStatusEnum.WAIT_REVIEW.getName());
        }

        @Test
        @DisplayName("空列表")
        void empty() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(LOGIN_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getById(LOGIN_VOLUNTEER_ID)).thenReturn(v);
            Community c = new Community();
            c.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectOne(any())).thenReturn(c);
            when(communityjoinreviewMapper.selectList(any(Wrapper.class)))
                    .thenReturn(Collections.emptyList());

            var result = service.getCommunityJoinReviewVOList(LOGIN_VOLUNTEER_ID);
            assertThat(result).isEmpty();
        }
    }

    // ---------- 辅助 ----------
    private CommunityJoinReviewUpdateRequest passReq() {
        CommunityJoinReviewUpdateRequest req = new CommunityJoinReviewUpdateRequest();
        req.setReviewId(REVIEW_ID);
        req.setReviewResult(true);
        return req;
    }

    private Communityjoinreview pendingReview(Long volunteerId, Long blindId) {
        Communityjoinreview r = new Communityjoinreview();
        r.setReviewId(REVIEW_ID);
        r.setCommunityId(COMMUNITY_ID);
        r.setVolunteerId(volunteerId);
        r.setBlindId(blindId);
        r.setReviewStatus(CommunityReviewStatusEnum.WAIT_REVIEW.getReviewStatus());
        return r;
    }
}
