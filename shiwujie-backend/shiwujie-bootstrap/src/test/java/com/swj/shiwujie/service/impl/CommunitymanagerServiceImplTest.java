package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunitymanagerMapper;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityEmployeeQueryRequest;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityManagerRequest;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link CommunitymanagerServiceImpl} 单元测试——社区成员权限治理核心。
 *
 * <p>纯 Mockito，不起 Spring；ServiceImpl 基类 baseMapper 由 {@code @Mock CommunitymanagerMapper}
 * 经 {@code @InjectMocks} 按类型注入覆盖。重点覆盖安全加固后的权限分支：
 * <ul>
 *   <li>操作人=注册人/管理员可通过；操作人=员工或非本社区成员拒 NO_AUTH。</li>
 *   <li>注册人不可后增；末位注册人不可降级/删除。</li>
 * </ul>
 */
@DisplayName("CommunitymanagerServiceImpl 权限治理")
@ExtendWith(MockitoExtension.class)
class CommunitymanagerServiceImplTest {

    private static final Long COMMUNITY_ID = 100L;
    private static final Long LOGIN_VOLUNTEER_ID = 10L;   // 操作人（ADMIN/REGISTRANT）
    private static final Long EMPLOYEE_ID = 11L;          // 操作人（EMPLOYEE）
    private static final Long TARGET_VOLUNTEER_ID = 20L;  // 被操作目标

    @Mock
    private CommunitymanagerMapper communitymanagerMapper;

    @Mock
    private CommunityMapper communityMapper;

    @Mock
    private InnerVolunteerService innerVolunteerService;

    @InjectMocks
    private CommunitymanagerServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", communitymanagerMapper);
    }

    // ---------- 辅助构造 ----------

    /** 构造操作人角色记录（assertCommunityAdmin 内部 getByVolunteerIdAndCommunityId 的返回）。 */
    private Communitymanager loginRole(long roleId) {
        Communitymanager m = new Communitymanager();
        m.setCommunityId(COMMUNITY_ID);
        m.setVolunteerId(LOGIN_VOLUNTEER_ID);
        m.setRolePermissionId(roleId);
        return m;
    }

    private CommunityManagerRequest addRequest(String roleName) {
        CommunityManagerRequest req = new CommunityManagerRequest();
        req.setCommunityId(COMMUNITY_ID);
        req.setVolunteerId(TARGET_VOLUNTEER_ID);
        req.setRoleName(roleName);
        return req;
    }

    /** 断言 BusinessException 的 code 与预期 ErrorCode 一致。 */
    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    // =========================================================
    // queryCommunityEmployees
    // =========================================================
    @Nested
    @DisplayName("queryCommunityEmployees 分页查询")
    class QueryEmployees {

        @Test
        @DisplayName("社区不存在 → PARAMS_ERROR")
        void communityAbsent() {
            CommunityEmployeeQueryRequest req = new CommunityEmployeeQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setCurrent(1);
            req.setPageSize(10);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(null);

            expectCode(() -> service.queryCommunityEmployees(req), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("分页参数越界（size>100）→ PARAMS_ERROR")
        void badPageSize() {
            CommunityEmployeeQueryRequest req = new CommunityEmployeeQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setCurrent(1);
            req.setPageSize(200);

            expectCode(() -> service.queryCommunityEmployees(req), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(communityMapper);
        }

        @Test
        @DisplayName("空记录 → 返空列表，不调 innerVolunteerService")
        void emptyRecords() {
            CommunityEmployeeQueryRequest req = new CommunityEmployeeQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setCurrent(1);
            req.setPageSize(10);

            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);

            Page<Communitymanager> emptyPage = new Page<>(1, 10, 0);
            emptyPage.setRecords(Collections.emptyList());
            when(communitymanagerMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

            Page<VolunteerVO> result = service.queryCommunityEmployees(req);

            assertThat(result.getRecords()).isEmpty();
            verifyNoInteractions(innerVolunteerService);
        }

        @Test
        @DisplayName("命中记录 → 逐个查 Volunteer 并转 VO；Volunteer 为 null 时跳过")
        void mapsRecordsAndSkipsNullVolunteer() {
            CommunityEmployeeQueryRequest req = new CommunityEmployeeQueryRequest();
            req.setCommunityId(COMMUNITY_ID);
            req.setCurrent(1);
            req.setPageSize(10);

            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);

            Communitymanager m1 = new Communitymanager();
            m1.setVolunteerId(21L);
            Communitymanager m2 = new Communitymanager();
            m2.setVolunteerId(22L);
            Page<Communitymanager> page = new Page<>(1, 10, 2);
            page.setRecords(List.of(m1, m2));
            when(communitymanagerMapper.selectPage(any(Page.class), any())).thenReturn(page);

            Volunteer v1 = new Volunteer();
            v1.setVolunteerId(21L);
            when(innerVolunteerService.getById(21L)).thenReturn(v1);
            when(innerVolunteerService.getById(22L)).thenReturn(null); // 跳过
            VolunteerVO vo1 = new VolunteerVO();
            when(innerVolunteerService.getVolunteerVO(v1)).thenReturn(vo1);

            Page<VolunteerVO> result = service.queryCommunityEmployees(req);

            assertThat(result.getRecords()).containsExactly(vo1);
        }
    }

    // =========================================================
    // addCommunityManager —— 权限核心
    // =========================================================
    @Nested
    @DisplayName("addCommunityManager 新增成员")
    class AddCommunityManager {

        @Test
        @DisplayName("操作人=ADMIN → 通过，新增员工成功")
        void adminCanAdd() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            // addCommunityManager 内部两次走 communitymanagerMapper.selectOne：
            //   第 1 次（assertCommunityAdmin 权限校验）→ ADMIN 角色；
            //   第 2 次（getByVolunteerIdAndCommunityId 目标查重）→ null（目标未存在）。
            // 原代码分两条 when(...)，第二条覆盖第一条致第 1 次调用也得 null→NO_AUTH；合并为单条链式 thenReturn。
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId()))
                    .thenReturn(null);
            Volunteer target = new Volunteer();
            target.setVolunteerId(TARGET_VOLUNTEER_ID);
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(target);
            when(communitymanagerMapper.insert(any(Communitymanager.class))).thenReturn(1);

            boolean ok = service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID);

            assertThat(ok).isTrue();
            // 校验写入的 roleId 是 EMPLOYEE。
            // MP 3.5.9 BaseMapper 有 insert(T) 与 insert(Collection<T>) 两重载，裸 argThat(...) 因泛型推断
            // 触发 "insert(Communitymanager) is ambiguous" 编译错误；改用强类型 ArgumentCaptor 消歧。
            org.mockito.ArgumentCaptor<Communitymanager> captor =
                    org.mockito.ArgumentCaptor.forClass(Communitymanager.class);
            verify(communitymanagerMapper).insert(captor.capture());
            Communitymanager saved = captor.getValue();
            assertThat(saved.getRolePermissionId()).isEqualTo(CommunityRolePermissionEnum.EMPLOYEE.getRoleId());
            assertThat(saved.getCommunityId()).isEqualTo(COMMUNITY_ID);
            assertThat(saved.getVolunteerId()).isEqualTo(TARGET_VOLUNTEER_ID);
        }

        @Test
        @DisplayName("操作人=REGISTRANT → 通过，新增管理员成功")
        void registrantCanAdd() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.REGISTRANT.getRoleId())) // 权限
                    .thenReturn(null); // 目标未存在
            Volunteer target = new Volunteer();
            target.setVolunteerId(TARGET_VOLUNTEER_ID);
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(target);
            when(communitymanagerMapper.insert(any(Communitymanager.class))).thenReturn(1);

            boolean ok = service.addCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID);

            assertThat(ok).isTrue();
        }

        @Test
        @DisplayName("操作人=EMPLOYEE → NO_AUTH（员工无权管理成员）")
        void employeeCannotAdd() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));

            expectCode(() -> service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(innerVolunteerService, never()).getById(anyLong());
            verify(communitymanagerMapper, never()).insert(any(Communitymanager.class));
        }

        @Test
        @DisplayName("操作人不在该社区（selectOne 返 null） → NO_AUTH")
        void nonMemberCannotAdd() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any())).thenReturn(null);

            expectCode(() -> service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("社区不存在 → PARAMS_ERROR")
        void communityAbsent() {
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(null);

            expectCode(() -> service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("请求 roleName=注册人 → NO_AUTH（注册人不可新增）")
        void cannotAddRegistrant() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())); // 权限通过
            Volunteer target = new Volunteer();
            target.setVolunteerId(TARGET_VOLUNTEER_ID);
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(target);

            expectCode(() -> service.addCommunityManager(addRequest("注册人"), LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(communitymanagerMapper, never()).insert(any(Communitymanager.class));
        }

        @Test
        @DisplayName("目标志愿者已是成员 → OPERATION_ERROR")
        void targetAlreadyMember() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(new Communitymanager()); // 目标已存在
            Volunteer target = new Volunteer();
            target.setVolunteerId(TARGET_VOLUNTEER_ID);
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(target);

            expectCode(() -> service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID), ErrorCode.OPERATION_ERROR);
        }

        @Test
        @DisplayName("目标志愿者不存在 → PARAMS_ERROR")
        void targetVolunteerAbsent() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId()));
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(null);

            expectCode(() -> service.addCommunityManager(addRequest("员工"), LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("roleName 非法字符串 → PARAMS_ERROR")
        void invalidRoleName() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId()));
            Volunteer target = new Volunteer();
            target.setVolunteerId(TARGET_VOLUNTEER_ID);
            when(innerVolunteerService.getById(TARGET_VOLUNTEER_ID)).thenReturn(target);

            expectCode(() -> service.addCommunityManager(addRequest("不存在的角色"), LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }
    }

    // =========================================================
    // updateCommunityManager —— 末位注册人护栏
    // =========================================================
    @Nested
    @DisplayName("updateCommunityManager 修改成员角色")
    class UpdateCommunityManager {

        @Test
        @DisplayName("ADMIN 改普通管理员 → 通过")
        void adminCanUpdate() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(managerOf(TARGET_VOLUNTEER_ID, CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));
            when(communitymanagerMapper.updateById(any(Communitymanager.class))).thenReturn(1);

            boolean ok = service.updateCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID);

            assertThat(ok).isTrue();
            // MP 3.5.9 BaseMapper 有 updateById(T) 与 updateById(Collection<T>) 两重载，裸 argThat(...) 因泛型推断
            // 触发 "updateById(Communitymanager) is ambiguous" 编译错误；改用强类型 ArgumentCaptor 消歧。
            org.mockito.ArgumentCaptor<Communitymanager> captor =
                    org.mockito.ArgumentCaptor.forClass(Communitymanager.class);
            verify(communitymanagerMapper).updateById(captor.capture());
            assertThat(captor.getValue().getRolePermissionId())
                    .isEqualTo(CommunityRolePermissionEnum.ADMIN.getRoleId());
        }

        @Test
        @DisplayName("EMPLOYEE 操作 → NO_AUTH")
        void employeeCannotUpdate() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));

            expectCode(() -> service.updateCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("把唯一注册人降级（count<=1） → NO_AUTH 不可降级末位注册人")
        void cannotDegradeLastRegistrant() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(managerOf(TARGET_VOLUNTEER_ID, CommunityRolePermissionEnum.REGISTRANT.getRoleId()));
            // countByCommunityIdAndRole 返 1
            when(communitymanagerMapper.selectCount(any())).thenReturn(1L);

            expectCode(() -> service.updateCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(communitymanagerMapper, never()).updateById(any(Communitymanager.class));
        }

        @Test
        @DisplayName("多个注册人时降级其中一个（count>1） → 通过")
        void canDegradeWhenMultipleRegistrants() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId()))
                    .thenReturn(managerOf(TARGET_VOLUNTEER_ID, CommunityRolePermissionEnum.REGISTRANT.getRoleId()));
            when(communitymanagerMapper.selectCount(any())).thenReturn(2L);
            when(communitymanagerMapper.updateById(any(Communitymanager.class))).thenReturn(1);

            boolean ok = service.updateCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID);

            assertThat(ok).isTrue();
        }

        @Test
        @DisplayName("管理记录不存在 → PARAMS_ERROR")
        void recordAbsent() {
            Community community = new Community();
            community.setCommunityId(COMMUNITY_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId()))
                    .thenReturn(null);

            expectCode(() -> service.updateCommunityManager(addRequest("管理员"), LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }
    }

    // =========================================================
    // deleteCommunityManager
    // =========================================================
    @Nested
    @DisplayName("deleteCommunityManager 删除成员")
    class DeleteCommunityManager {

        @Test
        @DisplayName("ADMIN 删普通员工 → 通过")
        void adminCanDelete() {
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(managerOf(TARGET_VOLUNTEER_ID, CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));
            when(communitymanagerMapper.delete(any())).thenReturn(1);

            boolean ok = service.deleteCommunityManager(TARGET_VOLUNTEER_ID, COMMUNITY_ID, LOGIN_VOLUNTEER_ID);

            assertThat(ok).isTrue();
        }

        @Test
        @DisplayName("EMPLOYEE 删 → NO_AUTH")
        void employeeCannotDelete() {
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));

            expectCode(() -> service.deleteCommunityManager(TARGET_VOLUNTEER_ID, COMMUNITY_ID, LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(communitymanagerMapper, never()).delete(any(Wrapper.class));
        }

        @Test
        @DisplayName("删唯一注册人（count<=1） → NO_AUTH 不可删除末位注册人")
        void cannotDeleteLastRegistrant() {
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(managerOf(TARGET_VOLUNTEER_ID, CommunityRolePermissionEnum.REGISTRANT.getRoleId()));
            when(communitymanagerMapper.selectCount(any())).thenReturn(1L);

            expectCode(() -> service.deleteCommunityManager(TARGET_VOLUNTEER_ID, COMMUNITY_ID, LOGIN_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(communitymanagerMapper, never()).delete(any(Wrapper.class));
        }

        @Test
        @DisplayName("目标记录不存在 → PARAMS_ERROR")
        void targetRecordAbsent() {
            when(communitymanagerMapper.selectOne(any()))
                    .thenReturn(loginRole(CommunityRolePermissionEnum.ADMIN.getRoleId())) // 权限
                    .thenReturn(null); // 目标查不到

            expectCode(() -> service.deleteCommunityManager(TARGET_VOLUNTEER_ID, COMMUNITY_ID, LOGIN_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }
    }

    // =========================================================
    // 辅助方法
    // =========================================================
    @Nested
    @DisplayName("辅助查询方法")
    class Helpers {

        @Test
        @DisplayName("getCountByVolunteerIdAndCommunityId → selectCount")
        void getCount() {
            when(communitymanagerMapper.selectCount(any())).thenReturn(3L);
            long c = service.getCountByVolunteerIdAndCommunityId(1L, 2L);
            assertThat(c).isEqualTo(3L);
        }

        @Test
        @DisplayName("getByVolunteerIdAndCommunityId → selectOne")
        void getBy() {
            Communitymanager m = new Communitymanager();
            when(communitymanagerMapper.selectOne(any())).thenReturn(m);
            assertThat(service.getByVolunteerIdAndCommunityId(1L, 2L)).isSameAs(m);
        }

        @Test
        @DisplayName("removeByVolunteerIdAndCommunityId → delete 返影响行数")
        void removeBy() {
            when(communitymanagerMapper.delete(any(Wrapper.class))).thenReturn(5);
            assertThat(service.removeByVolunteerIdAndCommunityId(1L, 2L)).isEqualTo(5);
        }

        @Test
        @DisplayName("removeByCommunityId → delete 返影响行数")
        void removeByCommunity() {
            when(communitymanagerMapper.delete(any(Wrapper.class))).thenReturn(7);
            assertThat(service.removeByCommunityId(1L)).isEqualTo(7);
        }
    }

    private Communitymanager managerOf(Long volunteerId, long roleId) {
        Communitymanager m = new Communitymanager();
        m.setCommunityId(COMMUNITY_ID);
        m.setVolunteerId(volunteerId);
        m.setRolePermissionId(roleId);
        return m;
    }
}
