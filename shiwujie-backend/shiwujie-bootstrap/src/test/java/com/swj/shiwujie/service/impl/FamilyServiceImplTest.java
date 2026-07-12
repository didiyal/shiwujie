package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.FamilyMapper;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Family;
import com.swj.shiwujie.model.domain.user.FamilyJoinReview;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.family.FamilyRemoveUserRequest;
import com.swj.shiwujie.model.request.user.family.FamilyUpdateRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.FamilyJoinReviewService;
import com.swj.shiwujie.service.VolunteerService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FamilyServiceImpl} 单元测试——家庭域主要分支。
 * 纯 Mockito：@ExtendWith(MockitoExtension) + @Mock + @InjectMocks，不起 Spring/不连库。
 *
 * <p><b>注入字段类型说明：</b>
 * <ul>
 *   <li>{@code volunteerService}/{@code blindService}/{@code familyJoinReviewService} 都是 <b>顶层 Service 接口</b>
 *       （IService 子接口）的 mock。被测方法直接调它们的 {@code getById/list/updateById/save} 等方法时，
 *       直接对 mock 方法 stub 即可（mock 不执行 default 体，返默认值或 stub 值）。</li>
 *   <li>{@code familyService} 自身的 {@code getById/getOne/save/updateById/removeById} 走 baseMapper（{@code @Mock FamilyMapper}）。</li>
 * </ul>
 * MP 调用链：{@code getOne(qw)} → {@code baseMapper.selectOne(qw, true)} 两参（必须 stub 两参覆盖）；
 * {@code getById(id)} → {@code baseMapper.selectById(id)}；
 * {@code save(e)} → {@code baseMapper.insert(e)}（默认返 0=失败）；
 * {@code updateById(e)} → {@code baseMapper.updateById(e)}（默认返 0=失败）。
 */
@DisplayName("FamilyServiceImpl 家庭服务")
@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

    @Mock
    private FamilyMapper familyMapper;

    @Mock
    private VolunteerService volunteerService;

    @Mock
    private BlindService blindService;

    @Mock
    private FamilyJoinReviewService familyJoinReviewService;

    @InjectMocks
    private FamilyServiceImpl familyService;

    /**
     * 手动反射注入父类 baseMapper（同 BlindServiceImplTest 说明）。
     */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(familyService, "baseMapper", familyMapper);
    }

    // ==================== createFamily ====================

    @Nested
    @DisplayName("createFamily：创建家庭")
    class CreateFamily {

        @Test
        @DisplayName("已存在同 creator 的家庭 → 抛 PARAMS_ERROR")
        void create_alreadyExists_throws() {
            Long creatorId = 1L;
            Family existing = new Family();
            existing.setFamilyId(10L);
            existing.setCreatorVolunteerId(creatorId);

            // getOne → familyMapper.selectOne(qw, true) 两参
            when(familyMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.createFamily(creatorId, "13900000000"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(familyMapper, never()).insert(any(Family.class));
        }

        @Test
        @DisplayName("全新 creator → save 被调、家主 familyId 被更新")
        void create_new_savesFamilyAndUpdatesCreator() {
            Long creatorId = 2L;
            Volunteer creator = new Volunteer();
            creator.setVolunteerId(creatorId);

            // getOne 默认返 null（不显式 stub）
            when(volunteerService.getById(eq(creatorId))).thenReturn(creator);

            // save → baseMapper.insert 回填 familyId（用 thenAnswer 模拟 MP ASSIGN_ID 回填）
            when(familyMapper.insert(any(Family.class))).thenAnswer(invocation -> {
                Family f = invocation.getArgument(0);
                f.setFamilyId(777L); // 模拟回填
                return 1;
            });
            when(volunteerService.updateById(any(Volunteer.class))).thenReturn(true);

            // createFamily 末尾 getFamilyVO → 间接调 volunteerService.getVolunteerVOListByFamilyId(777)
            // 与 blindService.getBlindListByFamilyId(777)，给空 list 避免依赖
            when(volunteerService.getVolunteerVOListByFamilyId(eq(777L))).thenReturn(Collections.emptyList());
            when(blindService.getBlindListByFamilyId(eq(777L))).thenReturn(Collections.emptyList());

            familyService.createFamily(creatorId, "13900000001");

            verify(familyMapper, times(1)).insert(any(Family.class));
            verify(volunteerService, times(1)).updateById(any(Volunteer.class));
        }
    }

    // ==================== getFamilyVOById ====================

    @Nested
    @DisplayName("getFamilyVOById：获取家庭信息")
    class GetFamilyVOById {

        @Test
        @DisplayName("家庭不存在 → 抛 PARAMS_ERROR")
        void get_notExist_throws() {
            // getById → familyMapper.selectById 默认返 null（不显式 stub）
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.getFamilyVOById(99L, "13900000000"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }
    }

    // ==================== updateFamily ====================

    @Nested
    @DisplayName("updateFamily：更新家庭信息（仅家主可改）")
    class UpdateFamily {

        @Test
        @DisplayName("非家主 → 抛 PARAMS_ERROR")
        void update_notCreator_throws() {
            Family family = new Family();
            family.setFamilyId(1L);
            family.setCreatorVolunteerId(10L);

            FamilyUpdateRequest req = new FamilyUpdateRequest();
            req.setFamilyId(1L);
            req.setFamilyName("newName");

            when(familyMapper.selectById(eq(1L))).thenReturn(family);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.updateFamily(req, 999L)); // 非 10L 家主
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(familyMapper, never()).updateById(any(Family.class));
        }

        @Test
        @DisplayName("家主 + 改名 → updateById 被调")
        void update_byCreator_success() {
            Family family = new Family();
            family.setFamilyId(1L);
            family.setCreatorVolunteerId(10L);

            FamilyUpdateRequest req = new FamilyUpdateRequest();
            req.setFamilyId(1L);
            req.setFamilyName("happy family");
            req.setFamilyDescription("desc");

            when(familyMapper.selectById(eq(1L))).thenReturn(family);
            when(familyMapper.updateById(any(Family.class))).thenReturn(1);

            boolean result = familyService.updateFamily(req, 10L);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<Family> captor =
                    org.mockito.ArgumentCaptor.forClass(Family.class);
            verify(familyMapper).updateById(captor.capture());
            assertThat(captor.getValue().getFamilyName()).isEqualTo("happy family");
            assertThat(captor.getValue().getFamilyDescription()).isEqualTo("desc");
        }
    }

    // ==================== joinFamily ====================

    @Nested
    @DisplayName("joinFamily：申请加入家庭")
    class JoinFamily {

        @Test
        @DisplayName("familyVolunteerPhone 空 → 抛 PARAMS_ERROR")
        void join_emptyPhone_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.joinFamily(null, 1L, null, "13900000000"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("盲人加入 → 创建 Blind 申请记录（familyJoinReviewService.save 被调）")
        void join_blind_success() {
            Long familyId = 5L;
            Volunteer familyOwner = new Volunteer();
            familyOwner.setVolunteerId(20L);
            familyOwner.setFamilyId(familyId);

            when(volunteerService.getByPhone(eq("13900000001"))).thenReturn(familyOwner);
            when(familyJoinReviewService.save(any(FamilyJoinReview.class))).thenReturn(true);

            boolean result = familyService.joinFamily("13900000001", 100L, null, "13900000010");

            assertThat(result).isTrue();
            verify(familyJoinReviewService, times(1)).save(any(FamilyJoinReview.class));
        }

        @Test
        @DisplayName("家主尝试加入自己家庭 → 抛 PARAMS_ERROR")
        void join_volunteerOwnerSelfJoin_throws() {
            Long familyId = 6L;
            Volunteer familyOwner = new Volunteer();
            familyOwner.setVolunteerId(30L);
            familyOwner.setFamilyId(familyId);

            Family family = new Family();
            family.setFamilyId(familyId);
            family.setCreatorVolunteerId(30L);

            when(volunteerService.getByPhone(eq("13900000002"))).thenReturn(familyOwner);
            when(familyMapper.selectById(eq(familyId))).thenReturn(family);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.joinFamily("13900000002", null, 30L, "13900000030"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(familyJoinReviewService, never()).save(any(FamilyJoinReview.class));
        }

        @Test
        @DisplayName("志愿者加入非自家家庭 → 创建 Volunteer 申请记录")
        void join_volunteerOtherFamily_success() {
            Long familyId = 7L;
            Volunteer familyOwner = new Volunteer();
            familyOwner.setVolunteerId(40L);
            familyOwner.setFamilyId(familyId);

            Family family = new Family();
            family.setFamilyId(familyId);
            family.setCreatorVolunteerId(40L);

            when(volunteerService.getByPhone(eq("13900000003"))).thenReturn(familyOwner);
            when(familyMapper.selectById(eq(familyId))).thenReturn(family);
            when(familyJoinReviewService.save(any(FamilyJoinReview.class))).thenReturn(true);

            boolean result = familyService.joinFamily("13900000003", null, 50L, "13900000050");

            assertThat(result).isTrue();
            verify(familyJoinReviewService, times(1)).save(any(FamilyJoinReview.class));
        }
    }

    // ==================== removeUserFromFamily ====================

    @Nested
    @DisplayName("removeUserFromFamily：家主移除成员")
    class RemoveUserFromFamily {

        @Test
        @DisplayName("非家主操作 → 抛 PARAMS_ERROR")
        void remove_notCreator_throws() {
            Family family = new Family();
            family.setFamilyId(1L);
            family.setCreatorVolunteerId(10L);

            FamilyRemoveUserRequest req = new FamilyRemoveUserRequest();
            req.setFamilyId(1L);
            req.setBlindId(99L);

            when(familyMapper.selectById(eq(1L))).thenReturn(family);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> familyService.removeUserFromFamily(req, 888L, "13900000088"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("家主移除盲人成员 → blindService.updateById 被调")
        void remove_blindByCreator_success() {
            Family family = new Family();
            family.setFamilyId(1L);
            family.setCreatorVolunteerId(10L);

            Blind blind = new Blind();
            blind.setBlindId(50L);

            FamilyRemoveUserRequest req = new FamilyRemoveUserRequest();
            req.setFamilyId(1L);
            req.setBlindId(50L);

            when(familyMapper.selectById(eq(1L))).thenReturn(family);
            when(blindService.getById(eq(50L))).thenReturn(blind);
            when(blindService.updateById(any(Blind.class))).thenReturn(true);

            Boolean result = familyService.removeUserFromFamily(req, 10L, "13900000010");

            assertThat(result).isTrue();
            verify(blindService, times(1)).updateById(any(Blind.class));
        }

        @Test
        @DisplayName("家主移除志愿者成员 → volunteerService.updateById 被调")
        void remove_volunteerByCreator_success() {
            Family family = new Family();
            family.setFamilyId(1L);
            family.setCreatorVolunteerId(10L);

            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(60L);

            FamilyRemoveUserRequest req = new FamilyRemoveUserRequest();
            req.setFamilyId(1L);
            req.setVolunteerId(60L);

            when(familyMapper.selectById(eq(1L))).thenReturn(family);
            when(volunteerService.getById(eq(60L))).thenReturn(volunteer);
            when(volunteerService.updateById(any(Volunteer.class))).thenReturn(true);

            Boolean result = familyService.removeUserFromFamily(req, 10L, "13900000010");

            assertThat(result).isTrue();
            verify(volunteerService, times(1)).updateById(any(Volunteer.class));
        }
    }

    // ==================== userLeaveFromFamily ====================

    @Nested
    @DisplayName("userLeaveFromFamily：用户主动退出")
    class UserLeaveFromFamily {

        @Test
        @DisplayName("盲人退出 → blindService.updateById 被调")
        void leave_blind_success() {
            Blind blind = new Blind();
            blind.setBlindId(1L);

            when(blindService.getById(eq(1L))).thenReturn(blind);
            when(blindService.updateById(any(Blind.class))).thenReturn(true);

            boolean result = familyService.userLeaveFromFamily(1L, null, "13900000001");

            assertThat(result).isTrue();
            verify(blindService, times(1)).updateById(any(Blind.class));
        }

        @Test
        @DisplayName("志愿者退出 → volunteerService.updateById 被调")
        void leave_volunteer_success() {
            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(2L);

            when(volunteerService.getById(eq(2L))).thenReturn(volunteer);
            when(volunteerService.updateById(any(Volunteer.class))).thenReturn(true);

            boolean result = familyService.userLeaveFromFamily(null, 2L, "13900000002");

            assertThat(result).isTrue();
            verify(volunteerService, times(1)).updateById(any(Volunteer.class));
        }
    }

    // ==================== getFamilyVO（null 入参） ====================

    @Nested
    @DisplayName("getFamilyVO：脱敏")
    class GetFamilyVO {

        @Test
        @DisplayName("family=null → 返回 null")
        void getFamilyVO_null_returnsNull() {
            assertThat(familyService.getFamilyVO(null)).isNull();
        }
    }
}
