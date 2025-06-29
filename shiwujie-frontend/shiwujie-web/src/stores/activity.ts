import { defineStore } from 'pinia'
import { ref } from 'vue'

interface ActivityItem {
  key: string;
  activityName: string;
  joinCount: number;
  peopleCount: number;
  beginTime: string;
  endTime: string;
  content: string;
  status: string;
  location: string;
  description: string;
  requirements: string[];
  process: string[];
}

export const useActivityStore = defineStore('activity', () => {
  const now = new Date()
  
  // 活动列表数据
  const activities = ref<ActivityItem[]>([
    {
      key: "0",
      activityName: "志愿者培训活动",
      joinCount: 18,
      peopleCount: 20,
      beginTime: "2024年03月20日 14时00分00秒",
      endTime: "2024年03月20日 16时00分00秒",
      content: "培训活动详情",
      status: "进行中",
      location: "市民中心大讲堂",
      description: "这是一个志愿者培训活动，旨在提升志愿者的服务能力和专业素养...",
      requirements: [
        "年满18岁",
        "具有志愿服务热情",
        "能够全程参与活动",
        "具备基本的沟通能力"
      ],
      process: [
        "签到入场",
        "开场致辞",
        "活动进行",
        "总结交流"
      ]
    },
    {
      key: "1",
      activityName: "社区关爱活动",
      joinCount: 15,
      peopleCount: 30,
      beginTime: "2024年03月25日 09时00分00秒",
      endTime: "2024年03月25日 12时00分00秒",
      content: "社区关爱活动详情",
      status: "未开始",
      location: "和平社区",
      description: "这是一个社区关爱活动，为社区老人提供志愿服务...",
      requirements: [
        "年满18岁",
        "有爱心和耐心"
      ],
      process: [
        "志愿者集合",
        "分配任务",
        "开展服务"
      ]
    }
  ])

  // 添加活动
  const addActivity = (activity: ActivityItem) => {
    activities.value.push(activity)
  }

  // 删除活动
  const deleteActivity = (key: string) => {
    activities.value = activities.value.filter(item => item.key !== key)
  }

  // 更新活动
  const updateActivity = (key: string, updatedActivity: Partial<ActivityItem>) => {
    const index = activities.value.findIndex(item => item.key === key)
    if (index !== -1) {
      activities.value[index] = { ...activities.value[index], ...updatedActivity }
    }
  }

  return {
    activities,
    addActivity,
    deleteActivity,
    updateActivity,
  }
}, {
  persist: true
}) 