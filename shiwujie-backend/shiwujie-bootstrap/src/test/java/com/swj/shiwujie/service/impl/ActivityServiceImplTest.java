package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.ActivityMapper;
import com.swj.shiwujie.model.domain.community.Activity;
import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import com.swj.shiwujie.model.request.community.activity.ActivityAddRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityUpdateRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link ActivityServiceImpl} 单元测试——活动增改查删主流程。
 *
 * <p>注：当前实现未做权限（known-issue），按现状逻辑测分支：参数校验、不存在的兜底、
 * 字段选择性更新、状态枚举转换、分页查询。
 */
@DisplayName("ActivityServiceImpl 活动主流程")
@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    private static final Long ACTIVITY_ID = 500L;
    private static final Long COMMUNITY_ID = 100L;
    private static final Long LOGIN_VOLUNTEER_ID = 10L;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    @SuppressWarnings("unused") // 源码注入但当前未在主流程调用，仅占位
    private CommunitymanagerService communitymanagerService;

    @InjectMocks
    private ActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", activityMapper);
    }

    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    // ---------- addActivity ----------
    @Nested
    @DisplayName("addActivity 创建活动")
    class Add {

        @Test
        @DisplayName("请求体 null → PARAMS_ERROR")
        void nullRequest() {
            expectCode(() -> service.addActivity(null, LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("save 失败 → SYSTEM_ERROR")
        void saveFails() {
            ActivityAddRequest req = new ActivityAddRequest();
            req.setActivityName("活动");
            req.setCommunityId(COMMUNITY_ID);
            when(activityMapper.insert(any(Activity.class))).thenReturn(0);

            expectCode(() -> service.addActivity(req, LOGIN_VOLUNTEER_ID), ErrorCode.SYSTEM_ERROR);
        }

        @Test
        @DisplayName("成功 → managerId 设为登录志愿者，初始状态=未开始")
        void success() {
            ActivityAddRequest req = new ActivityAddRequest();
            req.setActivityName("助盲出行");
            req.setCommunityId(COMMUNITY_ID);
            when(activityMapper.insert(any(Activity.class))).thenReturn(1);

            var vo = service.addActivity(req, LOGIN_VOLUNTEER_ID);

            assertThat(vo).isNotNull();
            // ActivityVO.setActivityStatus(Integer) 把 0 转为状态名 "未开始"
            assertThat(vo.getActivityStatus()).isEqualTo(ActivityStatusEnum.WAITING.getName());
            // 校验传给 insert 的 Activity 关键字段（实体上仍是 Integer code）。
            // MP 3.5.9 BaseMapper 有 insert(T) 与 insert(Collection<T>) 两重载，裸 argThat(...) 因泛型推断
            // 触发 "insert(Activity) is ambiguous" 编译错误；改用强类型 ArgumentCaptor 消歧。
            org.mockito.ArgumentCaptor<Activity> captor = org.mockito.ArgumentCaptor.forClass(Activity.class);
            verify(activityMapper).insert(captor.capture());
            Activity saved = captor.getValue();
            assertThat(saved.getManagerId()).isEqualTo(LOGIN_VOLUNTEER_ID);
            assertThat(saved.getActivityStatus()).isEqualTo(ActivityStatusEnum.WAITING.getPostStatus());
            assertThat(saved.getCreateTime()).isNotNull();
        }
    }

    // ---------- getActivityVOById ----------
    @Nested
    @DisplayName("getActivityVOById 查询")
    class GetById {

        @Test
        @DisplayName("ID 非法 → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.getActivityVOById(0L), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(activityMapper);
        }

        @Test
        @DisplayName("活动不存在 → SYSTEM_ERROR")
        void absent() {
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(null);
            expectCode(() -> service.getActivityVOById(ACTIVITY_ID), ErrorCode.SYSTEM_ERROR);
        }

        @Test
        @DisplayName("命中 → 返 VO")
        void found() {
            Activity a = activityOf(ACTIVITY_ID);
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(a);
            var vo = service.getActivityVOById(ACTIVITY_ID);
            assertThat(vo.getActivityId()).isEqualTo(ACTIVITY_ID);
        }
    }

    // ---------- listActivitiesByCommunity ----------
    @Nested
    @DisplayName("listActivitiesByCommunity 分页查询")
    class ListByCommunity {

        @Test
        @DisplayName("社区 ID 非法 → PARAMS_ERROR")
        void invalidCommunityId() {
            ActivityQueryRequest req = new ActivityQueryRequest();
            req.setCommunityId(0L);
            expectCode(() -> service.listActivitiesByCommunity(req), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(activityMapper);
        }

        @Test
        @DisplayName("非法 activityStatus 字符串 → PARAMS_ERROR")
        void invalidStatus() {
            ActivityQueryRequest req = new ActivityQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setActivityStatus("不存在的状态");
            // 状态校验发生在分页查询之前，selectPage 不会被调用，故不 stub（避免 UnnecessaryStubbing）。

            expectCode(() -> service.listActivitiesByCommunity(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("合法状态查询 → 返 VO 列表")
        void validQuery() {
            ActivityQueryRequest req = new ActivityQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setActivityStatus("未开始");

            Page<Activity> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(activityOf(ACTIVITY_ID)));
            when(activityMapper.selectPage(any(Page.class), any())).thenReturn(page);

            var result = service.listActivitiesByCommunity(req);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getActivityId()).isEqualTo(ACTIVITY_ID);
        }
    }

    // ---------- deleteActivity ----------
    @Nested
    @DisplayName("deleteActivity 删除")
    class Delete {

        @Test
        @DisplayName("ID 非法 → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.deleteActivity(-1L), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("活动不存在 → SYSTEM_ERROR")
        void absent() {
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(null);
            expectCode(() -> service.deleteActivity(ACTIVITY_ID), ErrorCode.SYSTEM_ERROR);
        }

        @Test
        @DisplayName("命中 → 调 removeById（deleteById）")
        void deleted() {
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(activityOf(ACTIVITY_ID));
            // removeById(id) → baseMapper.deleteById(Serializable id)；MP 3.5.9 有 deleteById 三重载
            // （Serializable/Object+boolean/T），裸 any() 编译期歧义且 strict 下参数不匹配 → 用 anyLong() 锁定 Serializable 重载。
            when(activityMapper.deleteById(anyLong())).thenReturn(1);

            boolean ok = service.deleteActivity(ACTIVITY_ID);

            assertThat(ok).isTrue();
            verify(activityMapper).deleteById(anyLong());
        }
    }

    // ---------- updateActivity ----------
    @Nested
    @DisplayName("updateActivity 更新")
    class Update {

        @Test
        @DisplayName("activityId 为 null → PARAMS_ERROR")
        void nullId() {
            ActivityUpdateRequest req = new ActivityUpdateRequest();
            req.setActivityId(null);
            expectCode(() -> service.updateActivity(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("活动不存在 → SYSTEM_ERROR")
        void absent() {
            ActivityUpdateRequest req = updateReq();
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(null);
            expectCode(() -> service.updateActivity(req), ErrorCode.SYSTEM_ERROR);
        }

        @Test
        @DisplayName("选择性更新字段 + 状态转换 → 通过")
        void selectiveUpdate() {
            ActivityUpdateRequest req = updateReq();
            req.setActivityName("新名字");
            req.setActivityContent("新内容");
            req.setMaxParticipants(50L);
            req.setStartTime(new Date());
            req.setActivityStatus("进行中");

            Activity activity = activityOf(ACTIVITY_ID);
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(activity);
            when(activityMapper.updateById(any(Activity.class))).thenReturn(1);

            boolean ok = service.updateActivity(req);

            assertThat(ok).isTrue();
            assertThat(activity.getActivityName()).isEqualTo("新名字");
            assertThat(activity.getActivityContent()).isEqualTo("新内容");
            assertThat(activity.getMaxParticipants()).isEqualTo(50L);
            assertThat(activity.getActivityStatus()).isEqualTo(ActivityStatusEnum.DOING.getPostStatus());
        }

        @Test
        @DisplayName("非法状态 → PARAMS_ERROR")
        void invalidStatus() {
            ActivityUpdateRequest req = updateReq();
            req.setActivityStatus("不存在");
            Activity activity = activityOf(ACTIVITY_ID);
            when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(activity);

            expectCode(() -> service.updateActivity(req), ErrorCode.PARAMS_ERROR);
            verify(activityMapper, never()).updateById(any(Activity.class));
        }
    }

    // ---------- 辅助 ----------
    private Activity activityOf(Long id) {
        Activity a = new Activity();
        a.setActivityId(id);
        a.setCommunityId(COMMUNITY_ID);
        a.setActivityStatus(ActivityStatusEnum.WAITING.getPostStatus());
        return a;
    }

    private ActivityUpdateRequest updateReq() {
        ActivityUpdateRequest req = new ActivityUpdateRequest();
        req.setActivityId(ACTIVITY_ID);
        return req;
    }
}
