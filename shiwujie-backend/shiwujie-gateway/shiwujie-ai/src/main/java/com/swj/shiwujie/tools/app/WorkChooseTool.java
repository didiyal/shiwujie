package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.app.ComplexProblemApp;
import com.swj.shiwujie.app.EasyProblemApp;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;


/**
 * 简单抉择工具(仅供问题评估助手使用)
 */
@Slf4j
//@Component
public class WorkChooseTool {

    // 简单问题处理AI
    private EasyProblemApp easyProblemApp;
    // 复杂问题处理AI
    private ComplexProblemApp complexProblemApp;

    public WorkChooseTool(@Lazy EasyProblemApp easyProblemApp,@Lazy ComplexProblemApp complexProblemApp){

        this.easyProblemApp = easyProblemApp;
        this.complexProblemApp = complexProblemApp;
    }


    /**
     *  问题评估(简单任务)
     * @param text 用户输入文本
     * @param imageUrl 图片URL
     * @param blindId 盲人ID
     */
    @Tool(description = "问题评估-简单任务(仅有问题评估助手可以使用)",returnDirect = true)
    public String easyWorkChoose(@ToolParam (description = "用户输入文本(与imageUrl互斥)") String text,
                               @ToolParam (description = "图片URL(与text互斥)") String imageUrl,
                               @ToolParam (description = "盲人ID(必须携带)") Long blindId) {

        log.info("easyWorkTool start");
        try {
            // 必须携带blindId
            ThrowUtils.throwIf(ObjUtil.isNull(blindId), ErrorCode.PARAMS_ERROR,"请携带盲人ID");
            // text 与 imageUrl 不能同时为空
            ThrowUtils.throwIf(ObjUtil.isEmpty(text) && ObjUtil.isEmpty(imageUrl), ErrorCode.PARAMS_ERROR,"请输入文字或图片");


            String res = "";
            // 调用模型
            if(ObjUtil.isNotEmpty(text)){
                res = this.easyProblemApp.doChatWithText(text,blindId);
            } else if (ObjUtil.isNotNull(imageUrl)) {
                res = this.easyProblemApp.doChatWithImage(imageUrl,blindId);
            }
            return res;
        } catch (Exception e) {
            // 出错不能终止ai运行
            log.error("easyWorkTool error", e);
            return e.getMessage();
        }
    }


    /**
     * 问题评估(复杂任务)
     * @param text
     * @param blindId
     * @return
     */
    @Tool(description = "问题评估-复杂任务(仅有问题评估助手可以使用)",returnDirect = true)
    public String complexWorkChoose(@ToolParam (description = "用户输入文本(必须携带)") String text,
                                 @ToolParam (description = "盲人ID(必须携带)") Long blindId) {

        log.info("complexWorkTool start");
        try {
            // 必须携带blindId
            ThrowUtils.throwIf(ObjUtil.isNull(blindId), ErrorCode.PARAMS_ERROR,"请携带盲人ID");

            String res = "";
            res =  this.complexProblemApp.doChatWithText(text, blindId);
            return res;
        } catch (Exception e) {
            // 出错不能终止ai运行
            log.error("easyWorkTool error", e);
            return e.getMessage();
        }
    }
    
    
    
}
