/**
 * 初始化socket连接
 */
import Storage from "./storage";
const user = Storage.getUserInfo();
export function websocketInit() {
	
	const socketTask = uni.connectSocket({
				url: 'ws://192.168.185.142:8081/shiwujie/websocket/' + user.id,
				success: () => {
					console.log("连接服务器成功")

					this.globalData.socketTask = socketTask;
					uni.$emit('websocketConnected',"ok");
				},

				fail: (err) => {
					console.error('连接失败:', err);
				}

			});
			

};