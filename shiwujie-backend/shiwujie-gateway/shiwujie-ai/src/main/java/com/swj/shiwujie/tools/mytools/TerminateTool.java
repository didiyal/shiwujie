package com.swj.shiwujie.tools.mytools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TerminateTool {

    /**
     * 终结工具
     * @return
     */
    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)  
    public String doTerminate() {  
        return "任务结束";  
    }

    /**
     * 直接回答工具
     * @param answer 需要直接返回的答案内容
     * @return 返回答案内容
     */
    @Tool(description = """
            Directly answer the user's question or provide a response without further processing.
            Use this tool when you have a clear and final answer to provide.
            """)
    public String doAnswer(@ToolParam(description = "The answer content to be returned directly") String answer) {
        return answer;
    }

}
