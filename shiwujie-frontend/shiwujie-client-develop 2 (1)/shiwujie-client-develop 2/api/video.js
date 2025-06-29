import Storage from "../utils/storage";
const token = Storage.getToken();
//封装的统一request
function request(options) {
	return new Promise((resolve, reject) => {
		uni.request({
			...options,
			success: (res) => {
				if (res.statusCode === 200) {
					resolve(res.data); // 请求成功，返回数据
				} else {
					reject(res.data); // 请求失败，返回错误信息
				}
			},
			fail: (err) => {
				reject(err); // 请求失败，返回错误信息
			}
		});
	});
}


/**
 * 志愿者获取视频通话频道信息与uid
 */
export function getVolunteerChannelAndUid() {
	const url = 'http://43.139.38.62:8081/shiwujie/channel/getVolunteerChannelAndUid'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});

}
/**
 * 盲人获取视频通话频道信息与uid
 */
export function getBlindChannelAndUid() {
	const url = 'http://43.139.38.62:8081/shiwujie/channel/getBlindChannelAndUid'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});

}

/**
 * 志愿者退出频道
 */
export function leaveVolunteerChannel() {
	const url = 'http://43.139.38.62:8081/shiwujie/channel/leaveVolunteerChannel'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});

}
/**
 * 盲人退出频道
 */
export function leaveBlindChannel() {
	const url = 'http://43.139.38.62:8081/shiwujie/channel/leaveBlindChannel'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});

}

export const leaveBlindChannelAsyc = async () =>{
	const url = 'http://43.139.38.62:8081/shiwujie/channel/leaveBlindChannel'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});
}