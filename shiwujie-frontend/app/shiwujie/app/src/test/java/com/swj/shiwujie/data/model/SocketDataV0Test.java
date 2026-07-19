package com.swj.shiwujie.data.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SocketDataV0 协议序列化单测（chunk-2e-2）。
 *
 * <p>守护上行帧 JSON 契约（最易错处）：
 * <ul>
 *   <li>AI turn 帧（100）带 text + position（裸 socketData，后端 CoordinationSocketHandler.onMessage
 *       直接 {@code JSONUtil.toBean(message, SocketData.class)}）。</li>
 *   <li>position=null 降级为 {@code "position":null}（后端宽松接收）。</li>
 *   <li>既有信令帧（login=0 / heartbeat=-1）JSON 只有原 4 字段，不含 text/position（向后兼容，不污染老协议）。</li>
 * </ul>
 */
public class SocketDataV0Test {

    private static JsonObject toJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    public void createAiTurnRequest_blind_fillsBlindPhoneAndFields() {
        SocketDataV0.Position pos = new SocketDataV0.Position(39.9, 116.4, "北京");
        SocketDataV0 data = SocketDataV0.createAiTurnRequest("13800000000", true, "附近餐厅", pos);

        assertEquals(SocketDataV0.REQUEST_TYPE_AI_TURN_REQUEST, data.getRequestType());
        assertEquals(100, data.getRequestType());
        assertEquals("13800000000", data.getBlindPhone());
        assertEquals("", data.getVolunteerPhone());
        assertEquals(0L, data.getChannelId());
        assertEquals("附近餐厅", data.getText());
        assertEquals(39.9, data.getPosition().getLat(), 0.0001);
        assertEquals(116.4, data.getPosition().getLng(), 0.0001);
        assertEquals("北京", data.getPosition().getAddress());
    }

    @Test
    public void createAiTurnRequest_volunteer_fillsVolunteerPhone() {
        SocketDataV0 data = SocketDataV0.createAiTurnRequest("13900000000", false, "你好", null);

        assertEquals("13900000000", data.getVolunteerPhone());
        assertEquals("", data.getBlindPhone());
    }

    @Test
    public void toSendJson_aiTurnRequest_includesTextAndPosition() {
        SocketDataV0.Position pos = new SocketDataV0.Position(39.9, 116.4, "北京");
        SocketDataV0 data = SocketDataV0.createAiTurnRequest("13800000000", true, "附近餐厅", pos);

        JsonObject obj = toJson(data.toSendJson());

        assertEquals(100, obj.get("requestType").getAsInt());
        assertEquals("13800000000", obj.get("blindPhone").getAsString());
        assertEquals("", obj.get("volunteerPhone").getAsString());
        assertEquals(0L, obj.get("channelId").getAsLong());
        assertEquals("附近餐厅", obj.get("text").getAsString());
        JsonObject p = obj.getAsJsonObject("position");
        assertEquals(39.9, p.get("lat").getAsDouble(), 0.0001);
        assertEquals(116.4, p.get("lng").getAsDouble(), 0.0001);
        assertEquals("北京", p.get("address").getAsString());
    }

    @Test
    public void toSendJson_aiTurnRequest_nullPosition_fieldOmitted() {
        // 定位失败降级：position=null → Gson 默认不序列化 null，position 字段缺省；
        // 后端 toBean 得 null、buildBody if(position!=null) 跳过（等价不带 position，不阻塞 turn）。
        SocketDataV0 data = SocketDataV0.createAiTurnRequest("13800000000", true, "你好", null);

        JsonObject obj = toJson(data.toSendJson());

        assertEquals(100, obj.get("requestType").getAsInt());
        assertEquals("你好", obj.get("text").getAsString());
        assertFalse("position=null 时字段应缺省（Gson 默认不序列化 null）", obj.has("position"));
    }

    @Test
    public void toSendJson_legacyLoginFrame_unchanged_noTextOrPosition() {
        // 向后兼容：既有 login 帧（0）JSON 只含 4 字段，不混入 text/position。
        SocketDataV0 data = SocketDataV0.createLoginMessage("13800000000", false);

        JsonObject obj = toJson(data.toSendJson());

        assertEquals(0, obj.get("requestType").getAsInt());
        assertEquals("13800000000", obj.get("blindPhone").getAsString());
        assertEquals("", obj.get("volunteerPhone").getAsString());
        assertEquals(0L, obj.get("channelId").getAsLong());
        assertFalse("老帧不应带 text", obj.has("text"));
        assertFalse("老帧不应带 position", obj.has("position"));
    }

    @Test
    public void toSendJson_legacyHeartbeatFrame_unchanged() {
        SocketDataV0 data = SocketDataV0.createHeartbeatMessage("13800000000", false);

        JsonObject obj = toJson(data.toSendJson());

        assertEquals(-1, obj.get("requestType").getAsInt());
        assertFalse(obj.has("text"));
        assertFalse(obj.has("position"));
    }
}
