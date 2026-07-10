package com.swj.shiwujie.interceptor;

import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;
import static com.swj.shiwujie.constants.UserConstants.TOKEN_SECRETKEY;


@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {


    @Resource
    private RedisUtils redisUtils;


    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 放行OPTIONS请求
        if (HttpMethod.OPTIONS.toString().equals(req.getMethod())) {
            return true;
        }
        String url = req.getRequestURL().toString();
        log.info("------------------------------------------------------");
        log.info("请求的url: {}", url);

        // 判断请求url中是否包含login，如果包含，说明是登录操作，放行。
        if(url.contains("loginAndRegister") || url.contains("Login") || url.contains("Register")) {
            log.info("登录或者注册操作, 放行...");
            return true;
        }

        // 获取Authorization请求头中的令牌（token）
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 提取 token
        String token = header.replace("Bearer ", "");

        // 如果token为空，返回未登录信息
        if (!StringUtils.hasLength(token)) {
            log.info("请求头Authorization为空,返回未登录的信息");
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }



        Long blindId = null;
        Long volunteerID = null;
        String phone = null;
        Long role = null;
        // 解析JWT令牌
        try {
            Map<String, Object> map = JwtUtils.validateToken(token, TOKEN_SECRETKEY, true);
            if((boolean)map.get("isBlind")){
                blindId = Long.parseLong(map.get("blindId").toString());
            }else{
                volunteerID = Long.parseLong(map.get("volunteerId").toString());
            }
            phone = map.get("phone").toString();
            if(ObjUtil.isNotNull(map.get("role"))){
                role = Long.parseLong(map.get("role").toString());
            }

        } catch (Exception e) {
            log.info("解析令牌失败, 返回未登录错误信息");
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 检查 Redis 中的令牌信息(读与续期共用同一 key,避免拼接不一致)
        String redisKey = REDIS_SECRETKEY + (blindId != null ? "-blind-"+blindId : "-volunteer-"+volunteerID);
        Object fromRedisObj = redisUtils.getFromRedis(redisKey);
        ThrowUtils.throwIf(fromRedisObj == null,ErrorCode.NOT_LOGIN, "未登录");

        // 比对token是否相同
        String tokenFromRedis = (String) fromRedisObj;
        ThrowUtils.throwIf(!token.equals(tokenFromRedis),ErrorCode.NOT_LOGIN, "未登录");


        // 续期 Redis 中的用户信息(滑动会话,对齐登录 90 天)
        redisUtils.renewKey(redisKey, 90L);



        // 将用户信息添加到请求中
        if(blindId!=null)

            req.setAttribute("loginBlindId", blindId);
        else
            req.setAttribute("loginVolunteerId",volunteerID);

        req.setAttribute("phone",phone);
        req.setAttribute("role",role);

        // 放行请求
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}

