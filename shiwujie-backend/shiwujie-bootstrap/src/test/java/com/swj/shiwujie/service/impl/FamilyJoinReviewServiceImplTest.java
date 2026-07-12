package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.FamilyJoinReviewMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.FamilyJoinReview;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.user.FamilyReviewStatusEnum;
import com.swj.shiwujie.model.request.user.familyJoinReview.FamilyJoinReviewUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FamilyJoinReviewServiceImpl} 单元测试——家庭加入审核状态流转。
 * 纯 Mockito：@ExtendWith(MockitoExtension) + @Mock + @InjectMocks，不起 Spring/不连库。
 * ServiceImpl 基类方法（getById/updateById/list 等）走 baseMapper（@Mock FamilyJoinReviewMapper）。
 */
@DisplayName("FamilyJoinReviewServiceImpl 家庭加入审核")
@ExtendWith(MockitoExtension.class)
class FamilyJoinReviewServiceImplTest {

    @Mock
    private FamilyJoinReviewMapper familyJoinReviewMapper;

    @Mock
    private VolunteerMapper volunteerMapper;

    @Mock
    private BlindMapper blindMapper;

    @InjectMocks
    private FamilyJoinReviewServiceImpl familyJoinReviewService;

    /**
     * 手动反射注入父类 baseMapper（同 BlindServiceImplTest 说明）。
     */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(familyJoinReviewService, "baseMapper", familyJoinReviewMapper);
    }

    // ==================== updateFamilyJoinReview ====================

    @Nested
    @DisplayName("updateFamilyJoinReview：审核状态流转")
    class UpdateFamilyJoinReview {

        @Test
        @DisplayName("reviewerId 为 null → 抛 PARAMS_ERROR")
        void update_nullReviewer_throws() {
            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewerId(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyJoinReviewService.updateFamilyJoinReview(req, 1L, "13900000000"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("非家主（loginVolunteerId != reviewerId）→ 抛 PARAMS_ERROR")
        void update_notOwner_throws() {
            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewerId(10L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyJoinReviewService.updateFamilyJoinReview(req, 999L, "13900000099"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("reviewId 为 null → 抛 PARAMS_ERROR")
        void update_nullReviewId_throws() {
            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewerId(10L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyJoinReviewService.updateFamilyJoinReview(req, 10L, "13900000010"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("🔥 审核通过 + 盲人申请：状态 PASSED + blind.familyId 被设置")
        void update_passBlind_setsBlindFamilyId() {
            Long reviewId = 1L;
            Long familyId = 50L;
            Long blindId = 100L;

            FamilyJoinReview review = new FamilyJoinReview();
            review.setReviewId(reviewId);
            review.setFamilyId(familyId);
            review.setBlindId(blindId);
            review.setVolunteerId(null);

            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewId(reviewId);
            req.setReviewResult(true); // 通过
            req.setReviewerId(10L);

            Blind blind = new Blind();
            blind.setBlindId(blindId);

            when(familyJoinReviewMapper.selectById(eq(reviewId))).thenReturn(review);
            when(blindMapper.selectById(eq(blindId))).thenReturn(blind);
            when(blindMapper.updateById(any(Blind.class))).thenReturn(1);
            when(familyJoinReviewMapper.updateById(any(FamilyJoinReview.class))).thenReturn(1);

            boolean result = familyJoinReviewService.updateFamilyJoinReview(req, 10L, "13900000010");

            assertThat(result).isTrue();
            // 审核记录状态 PASSED
            org.mockito.ArgumentCaptor<FamilyJoinReview> reviewCaptor =
                    org.mockito.ArgumentCaptor.forClass(FamilyJoinReview.class);
            verify(familyJoinReviewMapper).updateById(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getReviewStatus())
                    .isEqualTo(FamilyReviewStatusEnum.PASSED.getReviewStatus());
            // blind.familyId 被设置
            org.mockito.ArgumentCaptor<Blind> blindCaptor =
                    org.mockito.ArgumentCaptor.forClass(Blind.class);
            verify(blindMapper).updateById(blindCaptor.capture());
            assertThat(blindCaptor.getValue().getFamilyId()).isEqualTo(familyId);
        }

        @Test
        @DisplayName("🔥 审核通过 + 志愿者申请：状态 PASSED + volunteer.familyId 被设置")
        void update_passVolunteer_setsVolunteerFamilyId() {
            Long reviewId = 2L;
            Long familyId = 60L;
            Long volunteerId = 200L;

            FamilyJoinReview review = new FamilyJoinReview();
            review.setReviewId(reviewId);
            review.setFamilyId(familyId);
            review.setBlindId(null);
            review.setVolunteerId(volunteerId);

            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewId(reviewId);
            req.setReviewResult(true);
            req.setReviewerId(20L);

            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(volunteerId);

            when(familyJoinReviewMapper.selectById(eq(reviewId))).thenReturn(review);
            when(volunteerMapper.selectById(eq(volunteerId))).thenReturn(volunteer);
            when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);
            when(familyJoinReviewMapper.updateById(any(FamilyJoinReview.class))).thenReturn(1);

            boolean result = familyJoinReviewService.updateFamilyJoinReview(req, 20L, "13900000020");

            assertThat(result).isTrue();
            // volunteer.familyId 被设置
            org.mockito.ArgumentCaptor<Volunteer> vCaptor =
                    org.mockito.ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerMapper).updateById(vCaptor.capture());
            assertThat(vCaptor.getValue().getFamilyId()).isEqualTo(familyId);
        }

        @Test
        @DisplayName("🔥 审核拒绝：状态 REJECTED，blind/volunteer 都不被改")
        void update_reject_setsRejectedStatusOnly() {
            Long reviewId = 3L;
            FamilyJoinReview review = new FamilyJoinReview();
            review.setReviewId(reviewId);
            review.setFamilyId(70L);
            review.setBlindId(300L);

            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewId(reviewId);
            req.setReviewResult(false); // 拒绝
            req.setReviewerId(30L);

            when(familyJoinReviewMapper.selectById(eq(reviewId))).thenReturn(review);
            when(familyJoinReviewMapper.updateById(any(FamilyJoinReview.class))).thenReturn(1);

            boolean result = familyJoinReviewService.updateFamilyJoinReview(req, 30L, "13900000030");

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<FamilyJoinReview> captor =
                    org.mockito.ArgumentCaptor.forClass(FamilyJoinReview.class);
            verify(familyJoinReviewMapper).updateById(captor.capture());
            assertThat(captor.getValue().getReviewStatus())
                    .isEqualTo(FamilyReviewStatusEnum.REJECTED.getReviewStatus());
            // 拒绝时不碰 blind/volunteer
            verify(blindMapper, never()).selectById(any());
            verify(blindMapper, never()).updateById(any(Blind.class));
            verify(volunteerMapper, never()).selectById(any());
            verify(volunteerMapper, never()).updateById(any(Volunteer.class));
        }

        @Test
        @DisplayName("updateById 失败 → 抛 SYSTEM_ERROR")
        void update_reviewUpdateFails_throws() {
            FamilyJoinReview review = new FamilyJoinReview();
            review.setReviewId(4L);
            review.setFamilyId(80L);

            FamilyJoinReviewUpdateRequest req = new FamilyJoinReviewUpdateRequest();
            req.setReviewId(4L);
            req.setReviewResult(false);
            req.setReviewerId(40L);

            when(familyJoinReviewMapper.selectById(eq(4L))).thenReturn(review);
            when(familyJoinReviewMapper.updateById(any(FamilyJoinReview.class))).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyJoinReviewService.updateFamilyJoinReview(req, 40L, "13900000040"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        }
    }

    // ==================== getFamilyJoinReviewVOList ====================

    @Nested
    @DisplayName("getFamilyJoinReviewVOList：审核列表")
    class GetFamilyJoinReviewVOList {

        @Test
        @DisplayName("通过登录志愿者查询其家庭审核列表（VO 脱敏）")
        void list_returnsVoList() {
            Long volunteerId = 1L;
            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(volunteerId);
            volunteer.setFamilyId(500L);

            when(volunteerMapper.selectById(eq(volunteerId))).thenReturn(volunteer);

            FamilyJoinReview r1 = new FamilyJoinReview();
            r1.setReviewId(11L);
            r1.setFamilyId(500L);
            r1.setBlindId(20L);
            r1.setReviewStatus(FamilyReviewStatusEnum.WAIT_REVIEW.getReviewStatus());

            // list 走 baseMapper.selectList
            when(familyJoinReviewMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(java.util.Arrays.asList(r1));

            java.util.List<com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO> result =
                    familyJoinReviewService.getFamilyJoinReviewVOList(volunteerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReviewId()).isEqualTo(11L);
            assertThat(result.get(0).getReviewStatus())
                    .isEqualTo(FamilyReviewStatusEnum.WAIT_REVIEW.getName());
        }
    }

    // ==================== getFamilyJoinReviewVO（脱敏工具） ====================

    @Nested
    @DisplayName("getFamilyJoinReviewVO：脱敏")
    class GetFamilyJoinReviewVO {

        @Test
        @DisplayName("WAIT_REVIEW → 中文名「待审核」")
        void vo_waitReview() {
            FamilyJoinReview r = new FamilyJoinReview();
            r.setReviewId(1L);
            r.setFamilyId(2L);
            r.setReviewStatus(FamilyReviewStatusEnum.WAIT_REVIEW.getReviewStatus());

            com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO vo =
                    familyJoinReviewService.getFamilyJoinReviewVO(r);

            assertThat(vo.getReviewStatus()).isEqualTo(FamilyReviewStatusEnum.WAIT_REVIEW.getName());
        }

        @Test
        @DisplayName("PASSED → 中文名「已通过」")
        void vo_passed() {
            FamilyJoinReview r = new FamilyJoinReview();
            r.setReviewId(1L);
            r.setReviewStatus(FamilyReviewStatusEnum.PASSED.getReviewStatus());

            com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO vo =
                    familyJoinReviewService.getFamilyJoinReviewVO(r);

            assertThat(vo.getReviewStatus()).isEqualTo(FamilyReviewStatusEnum.PASSED.getName());
        }

        @Test
        @DisplayName("REJECTED → 中文名「已拒绝」")
        void vo_rejected() {
            FamilyJoinReview r = new FamilyJoinReview();
            r.setReviewId(1L);
            r.setReviewStatus(FamilyReviewStatusEnum.REJECTED.getReviewStatus());

            com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO vo =
                    familyJoinReviewService.getFamilyJoinReviewVO(r);

            assertThat(vo.getReviewStatus()).isEqualTo(FamilyReviewStatusEnum.REJECTED.getName());
        }
    }
}
