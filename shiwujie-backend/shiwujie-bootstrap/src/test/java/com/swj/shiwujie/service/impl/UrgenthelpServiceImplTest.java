package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.UrgenthelpMapper;
import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UrgenthelpServiceImpl 单元测试。
 * 纯 Mockito：@Mock 三个 @Resource 字段 + baseMapper（ServiceImpl 走 baseMapper）。
 * 不起 Spring、不连 MySQL；CoordinationSocketHandler 被 mock 隔离，static sessionMap 不会被触发。
 *
 * <p><b>MyBatis-Plus 调用链说明（已反编译 MP 3.5.9 字节码核实）：</b>
 * <ul>
 *   <li>{@code this.getOne(qw)} → {@code baseMapper.selectOne(qw, true)} <b>两参</b>（default 方法，mock 默认返 null）。
 *       stub 必须用两参 {@code selectOne(any(QueryWrapper.class), anyBoolean())}。</li>
 *   <li>{@code this.save(e)} → {@code baseMapper.insert(e)}；{@code this.updateById(e)} → {@code baseMapper.updateById(e)}。</li>
 *   <li>MP 3.5.9 的 {@code ServiceImpl.getBaseMapper()} 会断言 baseMapper != null；@InjectMocks 对**继承字段**
 *       注入不可靠，必须手动反射注入父类 baseMapper 字段（见 {@link #injectBaseMapper()}）。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class UrgenthelpServiceImplTest {

    @Mock
    private UrgenthelpMapper baseMapper;

    @Mock
    private InnerVolunteerService innerVolunteerService;

    @Mock
    private InnerBlindService innerBlindService;

    @Mock
    private CoordinationSocketHandler coordinationSocketHandler;

    @InjectMocks
    private UrgenthelpServiceImpl service;

    /**
     * 反射强制注入 ServiceImpl 父类 baseMapper 字段（@InjectMocks 对继承字段注入不可靠），
     * 否则 {@code this.getOne(...)} 走到 {@code getBaseMapper()} 抛 "baseMapper can not be null"。
     */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    // ===================== createUrgenthelp =====================

    @Nested
    @DisplayName("createUrgenthelp：盲人发起紧急求助")
    class CreateUrgenthelp {

        @Test
        @DisplayName("已在 WAITING 求助中 → 抛 PARAMS_ERROR")
        void waitingExists_throws() {
            Long blindId = 1001L;
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean()))
                    .thenReturn(new Urgenthelp()); // getWaitingByBlindId 命中

            assertThatThrownBy(() -> service.createUrgenthelp(blindId, "13800000001"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("已在 HELPING 求助中 → 抛 PARAMS_ERROR")
        void helpingExists_throws() {
            Long blindId = 1002L;
            // 第一次 getWaitingByBlindId 返 null，第二次 getHelpingByBlindId 命中
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean()))
                    .thenReturn(null)
                    .thenReturn(new Urgenthelp());

            assertThatThrownBy(() -> service.createUrgenthelp(blindId, "13800000002"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("盲人未加入家庭（familyId=null）→ 抛 PARAMS_ERROR")
        void noFamily_throws() {
            Long blindId = 1003L;
            Blind blind = new Blind();
            blind.setBlindId(blindId);
            blind.setFamilyId(null);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null); // waiting/helping 都无
            when(innerBlindService.getById(blindId)).thenReturn(blind);

            assertThatThrownBy(() -> service.createUrgenthelp(blindId, "13800000003"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("成功创建：save 被调、socket 转发被调、状态=WAITING、返 true")
        void success() {
            Long blindId = 1004L;
            Long familyId = 9001L;
            Blind blind = new Blind();
            blind.setBlindId(blindId);
            blind.setFamilyId(familyId);
            blind.setPhone("13800000004");
            List<Volunteer> familyMembers = Collections.emptyList();

            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);
            when(innerBlindService.getById(blindId)).thenReturn(blind);
            when(innerVolunteerService.getListByFamilyId(familyId)).thenReturn(familyMembers);
            when(baseMapper.insert(any(Urgenthelp.class))).thenReturn(1);

            boolean result = service.createUrgenthelp(blindId, "13800000004");

            assertThat(result).isTrue();

            // 校验 save 写入的字段
            ArgumentCaptor<Urgenthelp> captor = ArgumentCaptor.forClass(Urgenthelp.class);
            verify(baseMapper).insert(captor.capture());
            Urgenthelp saved = captor.getValue();
            assertThat(saved.getBlindId()).isEqualTo(blindId);
            assertThat(saved.getFamilyId()).isEqualTo(familyId);
            assertThat(saved.getHelpStatus())
                    .isEqualTo(CallHelpStatusEnum.WAITING.getHelpStatus());
            assertThat(saved.getStartTime()).isNotNull();

            // socket 转发被调（空 list，不会触发 static sessionMap）
            verify(coordinationSocketHandler).urgenthelpToFamily(eq(familyMembers), any(SocketData.class));
        }
    }

    // ===================== removeFromUrgenthelp =====================

    @Nested
    @DisplayName("removeFromUrgenthelp：盲人取消求助")
    class RemoveFromUrgenthelp {

        @Test
        @DisplayName("无 WAITING 记录（未求助）→ 抛 PARAMS_ERROR")
        void notWaiting_throws() {
            Long blindId = 2001L;
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

            assertThatThrownBy(() -> service.removeFromUrgenthelp(blindId, "13800000011"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("成功取消：状态置 FALL、updateById 被调、socket 通知被调")
        void success() {
            Long blindId = 2002L;
            Long familyId = 9002L;
            Urgenthelp existing = new Urgenthelp();
            existing.setBlindId(blindId);
            existing.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());
            Blind blind = new Blind();
            blind.setBlindId(blindId);
            blind.setFamilyId(familyId);
            blind.setPhone("13800000012");

            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(existing);
            when(innerBlindService.getById(blindId)).thenReturn(blind);
            when(innerVolunteerService.getListByFamilyId(familyId)).thenReturn(Collections.emptyList());
            when(baseMapper.updateById(any(Urgenthelp.class))).thenReturn(1);

            boolean result = service.removeFromUrgenthelp(blindId, "13800000012");

            assertThat(result).isTrue();

            ArgumentCaptor<Urgenthelp> captor = ArgumentCaptor.forClass(Urgenthelp.class);
            verify(baseMapper).updateById(captor.capture());
            assertThat(captor.getValue().getHelpStatus())
                    .isEqualTo(CallHelpStatusEnum.FALL.getHelpStatus());

            verify(coordinationSocketHandler).cancelUrgenthelp(any(), any(SocketData.class));
        }
    }

    // ===================== joinUrgenthelp =====================

    @Nested
    @DisplayName("joinUrgenthelp：家属加入求助")
    class JoinUrgenthelp {

        @Test
        @DisplayName("对方未在求助（无 WAITING 记录）→ 抛 PARAMS_ERROR")
        void targetNotWaiting_throws() {
            String blindPhone = "13800000021";
            Long volunteerId = 3001L;
            Blind blind = new Blind();
            blind.setPhone(blindPhone);
            blind.setBlindId(5001L);
            when(innerBlindService.getByPhone(blindPhone)).thenReturn(blind);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

            assertThatThrownBy(() -> service.joinUrgenthelp(blindPhone, volunteerId, "13900000021"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("成功加入：状态置 HELPING、写 volunteerId/responseTime/channelId、updateById 被调")
        void success() {
            String blindPhone = "13800000022";
            Long volunteerId = 3002L;
            Long blindId = 5002L;
            Blind blind = new Blind();
            blind.setBlindId(blindId);
            blind.setPhone(blindPhone);
            Urgenthelp waiting = new Urgenthelp();
            waiting.setBlindId(blindId);
            waiting.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());

            when(innerBlindService.getByPhone(blindPhone)).thenReturn(blind);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(waiting);
            when(baseMapper.updateById(any(Urgenthelp.class))).thenReturn(1);

            boolean result = service.joinUrgenthelp(blindPhone, volunteerId, "13900000022");

            assertThat(result).isTrue();

            ArgumentCaptor<Urgenthelp> captor = ArgumentCaptor.forClass(Urgenthelp.class);
            verify(baseMapper).updateById(captor.capture());
            Urgenthelp updated = captor.getValue();
            assertThat(updated.getVolunteerId()).isEqualTo(volunteerId);
            assertThat(updated.getChannelId()).isEqualTo(volunteerId);
            assertThat(updated.getHelpStatus())
                    .isEqualTo(CallHelpStatusEnum.HELPING.getHelpStatus());
            assertThat(updated.getResponseTime()).isNotNull();
        }
    }

    // ===================== 工具方法（走 baseMapper） =====================

    @Nested
    @DisplayName("工具查询方法（null 入参短路 + baseMapper 查询）")
    class QueryHelpers {

        @Test
        @DisplayName("getByVolunteerId(null) → 直接返 null，不查 mapper")
        void getByVolunteerId_null() {
            assertThat(service.getByVolunteerId(null)).isNull();
            verify(baseMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("getByVolunteerId(id) → 走 baseMapper.selectOne")
        void getByVolunteerId_normal() {
            Urgenthelp expect = new Urgenthelp();
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(expect);

            assertThat(service.getByVolunteerId(1L)).isSameAs(expect);
        }

        @Test
        @DisplayName("getWaitingByVolunteerId(null) → 返 null，不查 mapper")
        void getWaitingByVolunteerId_null() {
            assertThat(service.getWaitingByVolunteerId(null)).isNull();
            verify(baseMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("getHelpingByVolunteerId(null) → 返 null，不查 mapper")
        void getHelpingByVolunteerId_null() {
            assertThat(service.getHelpingByVolunteerId(null)).isNull();
        }

        @Test
        @DisplayName("getByBlindId(null) → 返 null，不查 mapper")
        void getByBlindId_null() {
            assertThat(service.getByBlindId(null)).isNull();
        }

        @Test
        @DisplayName("getWaitingByBlindId(null) → 返 null，不查 mapper")
        void getWaitingByBlindId_null() {
            assertThat(service.getWaitingByBlindId(null)).isNull();
        }

        @Test
        @DisplayName("getHelpingByBlindId(null) → 返 null，不查 mapper")
        void getHelpingByBlindId_null() {
            assertThat(service.getHelpingByBlindId(null)).isNull();
        }

        @Test
        @DisplayName("getByVolunteerId 正常路径：校验 selectOne 仅调一次")
        void getByVolunteerId_invokesSelectOnce() {
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);
            service.getByVolunteerId(99L);
            verify(baseMapper, times(1)).selectOne(any(QueryWrapper.class), anyBoolean());
        }
    }
}
