package com.swj.shiwujie.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;
import com.swj.shiwujie.service.community.InnerCommunitymanagerService;
import com.swj.shiwujie.utils.PasswordUtils;
import com.swj.shiwujie.utils.RedisUtils;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link VolunteerServiceImpl} 单元测试——志愿者域安全热路径（与 BlindServiceImpl 对称）。
 * 纯 Mockito：@ExtendWith(MockitoExtension) + @Mock + @InjectMocks，不起 Spring/不连库。
 *
 * <p><b>MyBatis-Plus 调用链说明（已反编译 MP 3.5.9 字节码核实）：</b>
 * <ul>
 *   <li>{@code this.getOne(qw)} → {@code baseMapper.selectOne(qw, true)} <b>两参</b>，mock 默认返 null。stub 两参版本。</li>
 *   <li>{@code this.save(e)} → {@code baseMapper.insert(e)}。</li>
 *   <li>{@code this.updateById(e)} → {@code baseMapper.updateById(e)}。</li>
 *   <li>{@code this.getById(id)} → {@code baseMapper.selectById(id)}。</li>
 *   <li>{@code blindMapper.selectOne(qw)} <b>单参</b>（getBlindByPhone 直接调用），stub 单参。</li>
 * </ul>
 */
@DisplayName("VolunteerServiceImpl 志愿者服务")
@ExtendWith(MockitoExtension.class)
class VolunteerServiceImplTest {

    @Mock
    private VolunteerMapper volunteerMapper;

    @Mock
    private BlindMapper blindMapper;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private InnerCommunitymanagerService innerCommunitymanagerService;

    @InjectMocks
    private VolunteerServiceImpl volunteerService;

    private static final String VALID_PHONE = "13900139000";

    /**
     * 手动反射注入父类 baseMapper（同 BlindServiceImplTest 说明）。
     */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(volunteerService, "baseMapper", volunteerMapper);
    }

    // ==================== loginAndRegister ====================

    @Nested
    @DisplayName("loginAndRegister(VolunteerLARRequest)：手机号+密码 登录注册")
    class LoginAndRegister {

        @Test
        @DisplayName("① 全新手机号 → 注册分支：save 被调、密码存 BCrypt、返回 VO")
        void register_newPhone_savesBcryptAndReturnsVO() {
            String plain = "abc123";
            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            // getByPhone、getBlindByPhone 都默认返 null
            when(volunteerMapper.insert(any(Volunteer.class))).thenReturn(1);

            VolunteerLoginSuccessVO vo = volunteerService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            org.mockito.ArgumentCaptor<Volunteer> captor =
                    org.mockito.ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerMapper).insert(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("② 已存在且 BCrypt 密码匹配 → 登录成功，updateById 不被调")
        void login_existingBcryptMatch_succeedsWithoutUpgrade() {
            String plain = "abc123";
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(100L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(PasswordUtils.hash(plain));

            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            when(volunteerMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            VolunteerLoginSuccessVO vo = volunteerService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            verify(volunteerMapper, never()).updateById(any(Volunteer.class));
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("🔥 ③ 已存在且密码为 legacy MD5 → 登录成功且触发懒升级（updateById 被调、密码变 BCrypt）")
        void login_existingLegacyMd5_triggersLazyUpgrade() {
            String plain = "abc123";
            String md5Stored = SecureUtil.md5(plain);

            Volunteer existing = new Volunteer();
            existing.setVolunteerId(200L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(md5Stored);

            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            when(volunteerMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);
            when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

            VolunteerLoginSuccessVO vo = volunteerService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            org.mockito.ArgumentCaptor<Volunteer> captor =
                    org.mockito.ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerMapper, times(1)).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("④ 已存在但密码错 → 抛 PARAMS_ERROR")
        void login_existingWrongPassword_throws() {
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(300L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(PasswordUtils.hash("correctPwd1"));

            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("wrongPwd9");

            when(volunteerMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(volunteerMapper, never()).updateById(any(Volunteer.class));
            verify(redisUtils, never()).setToRedis(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("⑤ 快注册无密码用户走密码登录 → 抛 PARAMS_ERROR")
        void login_existingNullPassword_throws() {
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(400L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(null);

            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abc123");

            when(volunteerMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("⑥ 跨表查重：手机号已注册盲人 → 抛 PARAMS_ERROR")
        void register_phoneUsedByBlind_throws() {
            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abc123");

            Blind other = new Blind();
            other.setBlindId(1L);
            other.setPhone(VALID_PHONE);

            // getByPhone 默认返 null；getBlindByPhone → blindMapper.selectOne 单参
            when(blindMapper.selectOne(any(Wrapper.class))).thenReturn(other);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(volunteerMapper, never()).insert(any(Volunteer.class));
        }

        @Test
        @DisplayName("非法手机号 → 抛 PARAMS_ERROR，不查库")
        void login_invalidPhone_throws() {
            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone("not-a-phone");
            req.setPassword("abc123");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(volunteerMapper, never()).selectOne(any(), anyBoolean());
        }

        @Test
        @DisplayName("密码格式非法 → 抛 PARAMS_ERROR")
        void login_invalidPasswordFormat_throws() {
            VolunteerLARRequest req = new VolunteerLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("onlyletters");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }
    }

    // ==================== updateVolunteerPassword ====================

    @Nested
    @DisplayName("updateVolunteerPassword：修改密码（origin 兼容 legacy）")
    class UpdateVolunteerPassword {

        @Test
        @DisplayName("origin BCrypt 匹配 + newPassword 合法 → 成功")
        void update_originBcryptMatch_success() {
            String originPlain = "abc123";
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(1L);
            existing.setPassword(PasswordUtils.hash(originPlain));

            VolunteerUpdatePasswordRequest req = new VolunteerUpdatePasswordRequest();
            req.setVolunteerId(1L);
            req.setOriginPassword(originPlain);
            req.setNewPassword("xyz789");

            when(volunteerMapper.selectById(eq(1L))).thenReturn(existing);
            when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

            boolean result = volunteerService.updateVolunteerPassword(req);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<Volunteer> captor =
                    org.mockito.ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerMapper).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("🔥 origin legacy MD5 仍能匹配 → 升级为新 BCrypt")
        void update_originLegacyMd5Match_upgrades() {
            String originPlain = "abc123";
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(2L);
            existing.setPassword(SecureUtil.md5(originPlain));

            VolunteerUpdatePasswordRequest req = new VolunteerUpdatePasswordRequest();
            req.setVolunteerId(2L);
            req.setOriginPassword(originPlain);
            req.setNewPassword("xyz789");

            when(volunteerMapper.selectById(eq(2L))).thenReturn(existing);
            when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

            boolean result = volunteerService.updateVolunteerPassword(req);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<Volunteer> captor =
                    org.mockito.ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerMapper).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("origin 错 → 抛 PARAMS_ERROR")
        void update_wrongOrigin_throws() {
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(3L);
            existing.setPassword(PasswordUtils.hash("correct1"));

            VolunteerUpdatePasswordRequest req = new VolunteerUpdatePasswordRequest();
            req.setVolunteerId(3L);
            req.setOriginPassword("wrong999");
            req.setNewPassword("xyz789");

            when(volunteerMapper.selectById(eq(3L))).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.updateVolunteerPassword(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("origin 空 → 跳过原密码校验")
        void update_blankOrigin_skipsCheck() {
            Volunteer existing = new Volunteer();
            existing.setVolunteerId(4L);
            existing.setPassword(null);

            VolunteerUpdatePasswordRequest req = new VolunteerUpdatePasswordRequest();
            req.setVolunteerId(4L);
            req.setOriginPassword(null);
            req.setNewPassword("newPass1");

            when(volunteerMapper.selectById(eq(4L))).thenReturn(existing);
            when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

            assertThat(volunteerService.updateVolunteerPassword(req)).isTrue();
        }
    }

    // ==================== generateLoginToken ====================

    @Nested
    @DisplayName("generateLoginToken：jwt payload + redis 90 天 + 社区角色")
    class GenerateLoginToken {

        @Test
        @DisplayName("未加入社区：role=null，token 入 redis 90 天")
        void generateToken_noCommunity_roleNull() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(777L);
            v.setPhone(VALID_PHONE);
            v.setCommunityId(null); // 未加入社区

            String token = volunteerService.generateLoginToken(v);

            assertThat(token).isNotBlank();
            verify(redisUtils, times(1))
                    .setToRedis(eq("REDIS_SECRETKEY-volunteer-777"), eq(token), eq(90L));
            // communityId 为 null → 不查社区角色
            verify(innerCommunitymanagerService, never())
                    .getCountByVolunteerIdAndCommunityId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("已加入社区且为管理员：role 被写入 jwt（getByVolunteerIdAndCommunityId 被调）")
        void generateToken_withCommunityRole() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(888L);
            v.setPhone(VALID_PHONE);
            v.setCommunityId(55L);

            // generateLoginToken 内部查 count，count==1 时再查 Communitymanager
            when(innerCommunitymanagerService.getCountByVolunteerIdAndCommunityId(eq(888L), eq(55L)))
                    .thenReturn(1L);
            Communitymanager mgr = new Communitymanager();
            mgr.setRolePermissionId(2L); // ADMIN
            when(innerCommunitymanagerService.getByVolunteerIdAndCommunityId(eq(888L), eq(55L)))
                    .thenReturn(mgr);

            String token = volunteerService.generateLoginToken(v);

            assertThat(token).isNotBlank();
            verify(redisUtils, times(1))
                    .setToRedis(eq("REDIS_SECRETKEY-volunteer-888"), eq(token), eq(90L));
        }
    }

    // ==================== validatePassword / getByPhone（轻量） ====================

    @Nested
    @DisplayName("工具方法：validatePassword / getByPhone")
    class UtilityMethods {

        @Test
        @DisplayName("validatePassword：'abc123' 合法 → true")
        void validatePassword_legal_true() {
            assertThat(volunteerService.validatePassword("abc123")).isTrue();
        }

        @Test
        @DisplayName("validatePassword：null → false")
        void validatePassword_null_false() {
            assertThat(volunteerService.validatePassword(null)).isFalse();
        }

        @Test
        @DisplayName("getByPhone 合法手机号 → getOne → volunteerMapper.selectOne(qw, true) 两参 返回 Volunteer")
        void getByPhone_valid_returnsVolunteer() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(9L);
            when(volunteerMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(v);

            Volunteer result = volunteerService.getByPhone(VALID_PHONE);

            assertThat(result).isNotNull();
            assertThat(result.getVolunteerId()).isEqualTo(9L);
        }
    }
}
