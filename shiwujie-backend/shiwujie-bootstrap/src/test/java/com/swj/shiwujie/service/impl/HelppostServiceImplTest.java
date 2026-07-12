package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.HelppostMapper;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.community.Helppost;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.enums.community.PostStatusEnum;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.user.InnerBlindService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link HelppostServiceImpl} 单元测试——求助帖权限治理。
 *
 * <p>权限模型（deleteHelppost / updateHelppost）：作者（视障人士本人）或本社区 注册人/管理员 可操作；
 * 社区员工（EMPLOYEE）与非作者非管理员拒 NO_AUTH。addHelppost 校验视障人士已加入社区。
 *
 * <p>ServiceImpl 基类 {@code this.save/getById/removeById/updateById} 走 baseMapper，
 * 由 {@code @Mock HelppostMapper} 经 {@code @InjectMocks} 注入。
 */
@DisplayName("HelppostServiceImpl 求助帖权限")
@ExtendWith(MockitoExtension.class)
class HelppostServiceImplTest {

    private static final Long COMMUNITY_ID = 100L;
    private static final Long BLIND_ID = 30L;
    private static final Long OTHER_BLIND_ID = 31L;
    private static final Long VOLUNTEER_ID = 40L;
    private static final Long HELPPOST_ID = 200L;

    @Mock
    private HelppostMapper helppostMapper;

    @Mock
    private InnerBlindService innerBlindService;

    @Mock
    private CommunitymanagerService communitymanagerService;

    @InjectMocks
    private HelppostServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", helppostMapper);
    }

    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    private Helppost helppost(Long blindId) {
        Helppost h = new Helppost();
        h.setHelppostId(HELPPOST_ID);
        h.setCommunityId(COMMUNITY_ID);
        h.setBlindId(blindId);
        h.setPostStatus(PostStatusEnum.WAITING.getPostStatus());
        return h;
    }

    // =========================================================
    // addHelppost
    // =========================================================
    @Nested
    @DisplayName("addHelppost 视障人士发帖")
    class AddHelppost {

        @Test
        @DisplayName("视障人士已加入社区 → 成功，状态=待响应")
        void success() {
            HelppostAddRequest req = new HelppostAddRequest();
            req.setHelpContent("我迷路了");
            req.setHelpLocation("地铁站A出口");

            Blind blind = new Blind();
            blind.setBlindId(BLIND_ID);
            blind.setCommunityId(COMMUNITY_ID);
            when(innerBlindService.getById(BLIND_ID)).thenReturn(blind);
            when(helppostMapper.insert(any(Helppost.class))).thenReturn(1);

            var vo = service.addHelppost(req, BLIND_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.getHelpContent()).isEqualTo("我迷路了");
            // HelppostVO.setPostStatus(Integer) 把 code 0 转成状态名 "待响应"（VO 的 postStatus 是 String）。
            // 即源码现状：VO 暴露的是状态名而非 code；测试原先断言 getPostStatus()（int 0）属期望错配，非源码缺陷。
            assertThat(vo.getPostStatus()).isEqualTo(PostStatusEnum.WAITING.getName());
            verify(helppostMapper).insert(any(Helppost.class));
        }

        @Test
        @DisplayName("视障人士未加入社区 → NO_AUTH")
        void noCommunity() {
            HelppostAddRequest req = new HelppostAddRequest();
            req.setHelpContent("救命");
            req.setHelpLocation("某地");

            Blind blind = new Blind();
            blind.setBlindId(BLIND_ID);
            blind.setCommunityId(null); // 未加入
            when(innerBlindService.getById(BLIND_ID)).thenReturn(blind);

            expectCode(() -> service.addHelppost(req, BLIND_ID), ErrorCode.NO_AUTH);
            verify(helppostMapper, never()).insert(any(Helppost.class));
        }

        @Test
        @DisplayName("社区 ID 非法（<=0） → NO_AUTH")
        void invalidCommunityId() {
            HelppostAddRequest req = new HelppostAddRequest();
            req.setHelpContent("内容");
            req.setHelpLocation("地点");

            Blind blind = new Blind();
            blind.setBlindId(BLIND_ID);
            blind.setCommunityId(0L);
            when(innerBlindService.getById(BLIND_ID)).thenReturn(blind);

            expectCode(() -> service.addHelppost(req, BLIND_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("求助内容为空 → PARAMS_ERROR")
        void blankContent() {
            HelppostAddRequest req = new HelppostAddRequest();
            req.setHelpContent("   ");
            req.setHelpLocation("地点");

            expectCode(() -> service.addHelppost(req, BLIND_ID), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(innerBlindService);
        }

        @Test
        @DisplayName("求助地点为空 → PARAMS_ERROR")
        void blankLocation() {
            HelppostAddRequest req = new HelppostAddRequest();
            req.setHelpContent("内容");
            req.setHelpLocation("");

            expectCode(() -> service.addHelppost(req, BLIND_ID), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(innerBlindService);
        }

        @Test
        @DisplayName("请求体 null → PARAMS_ERROR")
        void nullRequest() {
            expectCode(() -> service.addHelppost(null, BLIND_ID), ErrorCode.PARAMS_ERROR);
        }
    }

    // =========================================================
    // getHelppostVOById
    // =========================================================
    @Nested
    @DisplayName("getHelppostVOById 查询")
    class GetById {

        @Test
        @DisplayName("ID 非法 → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.getHelppostVOById(0L), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(helppostMapper);
        }

        @Test
        @DisplayName("求助帖不存在 → PARAMS_ERROR")
        void absent() {
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(null);
            expectCode(() -> service.getHelppostVOById(HELPPOST_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("命中 → 返 VO")
        void found() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);

            var vo = service.getHelppostVOById(HELPPOST_ID);

            assertThat(vo.getHelppostId()).isEqualTo(HELPPOST_ID);
            assertThat(vo.getCommunityId()).isEqualTo(COMMUNITY_ID);
        }
    }

    // =========================================================
    // deleteHelppost —— 权限四象限
    // =========================================================
    @Nested
    @DisplayName("deleteHelppost 权限四象限")
    class DeleteHelppost {

        @Test
        @DisplayName("① 作者本人 → 通过")
        void authorCanDelete() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            // removeById(id) → baseMapper.deleteById(Serializable id)；MP 3.5.9 有 deleteById 三重载
            // （Serializable/Object+boolean/T），裸 any() 编译期歧义且 strict 下参数不匹配 → 用 anyLong() 锁定 Serializable 重载。
            when(helppostMapper.deleteById(anyLong())).thenReturn(1);

            boolean ok = service.deleteHelppost(HELPPOST_ID, BLIND_ID, null);

            assertThat(ok).isTrue();
            verifyNoInteractions(communitymanagerService);
        }

        @Test
        @DisplayName("② 社区 ADMIN 志愿者 → 通过")
        void adminCanDelete() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            Communitymanager adminRole = role(CommunityRolePermissionEnum.ADMIN.getRoleId());
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(adminRole);
            when(helppostMapper.deleteById(anyLong())).thenReturn(1);

            boolean ok = service.deleteHelppost(HELPPOST_ID, null, VOLUNTEER_ID);

            assertThat(ok).isTrue();
        }

        @Test
        @DisplayName("③ 社区 EMPLOYEE → NO_AUTH")
        void employeeCannotDelete() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            Communitymanager empRole = role(CommunityRolePermissionEnum.EMPLOYEE.getRoleId());
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(empRole);

            expectCode(() -> service.deleteHelppost(HELPPOST_ID, null, VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(helppostMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("④ 非作者非管理员（操作人不在该社区，返 null） → NO_AUTH")
        void nonAuthorNonAdmin() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(null);

            expectCode(() -> service.deleteHelppost(HELPPOST_ID, OTHER_BLIND_ID, VOLUNTEER_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("未登录（loginBlindId 与 loginVolunteerId 都为 null） → NOT_LOGIN")
        void notLogin() {
            expectCode(() -> service.deleteHelppost(HELPPOST_ID, null, null), ErrorCode.NOT_LOGIN);
        }

        @Test
        @DisplayName("求助帖不存在 → PARAMS_ERROR")
        void absent() {
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(null);
            expectCode(() -> service.deleteHelppost(HELPPOST_ID, BLIND_ID, null), ErrorCode.PARAMS_ERROR);
        }
    }

    // =========================================================
    // updateHelppost —— 权限四象限 + 字段更新
    // =========================================================
    @Nested
    @DisplayName("updateHelppost 权限与字段更新")
    class UpdateHelppost {

        @Test
        @DisplayName("① 作者本人修改内容 → 通过")
        void authorCanUpdate() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            when(helppostMapper.updateById(any(Helppost.class))).thenReturn(1);

            HelppostUpdateRequest req = updateReq();
            req.setHelpContent("新内容");
            req.setHelpLocation("新地点");
            req.setPostStatus("处理中");

            boolean ok = service.updateHelppost(req, BLIND_ID, null);

            assertThat(ok).isTrue();
            assertThat(h.getHelpContent()).isEqualTo("新内容");
            assertThat(h.getHelpLocation()).isEqualTo("新地点");
            assertThat(h.getPostStatus()).isEqualTo(PostStatusEnum.HELPING.getPostStatus());
        }

        @Test
        @DisplayName("② 社区 REGISTRANT 志愿者 → 通过")
        void registrantCanUpdate() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(role(CommunityRolePermissionEnum.REGISTRANT.getRoleId()));
            when(helppostMapper.updateById(any(Helppost.class))).thenReturn(1);

            boolean ok = service.updateHelppost(updateReq(), OTHER_BLIND_ID, VOLUNTEER_ID);

            assertThat(ok).isTrue();
        }

        @Test
        @DisplayName("③ 社区 EMPLOYEE → NO_AUTH")
        void employeeCannotUpdate() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(role(CommunityRolePermissionEnum.EMPLOYEE.getRoleId()));

            expectCode(() -> service.updateHelppost(updateReq(), null, VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(helppostMapper, never()).updateById(any(Helppost.class));
        }

        @Test
        @DisplayName("④ 非作者非管理员 → NO_AUTH")
        void nonAuthorNonAdmin() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);
            when(communitymanagerService.getByVolunteerIdAndCommunityId(VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(null);

            expectCode(() -> service.updateHelppost(updateReq(), OTHER_BLIND_ID, VOLUNTEER_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("未登录 → NOT_LOGIN")
        void notLogin() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);

            expectCode(() -> service.updateHelppost(updateReq(), null, null), ErrorCode.NOT_LOGIN);
        }

        @Test
        @DisplayName("非法 postStatus → PARAMS_ERROR")
        void invalidStatus() {
            Helppost h = helppost(BLIND_ID);
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(h);

            HelppostUpdateRequest req = updateReq();
            req.setPostStatus("不存在的状态");

            expectCode(() -> service.updateHelppost(req, BLIND_ID, null), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("求助帖不存在 → PARAMS_ERROR")
        void absent() {
            when(helppostMapper.selectById(HELPPOST_ID)).thenReturn(null);
            expectCode(() -> service.updateHelppost(updateReq(), BLIND_ID, null), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("请求体 null → PARAMS_ERROR")
        void nullRequest() {
            expectCode(() -> service.updateHelppost(null, BLIND_ID, null), ErrorCode.PARAMS_ERROR);
        }
    }

    // ---------- 辅助 ----------

    private HelppostUpdateRequest updateReq() {
        HelppostUpdateRequest req = new HelppostUpdateRequest();
        req.setHelppostId(HELPPOST_ID);
        return req;
    }

    private Communitymanager role(long roleId) {
        Communitymanager m = new Communitymanager();
        m.setCommunityId(COMMUNITY_ID);
        m.setVolunteerId(VOLUNTEER_ID);
        m.setRolePermissionId(roleId);
        return m;
    }
}
