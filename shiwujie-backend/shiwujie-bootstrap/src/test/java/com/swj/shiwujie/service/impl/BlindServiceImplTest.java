package com.swj.shiwujie.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.blind.BlindLoginSuccessVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.blind.BlindLARRequest;
import com.swj.shiwujie.model.request.user.blind.BlindUpdatePasswordRequest;
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
 * {@link BlindServiceImpl} 单元测试——视障人士域安全热路径。
 * 纯 Mockito：@ExtendWith(MockitoExtension) + @Mock + @InjectMocks，不起 Spring/不连库。
 *
 * <p><b>MyBatis-Plus 调用链说明（已反编译 MP 3.5.9 字节码核实）：</b>
 * <ul>
 *   <li>{@code this.getOne(qw)} → {@code baseMapper.selectOne(qw, true)} <b>两参</b>（default 方法，mock 默认返 null 不执行）。
 *       想让"用户已存在"分支生效，必须 stub 两参 {@code selectOne(any(), anyBoolean())}。</li>
 *   <li>{@code this.save(e)} → {@code baseMapper.insert(e)}（abstract，默认返 0=失败）。</li>
 *   <li>{@code this.updateById(e)} → {@code baseMapper.updateById(e)}（abstract，默认返 0=失败）。</li>
 *   <li>{@code this.getById(id)} → {@code baseMapper.selectById(id)}（abstract，默认返 null）。</li>
 *   <li>{@code volunteerMapper.selectOne(qw)} <b>单参</b>（default 方法，mock 默认返 null）—— 直接 mapper 调用走单参。</li>
 * </ul>
 */
@DisplayName("BlindServiceImpl 视障人士服务")
@ExtendWith(MockitoExtension.class)
class BlindServiceImplTest {

    @Mock
    private BlindMapper blindMapper;

    @Mock
    private VolunteerMapper volunteerMapper;

    @Mock
    private RedisUtils redisUtils;

    @InjectMocks
    private BlindServiceImpl blindService;

    private static final String VALID_PHONE = "13800138000";

    /**
     * MP 3.5.9 的 {@code ServiceImpl.getBaseMapper()} 会断言 baseMapper != null。
     * Mockito {@code @InjectMocks} 对**继承字段**注入有限制：当 mock 字段名与 baseMapper 不一致
     * 且有多个 mapper mock 时，不会注入到父类 CrudRepository 的 baseMapper 字段。
     * 这里手动反射注入，确保父类 baseMapper 字段被赋值。
     */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(blindService, "baseMapper", blindMapper);
    }

    // ==================== loginAndRegister ====================

    @Nested
    @DisplayName("loginAndRegister(BlindLARRequest)：手机号+密码 登录注册")
    class LoginAndRegister {

        @Test
        @DisplayName("① 全新手机号 → 注册分支：save 被调、密码存 BCrypt、generateLoginToken 被调、返回 VO")
        void register_newPhone_savesBcryptAndReturnsVO() {
            String plain = "abc123";
            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            // getByPhone → getOne → baseMapper.selectOne(qw, true) 两参 默认返 null（不显式 stub）
            // getVolunteerByPhone → volunteerMapper.selectOne(qw) 单参 默认返 null
            // save → baseMapper.insert 返回 1
            when(blindMapper.insert(any(Blind.class))).thenReturn(1);

            BlindLoginSuccessVO vo = blindService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            verify(blindMapper, times(1)).insert(any(Blind.class));
            // 密码被 BCrypt 处理
            org.mockito.ArgumentCaptor<Blind> captor = org.mockito.ArgumentCaptor.forClass(Blind.class);
            verify(blindMapper).insert(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
            // token 入 redis（90 天）
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("② 已存在且 BCrypt 密码匹配 → 登录成功，updateById 不被调（无需懒升级）")
        void login_existingBcryptMatch_succeedsWithoutUpgrade() {
            String plain = "abc123";
            Blind existing = new Blind();
            existing.setBlindId(100L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(PasswordUtils.hash(plain));

            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            // getByPhone → getOne → 两参 selectOne，必须显式 stub
            when(blindMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BlindLoginSuccessVO vo = blindService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            verify(blindMapper, never()).updateById(any(Blind.class));
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("🔥 ③ 已存在且密码为 legacy MD5 → 登录成功且触发懒升级（updateById 被调、密码变 BCrypt）")
        void login_existingLegacyMd5_triggersLazyUpgrade() {
            String plain = "abc123";
            String md5Stored = SecureUtil.md5(plain);

            Blind existing = new Blind();
            existing.setBlindId(200L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(md5Stored);

            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword(plain);

            when(blindMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);
            when(blindMapper.updateById(any(Blind.class))).thenReturn(1);

            BlindLoginSuccessVO vo = blindService.loginAndRegister(req);

            assertThat(vo).isNotNull();
            org.mockito.ArgumentCaptor<Blind> captor = org.mockito.ArgumentCaptor.forClass(Blind.class);
            verify(blindMapper, times(1)).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
            verify(redisUtils, times(1)).setToRedis(anyString(), anyString(), eq(90L));
        }

        @Test
        @DisplayName("④ 已存在但密码错 → 抛 PARAMS_ERROR（40000）")
        void login_existingWrongPassword_throws() {
            Blind existing = new Blind();
            existing.setBlindId(300L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(PasswordUtils.hash("correctPwd1"));

            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("wrongPwd9");

            when(blindMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(blindMapper, never()).updateById(any(Blind.class));
            verify(redisUtils, never()).setToRedis(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("⑤ 快注册无密码存量用户走密码登录 → matches(plain, null)=false 抛 PARAMS_ERROR")
        void login_existingNullPassword_throws() {
            Blind existing = new Blind();
            existing.setBlindId(400L);
            existing.setPhone(VALID_PHONE);
            existing.setPassword(null);

            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abc123");

            when(blindMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("⑥ 跨表查重：手机号已注册志愿者 → 抛 PARAMS_ERROR（不注册 Blind）")
        void register_phoneUsedByVolunteer_throws() {
            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abc123");

            Volunteer other = new Volunteer();
            other.setVolunteerId(1L);
            other.setPhone(VALID_PHONE);

            // getByPhone 默认返 null（用户不存在）；getVolunteerByPhone → volunteerMapper.selectOne 单参
            when(volunteerMapper.selectOne(any(Wrapper.class))).thenReturn(other);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(blindMapper, never()).insert(any(Blind.class));
        }

        @Test
        @DisplayName("非法手机号 → 抛 PARAMS_ERROR，不查库")
        void login_invalidPhone_throws() {
            BlindLARRequest req = new BlindLARRequest();
            req.setPhone("not-a-phone");
            req.setPassword("abc123");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(blindMapper, never()).selectOne(any(), anyBoolean());
        }

        @Test
        @DisplayName("密码格式非法（无数字） → 抛 PARAMS_ERROR")
        void login_invalidPasswordFormat_throws() {
            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abcdef");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
            verify(blindMapper, never()).selectOne(any(), anyBoolean());
        }

        @Test
        @DisplayName("注册分支 save 失败 → 抛 SYSTEM_ERROR")
        void register_saveFails_throwsSystemError() {
            BlindLARRequest req = new BlindLARRequest();
            req.setPhone(VALID_PHONE);
            req.setPassword("abc123");

            // getByPhone、getVolunteerByPhone 都默认返 null；insert 返 0 = save 失败
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.loginAndRegister(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        }
    }

    // ==================== updateBlindPassword ====================

    @Nested
    @DisplayName("updateBlindPassword：修改密码（origin 兼容 legacy MD5/BCrypt）")
    class UpdateBlindPassword {

        @Test
        @DisplayName("origin BCrypt 匹配 + newPassword 合法 → updateById 成功，新密码为 BCrypt")
        void update_originBcryptMatch_success() {
            String originPlain = "abc123";
            Blind existing = new Blind();
            existing.setBlindId(1L);
            existing.setPassword(PasswordUtils.hash(originPlain));

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(1L);
            req.setOriginPassword(originPlain);
            req.setNewPassword("xyz789");

            // getById → baseMapper.selectById（abstract，必须 stub）
            when(blindMapper.selectById(eq(1L))).thenReturn(existing);
            when(blindMapper.updateById(any(Blind.class))).thenReturn(1);

            boolean result = blindService.updateBlindPassword(req);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<Blind> captor = org.mockito.ArgumentCaptor.forClass(Blind.class);
            verify(blindMapper).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("🔥 origin 为 legacy MD5 仍能匹配 → 升级成功，新密码 BCrypt")
        void update_originLegacyMd5Match_upgradesToBcrypt() {
            String originPlain = "abc123";
            Blind existing = new Blind();
            existing.setBlindId(2L);
            existing.setPassword(SecureUtil.md5(originPlain)); // legacy MD5

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(2L);
            req.setOriginPassword(originPlain);
            req.setNewPassword("xyz789");

            when(blindMapper.selectById(eq(2L))).thenReturn(existing);
            when(blindMapper.updateById(any(Blind.class))).thenReturn(1);

            boolean result = blindService.updateBlindPassword(req);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<Blind> captor = org.mockito.ArgumentCaptor.forClass(Blind.class);
            verify(blindMapper).updateById(captor.capture());
            assertThat(captor.getValue().getPassword()).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("origin 错 → 抛 PARAMS_ERROR")
        void update_wrongOrigin_throws() {
            Blind existing = new Blind();
            existing.setBlindId(3L);
            existing.setPassword(PasswordUtils.hash("correct1"));

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(3L);
            req.setOriginPassword("wrong999");
            req.setNewPassword("xyz789");

            when(blindMapper.selectById(eq(3L))).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.updateBlindPassword(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("origin 为空 → 直接设置新密码（无原密码校验分支）")
        void update_blankOrigin_skipsOriginCheck() {
            Blind existing = new Blind();
            existing.setBlindId(4L);
            existing.setPassword(null);

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(4L);
            req.setOriginPassword(null);
            req.setNewPassword("newPass1");

            when(blindMapper.selectById(eq(4L))).thenReturn(existing);
            when(blindMapper.updateById(any(Blind.class))).thenReturn(1);

            boolean result = blindService.updateBlindPassword(req);

            assertThat(result).isTrue();
            verify(blindMapper).updateById(any(Blind.class));
        }

        @Test
        @DisplayName("newPassword 格式非法 → 抛 PARAMS_ERROR")
        void update_invalidNewFormat_throws() {
            Blind existing = new Blind();
            existing.setBlindId(5L);
            existing.setPassword(null);

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(5L);
            req.setOriginPassword(null);
            req.setNewPassword("123"); // 仅数字，无字母

            when(blindMapper.selectById(eq(5L))).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.updateBlindPassword(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("origin 非法格式 → 抛 PARAMS_ERROR")
        void update_invalidOriginFormat_throws() {
            Blind existing = new Blind();
            existing.setBlindId(6L);
            existing.setPassword(PasswordUtils.hash("abc123"));

            BlindUpdatePasswordRequest req = new BlindUpdatePasswordRequest();
            req.setBlindId(6L);
            req.setOriginPassword("abcdef"); // 仅字母非法
            req.setNewPassword("xyz789");

            when(blindMapper.selectById(eq(6L))).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.updateBlindPassword(req));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }
    }

    // ==================== generateLoginToken ====================

    @Nested
    @DisplayName("generateLoginToken：构造 payload + JwtUtils + redis 90 天")
    class GenerateLoginToken {

        @Test
        @DisplayName("正常生成：token 入 redis 90 天，key 形如 REDIS_SECRETKEY-blind-{blindId}")
        void generateToken_putsToRedis90Days() {
            Blind blind = new Blind();
            blind.setBlindId(777L);
            blind.setPhone(VALID_PHONE);

            String token = blindService.generateLoginToken(blind);

            assertThat(token).isNotBlank();
            verify(redisUtils, times(1))
                    .setToRedis(eq("REDIS_SECRETKEY-blind-777"), eq(token), eq(90L));
        }
    }

    // ==================== getByPhone / getVolunteerByPhone / validatePassword（轻量覆盖） ====================

    @Nested
    @DisplayName("工具方法：getByPhone / getVolunteerByPhone / validatePassword")
    class UtilityMethods {

        @Test
        @DisplayName("getByPhone 合法手机号 → getOne → baseMapper.selectOne(qw, true) 返回 Blind")
        void getByPhone_valid_returnsBlind() {
            Blind b = new Blind();
            b.setBlindId(1L);
            b.setPhone(VALID_PHONE);
            when(blindMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(b);

            Blind result = blindService.getByPhone(VALID_PHONE);

            assertThat(result).isNotNull();
            assertThat(result.getBlindId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getByPhone 非法手机号 → 抛 PARAMS_ERROR")
        void getByPhone_invalid_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> blindService.getByPhone("abc"));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("getVolunteerByPhone 合法手机号 → volunteerMapper.selectOne 单参")
        void getVolunteerByPhone_valid_callsVolunteerMapper() {
            Volunteer v = new Volunteer();
            v.setVolunteerId(9L);
            when(volunteerMapper.selectOne(any(Wrapper.class))).thenReturn(v);

            Volunteer result = blindService.getVolunteerByPhone(VALID_PHONE);

            assertThat(result).isNotNull();
            assertThat(result.getVolunteerId()).isEqualTo(9L);
            verify(volunteerMapper, times(1)).selectOne(any(Wrapper.class));
        }

        @Test
        @DisplayName("validatePassword：'abc123' 合法 → true")
        void validatePassword_legal_true() {
            assertThat(blindService.validatePassword("abc123")).isTrue();
        }

        @Test
        @DisplayName("validatePassword：'abcdef' 无数字 → false")
        void validatePassword_noDigit_false() {
            assertThat(blindService.validatePassword("abcdef")).isFalse();
        }

        @Test
        @DisplayName("validatePassword：null → false")
        void validatePassword_null_false() {
            assertThat(blindService.validatePassword(null)).isFalse();
        }
    }
}
