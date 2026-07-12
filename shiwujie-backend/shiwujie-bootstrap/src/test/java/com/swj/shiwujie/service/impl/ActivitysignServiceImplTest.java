package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.ActivitysignMapper;
import com.swj.shiwujie.model.domain.community.Activity;
import com.swj.shiwujie.model.domain.community.Activitysign;
import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.service.ActivityService;
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
 * {@link ActivitysignServiceImpl} 单元测试——活动报名主流程。
 *
 * <p>当前实现按源码现状测：参数二选一校验、活动不存在兜底、count==1 已报名分支、
 * 正常保存、查询与详情。未做权限（known-issue），按现状覆盖业务分支即可。
 */
@DisplayName("ActivitysignServiceImpl 活动报名")
@ExtendWith(MockitoExtension.class)
class ActivitysignServiceImplTest {

    private static final Long SIGN_ID = 700L;
    private static final Long ACTIVITY_ID = 500L;
    private static final Long BLIND_ID = 30L;
    private static final Long VOLUNTEER_ID = 40L;

    @Mock
    private ActivitysignMapper activitysignMapper;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivitysignServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", activitysignMapper);
    }

    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    // ---------- addActivitySign ----------
    @Nested
    @DisplayName("addActivitySign 报名")
    class AddSign {

        @Test
        @DisplayName("请求体 null → PARAMS_ERROR")
        void nullRequest() {
            expectCode(() -> service.addActivitySign(null), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("活动 ID 非法 → PARAMS_ERROR")
        void invalidActivityId() {
            ActivitySignAddRequest req = new ActivitySignAddRequest();
            req.setActivityId(0L);
            req.setBlindId(BLIND_ID);
            expectCode(() -> service.addActivitySign(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("盲人与志愿者 ID 都未提供 → PARAMS_ERROR")
        void neitherIdProvided() {
            ActivitySignAddRequest req = new ActivitySignAddRequest();
            req.setActivityId(ACTIVITY_ID);
            // blindId 与 volunteerId 都为 null
            expectCode(() -> service.addActivitySign(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("盲人 ID <= 0 且志愿者 ID <= 0 → PARAMS_ERROR（二选一不成立）")
        void bothIdsNonPositive() {
            ActivitySignAddRequest req = new ActivitySignAddRequest();
            req.setActivityId(ACTIVITY_ID);
            req.setBlindId(0L);
            req.setVolunteerId(-1L);
            expectCode(() -> service.addActivitySign(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("活动不存在 → PARAMS_ERROR")
        void activityAbsent() {
            ActivitySignAddRequest req = signReq();
            when(activityService.getById(ACTIVITY_ID)).thenReturn(null);
            expectCode(() -> service.addActivitySign(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("已报名（count==1） → SYSTEM_ERROR 活动已报名")
        void alreadySigned() {
            ActivitySignAddRequest req = signReq();
            when(activityService.getById(ACTIVITY_ID)).thenReturn(activity());
            when(activitysignMapper.selectCount(any())).thenReturn(1L);

            expectCode(() -> service.addActivitySign(req), ErrorCode.SYSTEM_ERROR);
            verify(activitysignMapper, never()).insert(any(Activitysign.class));
        }

        @Test
        @DisplayName("未报名（count==0） → save 触发")
        void success() {
            ActivitySignAddRequest req = signReq();
            when(activityService.getById(ACTIVITY_ID)).thenReturn(activity());
            when(activitysignMapper.selectCount(any())).thenReturn(0L);
            when(activitysignMapper.insert(any(Activitysign.class))).thenReturn(1);

            boolean ok = service.addActivitySign(req);

            assertThat(ok).isTrue();
            // MP 3.5.9 BaseMapper 有 insert(T) 与 insert(Collection<T>) 两重载，裸 argThat(...) 因泛型推断
            // 触发 "insert(Activitysign) is ambiguous" 编译错误；改用强类型 ArgumentCaptor 消歧。
            org.mockito.ArgumentCaptor<Activitysign> captor = org.mockito.ArgumentCaptor.forClass(Activitysign.class);
            verify(activitysignMapper).insert(captor.capture());
            Activitysign saved = captor.getValue();
            assertThat(saved.getActivityId()).isEqualTo(ACTIVITY_ID);
            assertThat(saved.getBlindId()).isEqualTo(BLIND_ID);
            assertThat(saved.getSignUpTime()).isNotNull();
        }
    }

    // ---------- getActivitySignVOById ----------
    @Nested
    @DisplayName("getActivitySignVOById 查询")
    class GetById {

        @Test
        @DisplayName("ID 非法 → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.getActivitySignVOById(0L), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("记录不存在 → SYSTEM_ERROR")
        void absent() {
            when(activitysignMapper.selectById(SIGN_ID)).thenReturn(null);
            expectCode(() -> service.getActivitySignVOById(SIGN_ID), ErrorCode.SYSTEM_ERROR);
        }

        @Test
        @DisplayName("命中 → 返 VO")
        void found() {
            Activitysign s = signOf(SIGN_ID);
            when(activitysignMapper.selectById(SIGN_ID)).thenReturn(s);
            var vo = service.getActivitySignVOById(SIGN_ID);
            assertThat(vo.getSignId()).isEqualTo(SIGN_ID);
        }
    }

    // ---------- listActivitySignByActivity ----------
    @Nested
    @DisplayName("listActivitySignByActivity 分页查询")
    class ListByActivity {

        @Test
        @DisplayName("请求体 null → PARAMS_ERROR")
        void nullRequest() {
            expectCode(() -> service.listActivitySignByActivity(null), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("命中记录 → 返 VO 列表")
        void list() {
            ActivitySignQueryRequest req = new ActivitySignQueryRequest();
            req.setActivityId(ACTIVITY_ID);
            req.setCurrent(1);
            req.setPageSize(20);

            Page<Activitysign> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(signOf(SIGN_ID)));
            when(activitysignMapper.selectPage(any(Page.class), any())).thenReturn(page);

            var result = service.listActivitySignByActivity(req);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getSignId()).isEqualTo(SIGN_ID);
        }

        @Test
        @DisplayName("空结果 → 返空列表")
        void empty() {
            ActivitySignQueryRequest req = new ActivitySignQueryRequest();
            req.setActivityId(ACTIVITY_ID);
            req.setCurrent(1);
            req.setPageSize(20);

            Page<Activitysign> empty = new Page<>(1, 20, 0);
            empty.setRecords(Collections.emptyList());
            when(activitysignMapper.selectPage(any(Page.class), any())).thenReturn(empty);

            var result = service.listActivitySignByActivity(req);
            assertThat(result.getRecords()).isEmpty();
        }
    }

    // ---------- 辅助 ----------
    private ActivitySignAddRequest signReq() {
        ActivitySignAddRequest req = new ActivitySignAddRequest();
        req.setActivityId(ACTIVITY_ID);
        req.setBlindId(BLIND_ID);
        return req;
    }

    private Activity activity() {
        Activity a = new Activity();
        a.setActivityId(ACTIVITY_ID);
        a.setActivityStatus(ActivityStatusEnum.WAITING.getPostStatus());
        return a;
    }

    private Activitysign signOf(Long id) {
        Activitysign s = new Activitysign();
        s.setSignId(id);
        s.setActivityId(ACTIVITY_ID);
        s.setBlindId(BLIND_ID);
        return s;
    }
}
