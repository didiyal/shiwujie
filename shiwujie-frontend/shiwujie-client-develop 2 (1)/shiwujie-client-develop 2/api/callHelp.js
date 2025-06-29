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
 * 盲人申请求助
 */
export function getBlindChannelAndUidAsyc(){
	const url = 'http://43.139.38.62:8081/shiwujie/callHelp/getBlindChannelAndUid'
	return request({
		url,
		method: 'GET',
		header: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});
}

/**
 * 被求助家属加入求助
 */
export function joinBlindChannelAndUidAsyc(blindUid){
	const url = 'http://43.139.38.62:8081/shiwujie/callHelp/joinBlindChannelAndUid'
	return request({
		url,
		method: 'POST',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		data: {
			blindUid,
		},
		withCredentials: true, //允许跨域携带token
	});
}


/**
 * 求助方取消求助
 */
export function leaveCallHelpByBlindAsyc() {
	const url = 'http://43.139.38.62:8081/shiwujie/callHelp/leaveCallHelpByBlind'
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
 * 协助家属方退出求助
 */
export function leaveBlindChannelAsyc() {
	const url = 'http://43.139.38.62:8081/shiwujie/callHelp/leaveBlindChannel'
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