import Storage from "./storage";


/**
 * 根据用户的身份跳转页面
 */
export function toPagesByUserRole(userRole){
	console.log("开始跳转页面,status:")
	console.log(userRole);
	//跳转页面
	switch (userRole) {
		case 0:
		//盲人主页
			uni.switchTab({ 
				url:'/pages/blindPages/blindHome'
			});
			break;
		case 1:
		//志愿者主页
			console.log("志愿者主页跳转");
			  uni.switchTab({
				url:'/pages/volunteerPages/volunteerHome'
			})
			break;
		case 2:
		//选择身份
			uni.navigateTo({
				url:'/pages/register-login/chooseRole',
			})
			break;
	}
}