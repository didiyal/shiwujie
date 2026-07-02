package com.swj.shiwujie.interceptor;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;
import static com.swj.shiwujie.constants.UserConstants.TOKEN_SECRETKEY;


@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {


    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @DubboReference
    private InnerBlindService innerBlindService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 放行OPTIONS请求
        if (HttpMethod.OPTIONS.toString().equals(req.getMethod())) {
            return true;
        }
        String url = req.getRequestURL().toString();
        log.info("------------------------------------------------------");
        log.info("请求的url: {}", url);
        // 获取Authorization请求头中的令牌（token）
        String header = req.getHeader("Authorization");
//        ThrowUtils.throwIf(header == null || !header.startsWith("Bearer "), ErrorCode.NOT_LOGIN, "未登录");

        // 默认用户登录
        if(header == null || !header.startsWith("Bearer ")){
            Blind blind = new Blind();
            // 默认用户登录
            blind.setBlindId(1L);
            blind.setPhone("19872250169");
            req.setAttribute("loginBlind", blind);
            req.setAttribute("phone",blind.getPhone());
            return true;
        }
        // 提取 token 如果token为空，返回未登录信息
        String token = header.replace("Bearer ", "");
        ThrowUtils.throwIf(!StringUtils.hasLength(token), ErrorCode.NOT_LOGIN, "未登录");


        Long blindId = null;
        String phone = null;
        // 解析JWT令牌
        try {
            Map<String, Object> map = JwtUtils.validateToken(token, TOKEN_SECRETKEY, true);
            blindId = Long.parseLong(map.get("blindId").toString());
            phone = map.get("phone").toString();

        } catch (Exception e) {
            log.info("解析令牌失败, 返回未登录错误信息");
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        // 检查 Redis 中的令牌信息
        Object fromRedisObj = redisTemplate.opsForValue().get(REDIS_SECRETKEY + "-blind-" + blindId );
        ThrowUtils.throwIf(fromRedisObj == null,ErrorCode.NOT_LOGIN, "未登录");

        // 比对token是否相同
        String tokenFromRedis = (String) fromRedisObj;
        ThrowUtils.throwIf(!token.equals(tokenFromRedis),ErrorCode.NOT_LOGIN, "未登录");

        // 续期 Redis 中的用户信息
        redisTemplate.expire(REDIS_SECRETKEY+"-"+ blindId, 1L, TimeUnit.DAYS);

        Blind blind = innerBlindService.getById(blindId);

        // 将用户信息添加到请求中
        req.setAttribute("loginBlind", blind);
        req.setAttribute("phone",phone);

        // 放行请求
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}

