package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.helppost.HelppostVO;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.community.Helppost;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.enums.community.PostStatusEnum;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.HelppostService;
import com.swj.shiwujie.mapper.HelppostMapper;
import com.swj.shiwujie.service.user.InnerBlindService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 求助帖服务实现类
 *
 * @author swj
 */
@Service
public class HelppostServiceImpl extends ServiceImpl<HelppostMapper, Helppost> implements HelppostService {


    @Resource
    private InnerBlindService innerBlindService;

    @Resource
    private CommunitymanagerService communitymanagerService;


    /**
     * 视障人士发出求助帖
     */
    @Override
    public HelppostVO addHelppost(HelppostAddRequest helppostAddRequest,Long loginBlindId) {
        // 参数校验
        ThrowUtils.throwIf(helppostAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        String helpContent = helppostAddRequest.getHelpContent();
        String helpLocation = helppostAddRequest.getHelpLocation();

        ThrowUtils.throwIf(helpContent == null || helpContent.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "求助内容不能为空");
        ThrowUtils.throwIf(helpLocation == null || helpLocation.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "求助地点不能为空");

        // 校验视障人士是否有社区
        Blind blind = innerBlindService.getById(loginBlindId);
        Long communityId = blind.getCommunityId();
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.NO_AUTH, "视障人士未加入任何社区，无法发布求助帖");

        // 创建求助帖
        Helppost helppost = new Helppost();
        helppost.setBlindId(loginBlindId);
        helppost.setCommunityId(communityId);
        helppost.setHelpContent(helpContent);
        helppost.setHelpLocation(helpLocation);
        helppost.setPostStatus(PostStatusEnum.WAITING.getPostStatus()); // 设置初始状态为待响应
        boolean saveResult = this.save(helppost);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "求助帖创建失败");

        return getHelppostVO(helppost);
    }

    /**
     * 通过id查询求助帖VO
     */
    @Override
    public HelppostVO getHelppostVOById(Long helppostId) {
        ThrowUtils.throwIf(helppostId == null || helppostId <= 0, ErrorCode.PARAMS_ERROR, "求助帖ID不合法");

        Helppost helppost = this.getById(helppostId);
        ThrowUtils.throwIf(helppost == null, ErrorCode.PARAMS_ERROR, "求助帖不存在");

        return getHelppostVO(helppost);
    }

    /**
     * 分页选择查询社区下的求助帖
     */
    @Override
    public Page<HelppostVO> listHelppostsByCommunity(HelppostQueryRequest helppostQueryRequest) {
        ThrowUtils.throwIf(helppostQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long communityId = helppostQueryRequest.getCommunityId();
        Long blindId = helppostQueryRequest.getBlindId();
        Long volunteerId = helppostQueryRequest.getVolunteerId();
        String postStatus = helppostQueryRequest.getPostStatus();
        long current = helppostQueryRequest.getCurrent();
        long size = helppostQueryRequest.getPageSize();

        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(current <= 0 || size <= 0 || size > 100, ErrorCode.PARAMS_ERROR, "分页参数不合法");

        // 构建查询条件
        QueryWrapper<Helppost> queryWrapper = new QueryWrapper<>();
        if(ObjUtil.isNotNull(communityId)){
            queryWrapper.eq("community_id", communityId);
        }
        if(ObjUtil.isNotNull(blindId)){
            queryWrapper.eq("blind_id", blindId);
        }
        if(ObjUtil.isNotNull(volunteerId)){
            queryWrapper.eq("volunteer_id", volunteerId);
        }


        // 状态转换：字符串转枚举
        if (postStatus != null && !postStatus.isEmpty()) {
            PostStatusEnum statusEnum = PostStatusEnum.getByName(postStatus);
            ThrowUtils.throwIf(statusEnum == null, ErrorCode.PARAMS_ERROR, "求助帖状态不合法");
            queryWrapper.eq("post_status", statusEnum.getPostStatus());
        }

        // 分页查询
        Page<Helppost> page = new Page<>(current, size);
        Page<Helppost> helppostPage = this.page(page, queryWrapper);

        // 转换为VO并返回
        Page<HelppostVO> helppostVOPage = new Page<>();
        BeanUtils.copyProperties(helppostPage, helppostVOPage);
        List<HelppostVO> helppostVOList = helppostPage.getRecords().stream()
                .map(this::getHelppostVO)
                .collect(Collectors.toList());
        helppostVOPage.setRecords(helppostVOList);
        return helppostVOPage;
    }

    /**
     * 删除求助帖
     */
    @Override
    public boolean deleteHelppost(Long helppostId, Long loginBlindId,Long loginVolunteerId) {
        ThrowUtils.throwIf(helppostId == null || helppostId <= 0, ErrorCode.PARAMS_ERROR, "求助帖ID不合法");
        ThrowUtils.throwIf(loginBlindId == null && loginVolunteerId == null, ErrorCode.NOT_LOGIN, "未登录");

        // 查询求助帖
        Helppost helppost = this.getById(helppostId);
        ThrowUtils.throwIf(helppost == null, ErrorCode.PARAMS_ERROR, "求助帖不存在");

        // 权限检查：作者(盲人本人) 或 本社区注册人/管理员（员工无权）
        boolean isAuthor = loginBlindId != null && loginBlindId.equals(helppost.getBlindId());
        boolean isAdmin = false;
        if (!isAuthor && loginVolunteerId != null) {
            Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(loginVolunteerId, helppost.getCommunityId());
            isAdmin = communitymanager != null
                    && communitymanager.getRolePermissionId() != CommunityRolePermissionEnum.EMPLOYEE.getRoleId();
        }
        ThrowUtils.throwIf(!isAuthor && !isAdmin, ErrorCode.NO_AUTH, "无权限删除该求助帖");

        return this.removeById(helppostId);
    }

    /**
     * 修改求助帖信息
     */
    @Override
    public boolean updateHelppost(HelppostUpdateRequest helppostUpdateRequest, Long loginBlindId,Long loginVolunteerId) {
        ThrowUtils.throwIf(helppostUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long helppostId = helppostUpdateRequest.getHelppostId();
        Long volunteerId = helppostUpdateRequest.getVolunteerId();
        String postStatus = helppostUpdateRequest.getPostStatus();
        String helpContent = helppostUpdateRequest.getHelpContent();
        String helpLocation = helppostUpdateRequest.getHelpLocation();

        ThrowUtils.throwIf(helppostId == null || helppostId <= 0, ErrorCode.PARAMS_ERROR, "求助帖ID不合法");

        // 查询求助帖
        Helppost helppost = this.getById(helppostId);
        ThrowUtils.throwIf(helppost == null, ErrorCode.PARAMS_ERROR, "求助帖不存在");

        // 权限检查：作者(盲人本人) 或 本社区注册人/管理员（员工无权）
        ThrowUtils.throwIf(loginBlindId == null && loginVolunteerId == null, ErrorCode.NOT_LOGIN, "未登录");
        boolean isAuthor = loginBlindId != null && loginBlindId.equals(helppost.getBlindId());
        boolean isAdmin = false;
        if (!isAuthor && loginVolunteerId != null) {
            Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(loginVolunteerId, helppost.getCommunityId());
            isAdmin = communitymanager != null
                    && communitymanager.getRolePermissionId() != CommunityRolePermissionEnum.EMPLOYEE.getRoleId();
        }
        ThrowUtils.throwIf(!isAuthor && !isAdmin, ErrorCode.NO_AUTH, "无权限修改该求助帖");

        // 更新字段
        if (helpContent != null && !helpContent.isEmpty()) {
            helppost.setHelpContent(helpContent);
        }
        if (helpLocation != null && !helpLocation.isEmpty()) {
            helppost.setHelpLocation(helpLocation);
        }
        if (volunteerId != null && ObjUtil.isNotNull(volunteerId)) {
            helppost.setVolunteerId(volunteerId);
        }
        if (postStatus != null && !postStatus.isEmpty()) {
            PostStatusEnum statusEnum = PostStatusEnum.getByName(postStatus);
            ThrowUtils.throwIf(statusEnum == null, ErrorCode.PARAMS_ERROR, "求助帖状态不合法");
            helppost.setPostStatus(statusEnum.getPostStatus());
        }

        return this.updateById(helppost);
    }

    /**
     * 封装求助帖VO
     */
    @Override
    public HelppostVO getHelppostVO(Helppost helppost) {
        HelppostVO vo = new HelppostVO();
        vo.setCommunityId(helppost.getCommunityId());
        vo.setVolunteerId(helppost.getVolunteerId());
        vo.setHelppostId(helppost.getHelppostId());
        vo.setBlindId(helppost.getBlindId());
        vo.setHelpContent(helppost.getHelpContent());
        vo.setHelpLocation(helppost.getHelpLocation());
        // 设置状态描述
        vo.setPostStatus(helppost.getPostStatus());
        return vo;
    }

}




