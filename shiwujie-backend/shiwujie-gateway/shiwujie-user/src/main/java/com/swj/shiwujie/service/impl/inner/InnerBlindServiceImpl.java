package com.swj.shiwujie.service.impl.inner;

import cn.hutool.core.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.InnerBlindService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;


/**
 * @author Administrator
 * @description 针对表【Blind(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@DubboService
public class InnerBlindServiceImpl implements InnerBlindService {



    @Resource
    private BlindService blindService;


    /**
     * 通过手机号查询信息
     *
     * @param id id
     * @return 信息
     */
    @Override
    public Blind getById(Long id) {
        if(ObjUtil.isNull(id)){
            return null;
        }
        return blindService.getById(id);
    }

    /**
     * 通过手机号查询用户(视障人士)信息
     *
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    @Override
    public Blind getByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("phone", phone);
        return blindService.getOne(blindQueryWrapper);
    }


}




