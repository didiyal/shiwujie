package com.swj.shiwujie.model.VO.call;


import com.swj.shiwujie.model.request.call.SocketData;
import lombok.Data;

@Data
public class SocketVO implements java.io.Serializable{


    private static final long serialVersionUID = 3957989681641040018L;
    /**
     * 状态码
     */
    private Integer code;

    /**
     * 描述
     */
    private String message;


    /**
     * 请求内容
     */
    private SocketData socketData;

}
