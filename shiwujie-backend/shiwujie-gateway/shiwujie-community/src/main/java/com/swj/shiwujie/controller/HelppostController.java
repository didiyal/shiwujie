package com.swj.shiwujie.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.helppost.HelppostVO;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;
import com.swj.shiwujie.service.HelppostService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 求助帖接口
 *
 * @author swj
 */
@RestController
@RequestMapping("/helppost")
@Api(tags = "求助帖接口")
public class HelppostController {

    @Resource
    private HelppostService helppostService;

    /**
     * 视障人士发出求助帖
     */
    @PostMapping("/add")
    @ApiOperation("视障人士发出求助帖")
    public BaseResponse<HelppostVO> addHelppost(@RequestBody HelppostAddRequest helppostAddRequest, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(helppostAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        // 获取登录视障人士ID
        Long loginBlindId = LoginUtils.getLoginBlindId(httpRequest);
        ThrowUtils.throwIf(loginBlindId == null || loginBlindId <= 0, ErrorCode.PARAMS_ERROR, "视障人士ID不合法");

        HelppostVO helppostVO = helppostService.addHelppost(helppostAddRequest,loginBlindId);
        return ResultUtils.success(helppostVO);
    }

    /**
     * 通过id查询求助帖VO
     */
    @GetMapping("/get")
    @ApiOperation("通过id查询求助帖")
    public BaseResponse<HelppostVO> getHelppostById(Long helppostId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(helppostId == null || helppostId <= 0, ErrorCode.PARAMS_ERROR, "求助帖ID不合法");

        HelppostVO helppostVO = helppostService.getHelppostVOById(helppostId);
        return ResultUtils.success(helppostVO);
    }

    /**
     * 分页选择查询社区下的求助帖
     */
    @GetMapping("/list")
    @ApiOperation("分页查询社区下的求助帖")
    public BaseResponse<Page<HelppostVO>> listHelppostsByCommunity(HelppostQueryRequest helppostQueryRequest, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(helppostQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Page<HelppostVO> helppostVOPage = helppostService.listHelppostsByCommunity(helppostQueryRequest);
        return ResultUtils.success(helppostVOPage);
    }

    /**
     * 删除求助帖
     */
    @DeleteMapping("/delete")
    @ApiOperation("删除求助帖")
    public BaseResponse<Boolean> deleteHelppost(Long helppostId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(helppostId == null || helppostId <= 0, ErrorCode.PARAMS_ERROR, "求助帖ID不合法");

        Long loginBlindId = LoginUtils.getLoginBlindId(httpRequest);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        ThrowUtils.throwIf(loginBlindId == null, ErrorCode.NOT_LOGIN, "未登录");

        boolean result = helppostService.deleteHelppost(helppostId, loginBlindId,loginVolunteerId);
        return ResultUtils.success(result);
    }

    /**
     * 修改求助帖信息
     */
    @PostMapping("/update")
    @ApiOperation("修改求助帖信息")
    public BaseResponse<Boolean> updateHelppost(@RequestBody HelppostUpdateRequest helppostUpdateRequest, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(helppostUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long loginBlindId = LoginUtils.getLoginBlindId(httpRequest);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);

        boolean result = helppostService.updateHelppost(helppostUpdateRequest, loginBlindId,loginVolunteerId);
        return ResultUtils.success(result);
    }

}
