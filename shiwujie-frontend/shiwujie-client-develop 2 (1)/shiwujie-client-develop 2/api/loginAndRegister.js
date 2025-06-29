import Storage from "../utils/storage";

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
 * 手机号一键登录注册
 * @param {Object} userPhone 手机号
 */
export function loginAndRegisterQuickly(userPhone) {
	const url = 'http://43.139.38.62:8081/shiwujie/user/LoginAndRegisterQuickly'
	return request({
		url,
		method: 'POST',
		header: {
			'Content-Type': 'application/x-www-form-urlencoded',
		},
		data: {
			userPhone: userPhone,
		},
	});

}

/**
 * 校验jwt令牌
 */
export function testJwt() {
	const token = Storage.getToken();
	return request({
		url: 'http://43.139.38.62:8081/shiwujie/user/test/jwt',
		method: 'GET',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		withCredentials: true, //允许跨域携带token
	});
}


/**
 * 手机号密码登录
 * @param {Object} userPhone 手机号
 * @param {Object} userPassword 密码
 */
export function loginAndRegister(userPhone,userPassword){
	return request({
		url: 'http://43.139.38.62:8081/shiwujie/user/LoginAndRegister',
		method: 'POST',
		header: {
			'Content-Type': 'application/json',
		},
		data: {
			userPhone,
			userPassword,
		}
	})
}

/**
 * 修改用户信息
 */
export function updateUserInfo(data={}){
	const token = Storage.getToken();
	return request({
		url: 'http://43.139.38.62:8081/shiwujie/user/mine/update',
		method: 'PUT',
		header: {
			'Content-Type': 'application/json',
			'Authorization': `Bearer ${token}`,
		},
		data,
		withCredentials: true, //允许跨域携带token
	})
}

