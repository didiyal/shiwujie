package com.swj.shiwujie.model.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallHelpJoinRequest {
    /**
     * 要通话的盲人的uid
     */
    private String blindUid;
}
