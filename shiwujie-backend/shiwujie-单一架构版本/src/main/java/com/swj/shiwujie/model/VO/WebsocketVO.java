package com.swj.shiwujie.model.VO;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebsocketVO {

    private String title;
    private String blindUid;


    @Override
    public String toString() {
        return "WebsocketVO{" +
                "title='" + title + '\'' +
                ", blindUid='" + blindUid + '\'' +
                '}';
    }
}
