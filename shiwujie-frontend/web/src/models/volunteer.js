// 复用APP中的VolunteerVO结构
export class VolunteerModel {
  constructor(data) {
    this.volunteerId = data.volunteerId
    this.communityId = data.communityId
    this.isActivelyJoined = data.isActivelyJoined
    this.familyId = data.familyId
    this.name = data.name
    this.phone = data.phone
    this.gender = data.gender
    this.wechatId = data.wechatId
    this.qqId = data.qqId
    this.isIdCard = data.isIdCard
    this.otherInfo = data.otherInfo
    this.onlineStatus = data.onlineStatus
    this.helpCount = data.helpCount
    this.rating = data.rating
    this.latitude = data.latitude
    this.longitude = data.longitude
    this.locationAddress = data.locationAddress
    this.locationUpdateTime = data.locationUpdateTime
    this.createTime = data.createTime
    this.updateTime = data.updateTime
    this.communityManager = data.communityManager
  }
}

// 社区登录成功响应模型
export class CommunityLoginModel {
  constructor(data) {
    this.volunteer = new VolunteerModel(data.volunteer)
    this.token = data.token
  }
} 