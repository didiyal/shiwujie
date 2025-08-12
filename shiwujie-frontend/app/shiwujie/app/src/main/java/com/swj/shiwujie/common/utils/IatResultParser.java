package com.swj.shiwujie.common.utils;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 讯飞语音识别结果解析工具类
 * 按照官方文档要求解析语音听写返回的JSON结果
 */
public class IatResultParser {
    private static final String TAG = "IatResultParser";
    
    /**
     * 解析讯飞语音识别结果
     * 按照官方文档要求解析JSON格式
     * 
     * @param json 讯飞返回的JSON字符串
     * @return 解析后的文本内容
     */
    public static String parseIatResult(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        
        StringBuffer ret = new StringBuffer();
        
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);
            
            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
                // 如果需要多候选结果，解析数组其他字段
                // for(int j = 0; j < items.length(); j++) {
                //     JSONObject obj = items.getJSONObject(j);
                //     ret.append(obj.getString("w"));
                // }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析讯飞语音识别结果失败: " + e.getMessage(), e);
            // 如果解析失败，返回原始JSON字符串
            return json;
        }
        
        return ret.toString();
    }
    
    /**
     * 检查是否为最终结果
     * 按照官方文档要求判断识别是否完成
     * 
     * @param json 讯飞返回的JSON字符串
     * @return true表示是最终结果，false表示是中间结果
     */
    public static boolean isLastResult(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.optBoolean("ls", false);
        } catch (JSONException e) {
            Log.e(TAG, "检查最终结果失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取识别置信度
     * 按照官方文档要求获取识别质量
     * 
     * @param json 讯飞返回的JSON字符串
     * @return 置信度分数，-1表示获取失败
     */
    public static int getConfidence(String json) {
        if (json == null || json.isEmpty()) {
            return -1;
        }
        
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray wsArray = jsonObject.getJSONArray("ws");
            
            if (wsArray.length() > 0) {
                JSONObject ws = wsArray.getJSONObject(0);
                JSONArray cwArray = ws.getJSONArray("cw");
                
                if (cwArray.length() > 0) {
                    JSONObject cw = cwArray.getJSONObject(0);
                    return cw.optInt("sc", -1);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "获取置信度失败: " + e.getMessage(), e);
        }
        
        return -1;
    }
}
