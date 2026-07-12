package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallConstant;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.VideohelpMapper;
import com.swj.shiwujie.model.domain.call.Videohelp;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.RedisUtils;
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

import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VideohelpServiceImpl 单元测试。
 * 纯 Mockito：@Mock 三个 @Resource/@Autowired 字段 + baseMapper（ServiceImpl 走 baseMapper）。
 * RedisUtils 用 @Mock；CoordinationSocketHandler 用 @Mock，static sessionMap 不被触发。
 *
 * 注意源码缺陷（见 known-issues）：
 *  - removeVolunteerFromVideohelp 在 hasKey=FALSE 分支 queue=null 却调 queue.contains → NPE。
 *    本测试只覆盖 hasKey=TRUE 正常路径，不覆盖该 NPE 分支。
 *  - joinVideohelp 末尾 coordinationSocketHandler.matchSuccess 内部读 static sessionMap，
 *    但此处 handler 被 @Mock 隔离（不执行真实方法），故可单测。
 */
@ExtendWith(MockitoExtension.class)
class VideohelpServiceImplTest {

    @Mock
    private VideohelpMapper baseMapper;

    @Mock
    private InnerVolunteerService innerVolunteerService;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private CoordinationSocketHandler coordinationSocketHandler;

    @InjectMocks
    private VideohelpServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 父类的 baseMapper 字段，@InjectMocks 不可靠注入，反射强制塞入。
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    // ===================== createVideohelp =====================

    @Nested
    @DisplayName("createVideohelp：志愿者加入匹配")
    class CreateVideohelp {

        @Test
        @DisplayName("新队列（hasKey=false）：成功加入、save 被调、状态=WAITING、返 true")
        void newQueue_success() {
            Long volunteerId = 7001L;
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.FALSE);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null); // 不在 HELPING
            when(baseMapper.insert(any(Videohelp.class))).thenReturn(1);

            boolean result = service.createVideohelp(volunteerId, "13900000001");

            assertThat(result).isTrue();

            // 校验 save 写入字段
            ArgumentCaptor<Videohelp> captor = ArgumentCaptor.forClass(Videohelp.class);
            verify(baseMapper).insert(captor.capture());
            Videohelp saved = captor.getValue();
            assertThat(saved.getVolunteerId()).isEqualTo(volunteerId);
            assertThat(saved.getChannelId()).isEqualTo(volunteerId);
            assertThat(saved.getHelpStatus()).isEqualTo(CallHelpStatusEnum.WAITING.getHelpStatus());
            assertThat(saved.getStartTime()).isNotNull();

            // 队列写回 redis（30 天）
            verify(redisUtils).setToRedis(eq(CallConstant.VOLUNTEER_QUEUE_REDIS), any(), eq(30L));
        }

        @Test
        @DisplayName("已在 HELPING 通话中（getWaitingByVolunteerId 命中）→ 抛 PARAMS_ERROR")
        void alreadyHelping_throws() {
            Long volunteerId = 7002L;
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.FALSE);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(new Videohelp());

            assertThatThrownBy(() -> service.createVideohelp(volunteerId, "13900000002"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());

            // 失败路径不应写库
            verify(baseMapper, never()).insert(any(Videohelp.class));
        }

        @Test
        @DisplayName("已有队列且已含该志愿者 → 抛 PARAMS_ERROR（已在匹配中）")
        void existingQueue_containsVolunteer_throws() {
            Long volunteerId = 7003L;
            Queue<Long> queue = new LinkedList<>();
            queue.offer(volunteerId);
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.TRUE);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);

            assertThatThrownBy(() -> service.createVideohelp(volunteerId, "13900000003"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());

            verify(baseMapper, never()).insert(any(Videohelp.class));
        }

        @Test
        @DisplayName("已有队列但不含该志愿者 → 成功加入，save 被调、返 true")
        void existingQueue_success() {
            Long volunteerId = 7004L;
            Queue<Long> queue = new LinkedList<>(); // 空队列，不含目标
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.TRUE);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);
            when(baseMapper.insert(any(Videohelp.class))).thenReturn(1);

            boolean result = service.createVideohelp(volunteerId, "13900000004");

            assertThat(result).isTrue();
            verify(baseMapper).insert(any(Videohelp.class));
            verify(redisUtils).setToRedis(eq(CallConstant.VOLUNTEER_QUEUE_REDIS), any(), eq(30L));
        }
    }

    // ===================== removeVolunteerFromVideohelp =====================

    @Nested
    @DisplayName("removeVolunteerFromVideohelp：志愿者退出匹配")
    class RemoveVolunteer {

        @Test
        @DisplayName("志愿者不在队列中 → 抛 PARAMS_ERROR")
        void notInQueue_throws() {
            Long volunteerId = 8001L;
            Queue<Long> queue = new LinkedList<>(); // 空队列
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.TRUE);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);

            assertThatThrownBy(() -> service.removeVolunteerFromVideohelp(volunteerId, "13900000011"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());

            verify(baseMapper, never()).updateById(any(Videohelp.class));
        }

        @Test
        @DisplayName("在队列中但无 WAITING 记录 → 抛 PARAMS_ERROR")
        void inQueueButNoWaiting_throws() {
            Long volunteerId = 8002L;
            Queue<Long> queue = new LinkedList<>();
            queue.offer(volunteerId);
            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.TRUE);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

            assertThatThrownBy(() -> service.removeVolunteerFromVideohelp(volunteerId, "13900000012"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("成功退出：状态置 FALL、updateById 被调、返 true")
        void success() {
            Long volunteerId = 8003L;
            Queue<Long> queue = new LinkedList<>();
            queue.offer(volunteerId);
            Videohelp waiting = new Videohelp();
            waiting.setVolunteerId(volunteerId);
            waiting.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());

            when(redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(Boolean.TRUE);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(waiting);
            when(baseMapper.updateById(any(Videohelp.class))).thenReturn(1);

            boolean result = service.removeVolunteerFromVideohelp(volunteerId, "13900000013");

            assertThat(result).isTrue();

            ArgumentCaptor<Videohelp> captor = ArgumentCaptor.forClass(Videohelp.class);
            verify(baseMapper).updateById(captor.capture());
            assertThat(captor.getValue().getHelpStatus())
                    .isEqualTo(CallHelpStatusEnum.FALL.getHelpStatus());

            verify(redisUtils).setToRedis(eq(CallConstant.VOLUNTEER_QUEUE_REDIS), any(), eq(30L));
        }
    }

    // ===================== joinVideohelp =====================

    @Nested
    @DisplayName("joinVideohelp：视障人士加入匹配")
    class JoinVideohelp {

        @Test
        @DisplayName("Redis 无志愿者队列 → 抛 PARAMS_ERROR（没有空闲志愿者）")
        void noVolunteer_throws() {
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(null);

            assertThatThrownBy(() -> service.joinVideohelp(6001L, "13800000031"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());

            verify(baseMapper, never()).updateById(any(Videohelp.class));
        }

        @Test
        @DisplayName("poll 出的志愿者无 WAITING 记录 → 抛 PARAMS_ERROR")
        void volunteerNotWaiting_throws() {
            Long volunteerId = 9001L;
            Queue<Long> queue = new LinkedList<>();
            queue.offer(volunteerId);
            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

            assertThatThrownBy(() -> service.joinVideohelp(6002L, "13800000032"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("成功匹配：状态置 HELPING、写 blindId/channelId/responseTime、socket 转发、updateById 被调")
        void success() {
            Long volunteerId = 9002L;
            Long blindId = 6003L;
            String loginUserPhone = "13800000033";
            Queue<Long> queue = new LinkedList<>();
            queue.offer(volunteerId);
            Videohelp waiting = new Videohelp();
            waiting.setVolunteerId(volunteerId);
            waiting.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());
            Volunteer volunteer = new Volunteer();
            volunteer.setVolunteerId(volunteerId);
            volunteer.setPhone("13900000033");

            when(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS)).thenReturn(queue);
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(waiting);
            when(innerVolunteerService.getById(volunteerId)).thenReturn(volunteer);
            when(baseMapper.updateById(any(Videohelp.class))).thenReturn(1);

            boolean result = service.joinVideohelp(blindId, loginUserPhone);

            assertThat(result).isTrue();

            ArgumentCaptor<Videohelp> captor = ArgumentCaptor.forClass(Videohelp.class);
            verify(baseMapper).updateById(captor.capture());
            Videohelp updated = captor.getValue();
            assertThat(updated.getBlindId()).isEqualTo(blindId);
            assertThat(updated.getChannelId()).isEqualTo(volunteerId);
            assertThat(updated.getHelpStatus()).isEqualTo(CallHelpStatusEnum.HELPING.getHelpStatus());
            assertThat(updated.getResponseTime()).isNotNull();

            // socket 匹配成功被调（@Mock 隔离，不触发 static sessionMap）
            verify(coordinationSocketHandler).matchSuccess(any(SocketData.class));
            // 队列回写 redis
            verify(redisUtils).setToRedis(eq(CallConstant.VOLUNTEER_QUEUE_REDIS), any(), eq(30L));
        }
    }

    // ===================== 工具方法 =====================

    @Nested
    @DisplayName("工具查询方法（null 入参短路 + baseMapper）")
    class QueryHelpers {

        @Test
        @DisplayName("getByVolunteerId(null) → 返 null，不查 mapper")
        void getByVolunteerId_null() {
            assertThat(service.getByVolunteerId(null)).isNull();
            verify(baseMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("getWaitingByVolunteerId(null) → 返 null")
        void getWaitingByVolunteerId_null() {
            assertThat(service.getWaitingByVolunteerId(null)).isNull();
        }

        @Test
        @DisplayName("getByBlindId(null) → 返 null")
        void getByBlindId_null() {
            assertThat(service.getByBlindId(null)).isNull();
        }

        @Test
        @DisplayName("getByVolunteerId(id) → 走 baseMapper.selectOne")
        void getByVolunteerId_normal() {
            Videohelp expect = new Videohelp();
            when(baseMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(expect);
            assertThat(service.getByVolunteerId(1L)).isSameAs(expect);
        }

        @Test
        @DisplayName("getHelpingByVolunteerId(null) → 返 null，不查 mapper")
        void getHelpingByVolunteerId_null() {
            assertThat(service.getHelpingByVolunteerId(null)).isNull();
            verify(baseMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("getHelpingByVolunteerId(id) → 走 baseMapper.selectList")
        void getHelpingByVolunteerId_normal() {
            when(baseMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(java.util.Collections.emptyList());
            assertThat(service.getHelpingByVolunteerId(2L)).isEmpty();
        }

        @Test
        @DisplayName("getHelpingByBlindId(null) → 返 null，不查 mapper")
        void getHelpingByBlindId_null() {
            assertThat(service.getHelpingByBlindId(null)).isNull();
            verify(baseMapper, never()).selectList(any());
        }
    }
}
