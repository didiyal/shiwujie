package com.swj.shiwujie.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.community.CommunityUpdateRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.utils.PasswordUtils;
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
 * {@link CommunityServiceImpl} 单元测试——社区注册人权限 + 登录密码懒升级。
 *
 * <p>权限模型（updateCommunity / deleteCommunity）：仅 {@code community.registerVolunteerId == loginVolunteerId}
 * 可操作，否则 NO_AUTH「仅社区注册人可修改/删除」。communityLogin 兼容历史 MD5 并懒升级为 BCrypt。
 *
 * <p>communityRegister 流程复杂（自动注册志愿者/自动建省市社区/绑定 REGISTRANT），单测只覆盖
 * 高价值分支：密码/管理员绑定失败的兜底；正向流程因依赖 {@code BeanUtils.copyProperties} + 多次 save
 * 难以稳定断言，留作集成测试。
 */
@DisplayName("CommunityServiceImpl 注册人权限 + 登录")
@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    private static final Long COMMUNITY_ID = 100L;
    private static final Long REGISTER_VOLUNTEER_ID = 10L;
    private static final Long OTHER_VOLUNTEER_ID = 99L;

    @Mock
    private CommunityMapper communityMapper;

    @Mock
    private InnerVolunteerService innerVolunteerService;

    @Mock
    private InnerBlindService innerBlindService;

    @Mock
    private CommunitymanagerService communitymanagerService;

    @InjectMocks
    private CommunityServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段 @InjectMocks 不可靠注入，反射强制赋值
        ReflectionTestUtils.setField(service, "baseMapper", communityMapper);
    }

    private static void expectCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(expected.getCode()));
    }

    // =========================================================
    // updateCommunity —— 仅注册人
    // =========================================================
    @Nested
    @DisplayName("updateCommunity 仅注册人可修改")
    class UpdateCommunity {

        @Test
        @DisplayName("社区不存在 → PARAMS_ERROR")
        void communityAbsent() {
            CommunityUpdateRequest req = updateReq();
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(null);

            expectCode(() -> service.updateCommunity(req, REGISTER_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("操作人非注册人 → NO_AUTH")
        void notRegistrant() {
            CommunityUpdateRequest req = updateReq();
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);

            expectCode(() -> service.updateCommunity(req, OTHER_VOLUNTEER_ID), ErrorCode.NO_AUTH);
            verify(communityMapper, never()).updateById(any(Community.class));
        }

        @Test
        @DisplayName("注册人本人修改名称与介绍 → 通过，字段更新")
        void registrantCanUpdate() {
            CommunityUpdateRequest req = updateReq();
            req.setCommunityName("新名字");
            req.setCommunityDescription("新介绍");
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communityMapper.updateById(any(Community.class))).thenReturn(1);

            var vo = service.updateCommunity(req, REGISTER_VOLUNTEER_ID);

            assertThat(vo).isNotNull();
            assertThat(community.getCommunityName()).isEqualTo("新名字");
            assertThat(community.getCommunityDescription()).isEqualTo("新介绍");
        }

        @Test
        @DisplayName("空白字段不覆盖（trim 后为空跳过）")
        void blankFieldsNotOverwritten() {
            CommunityUpdateRequest req = updateReq();
            req.setCommunityName("   ");
            req.setCommunityDescription("");
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            community.setCommunityName("原名");
            community.setCommunityDescription("原介绍");
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communityMapper.updateById(any(Community.class))).thenReturn(1);

            service.updateCommunity(req, REGISTER_VOLUNTEER_ID);

            assertThat(community.getCommunityName()).isEqualTo("原名");
            assertThat(community.getCommunityDescription()).isEqualTo("原介绍");
        }

        @Test
        @DisplayName("更新失败（updateById 返 false） → SYSTEM_ERROR")
        void updateFails() {
            CommunityUpdateRequest req = updateReq();
            req.setCommunityName("新");
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(communityMapper.updateById(any(Community.class))).thenReturn(0);

            expectCode(() -> service.updateCommunity(req, REGISTER_VOLUNTEER_ID), ErrorCode.SYSTEM_ERROR);
        }
    }

    // =========================================================
    // deleteCommunity —— 仅注册人
    // =========================================================
    @Nested
    @DisplayName("deleteCommunity 仅注册人可删除")
    class DeleteCommunity {

        @Test
        @DisplayName("参数非法（communityId<=0） → PARAMS_ERROR")
        void invalidId() {
            expectCode(() -> service.deleteCommunity(0L, REGISTER_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(communityMapper);
        }

        @Test
        @DisplayName("社区不存在 → PARAMS_ERROR")
        void communityAbsent() {
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(null);

            expectCode(() -> service.deleteCommunity(COMMUNITY_ID, REGISTER_VOLUNTEER_ID), ErrorCode.PARAMS_ERROR);
        }

        @Test
        @DisplayName("操作人非注册人 → NO_AUTH")
        void notRegistrant() {
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);

            expectCode(() -> service.deleteCommunity(COMMUNITY_ID, OTHER_VOLUNTEER_ID), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("注册人删除 → 链路全通，返 true")
        void registrantCanDelete() {
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(innerBlindService.removeCommunityId(COMMUNITY_ID)).thenReturn(true);
            when(innerVolunteerService.removeCommunityId(COMMUNITY_ID)).thenReturn(true);
            when(communitymanagerService.removeByCommunityId(COMMUNITY_ID)).thenReturn(1);
            // removeById(id) → baseMapper.deleteById(Serializable id)；MP 3.5.9 有 deleteById 三重载
            // （Serializable/Object+boolean/T），裸 any() 编译期歧义且 strict 下参数不匹配 → 用 anyLong() 锁定 Serializable 重载。
            when(communityMapper.deleteById(anyLong())).thenReturn(1);

            boolean ok = service.deleteCommunity(COMMUNITY_ID, REGISTER_VOLUNTEER_ID);

            assertThat(ok).isTrue();
            verify(innerBlindService).removeCommunityId(COMMUNITY_ID);
            verify(innerVolunteerService).removeCommunityId(COMMUNITY_ID);
            verify(communitymanagerService).removeByCommunityId(COMMUNITY_ID);
            verify(communityMapper).deleteById(anyLong());
        }

        @Test
        @DisplayName("视障人士移出失败 → SYSTEM_ERROR")
        void blindRemoveFails() {
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(innerBlindService.removeCommunityId(COMMUNITY_ID)).thenReturn(false);

            expectCode(() -> service.deleteCommunity(COMMUNITY_ID, REGISTER_VOLUNTEER_ID), ErrorCode.SYSTEM_ERROR);
            verify(innerVolunteerService, never()).removeCommunityId(anyLong());
        }

        @Test
        @DisplayName("志愿者移出失败 → SYSTEM_ERROR")
        void volunteerRemoveFails() {
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(innerBlindService.removeCommunityId(COMMUNITY_ID)).thenReturn(true);
            when(innerVolunteerService.removeCommunityId(COMMUNITY_ID)).thenReturn(false);

            expectCode(() -> service.deleteCommunity(COMMUNITY_ID, REGISTER_VOLUNTEER_ID), ErrorCode.SYSTEM_ERROR);
            verify(communitymanagerService, never()).removeByCommunityId(anyLong());
        }
    }

    // =========================================================
    // communityLogin —— 密码 + legacy 懒升级
    // =========================================================
    @Nested
    @DisplayName("communityLogin 密码校验与懒升级")
    class CommunityLogin {

        @Test
        @DisplayName("手机号或密码空 → PARAMS_ERROR")
        void emptyCreds() {
            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone("");
            req.setPassword("x");

            expectCode(() -> service.communityLogin(req), ErrorCode.PARAMS_ERROR);
            verifyNoInteractions(innerVolunteerService);
        }

        @Test
        @DisplayName("用户不存在 → OPERATION_ERROR")
        void userAbsent() {
            VolunteerLARRequest req = loginReq("13800000000", "pwd");
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(null);

            expectCode(() -> service.communityLogin(req), ErrorCode.OPERATION_ERROR);
        }

        @Test
        @DisplayName("密码错误 → PARAMS_ERROR")
        void wrongPassword() {
            VolunteerLARRequest req = loginReq("13800000000", "wrong");
            Volunteer v = volunteerWithPassword(PasswordUtils.hash("correct"));
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(v);

            expectCode(() -> service.communityLogin(req), ErrorCode.PARAMS_ERROR);
            verify(innerVolunteerService, never()).updateById(any(Volunteer.class));
        }

        @Test
        @DisplayName("无社区管理权限（count<=0） → NO_AUTH")
        void noManagerRole() {
            VolunteerLARRequest req = loginReq("13800000000", "pwd");
            Volunteer v = volunteerWithPassword(PasswordUtils.hash("pwd"));
            v.setVolunteerId(REGISTER_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(v);
            when(communitymanagerService.getCountByVolunteerIdAndCommunityId(REGISTER_VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(0L);

            expectCode(() -> service.communityLogin(req), ErrorCode.NO_AUTH);
        }

        @Test
        @DisplayName("社区不存在 → OPERATION_ERROR")
        void communityAbsent() {
            VolunteerLARRequest req = loginReq("13800000000", "pwd");
            Volunteer v = volunteerWithPassword(PasswordUtils.hash("pwd"));
            v.setVolunteerId(REGISTER_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(v);
            when(communitymanagerService.getCountByVolunteerIdAndCommunityId(REGISTER_VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(1L);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(null);

            expectCode(() -> service.communityLogin(req), ErrorCode.OPERATION_ERROR);
        }

        @Test
        @DisplayName("BCrypt 库存密码登录通过 → 不触发懒升级")
        void bcryptLoginNoUpgrade() {
            VolunteerLARRequest req = loginReq("13800000000", "pwd");
            Volunteer v = volunteerWithPassword(PasswordUtils.hash("pwd"));
            v.setVolunteerId(REGISTER_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(v);
            when(communitymanagerService.getCountByVolunteerIdAndCommunityId(REGISTER_VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(1L);
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(innerVolunteerService.getVolunteerVO(any())).thenReturn(new com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO());
            when(innerVolunteerService.generateLoginToken(any())).thenReturn("token");

            var result = service.communityLogin(req);

            assertThat(result).isNotNull();
            verify(innerVolunteerService, never()).updateById(any(Volunteer.class)); // 不升级
        }

        @Test
        @DisplayName("历史 MD5 库存密码通过 → 懒升级为 BCrypt（updateById 被调）")
        void legacyMd5Upgrades() {
            String plain = "pwd";
            String md5Stored = SecureUtil.md5(plain); // 32 位 hex
            VolunteerLARRequest req = loginReq("13800000000", plain);
            Volunteer v = volunteerWithPassword(md5Stored);
            v.setVolunteerId(REGISTER_VOLUNTEER_ID);
            v.setCommunityId(COMMUNITY_ID);
            when(innerVolunteerService.getByPhone("13800000000")).thenReturn(v);
            when(communitymanagerService.getCountByVolunteerIdAndCommunityId(REGISTER_VOLUNTEER_ID, COMMUNITY_ID))
                    .thenReturn(1L);
            Community community = communityOwnedBy(REGISTER_VOLUNTEER_ID);
            when(communityMapper.selectById(COMMUNITY_ID)).thenReturn(community);
            when(innerVolunteerService.getVolunteerVO(any())).thenReturn(new com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO());
            when(innerVolunteerService.generateLoginToken(any())).thenReturn("token");

            var result = service.communityLogin(req);

            assertThat(result).isNotNull();
            // 关键：updateById 触发，且密码已升级为 BCrypt（$2a$ 前缀）
            verify(innerVolunteerService).updateById(argThat(vol ->
                    vol.getPassword() != null && vol.getPassword().startsWith("$2a$")));
            assertThat(v.getPassword()).startsWith("$2a$");
        }
    }

    // =========================================================
    // getCommunityVO —— typeId/levelId 为 null 时不 NPE
    // =========================================================
    @Nested
    @DisplayName("getCommunityVO 脱敏")
    class GetVO {

        @Test
        @DisplayName("typeId/levelId 为 null → VO 对应字段为 null，不 NPE")
        void nullTypeAndLevel() {
            Community c = new Community();
            c.setCommunityId(COMMUNITY_ID);
            c.setCommunityName("测试社区");
            c.setCommunityTypeId(null);
            c.setCommunityLevelId(null);

            var vo = service.getCommunityVO(c);

            assertThat(vo.getCommunityId()).isEqualTo(COMMUNITY_ID);
            assertThat(vo.getCommunityTypeName()).isNull();
            assertThat(vo.getCommunityLevelName()).isNull();
        }
    }

    // ---------- 辅助构造 ----------

    private CommunityUpdateRequest updateReq() {
        CommunityUpdateRequest req = new CommunityUpdateRequest();
        req.setCommunityId(COMMUNITY_ID);
        return req;
    }

    private Community communityOwnedBy(Long registerVolunteerId) {
        Community c = new Community();
        c.setCommunityId(COMMUNITY_ID);
        c.setRegisterVolunteerId(registerVolunteerId);
        return c;
    }

    private VolunteerLARRequest loginReq(String phone, String pwd) {
        VolunteerLARRequest req = new VolunteerLARRequest();
        req.setPhone(phone);
        req.setPassword(pwd);
        return req;
    }

    private Volunteer volunteerWithPassword(String password) {
        Volunteer v = new Volunteer();
        v.setPassword(password);
        return v;
    }
}
