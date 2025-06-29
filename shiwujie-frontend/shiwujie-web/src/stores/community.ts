import { defineStore } from 'pinia'
import { ref } from 'vue'

interface CommunityItem {
  key: string;
  name: string;
  account: string;
  gender: string;
  volunteerCount: number;
  isOnline: boolean;
}

export const useCommunityStore = defineStore('community', () => {
  // 社区列表数据
  const communities = ref<CommunityItem[]>([
    {
      key: '1',
      name: '张三',
      account: 'zhangsan001',
      gender: '男',
      volunteerCount: 5,
      isOnline: true,
    },
    {
      key: '2',
      name: '李四',
      account: 'lisi002',
      gender: '女',
      volunteerCount: 3,
      isOnline: false,
    },
  ])

  // 添加社区成员
  const addCommunity = (community: CommunityItem) => {
    communities.value.push(community)
  }

  // 删除社区成员
  const deleteCommunity = (key: string) => {
    communities.value = communities.value.filter(item => item.key !== key)
  }

  // 更新社区成员
  const updateCommunity = (key: string, updatedCommunity: Partial<CommunityItem>) => {
    const index = communities.value.findIndex(item => item.key === key)
    if (index !== -1) {
      communities.value[index] = { ...communities.value[index], ...updatedCommunity }
    }
  }

  return {
    communities,
    addCommunity,
    deleteCommunity,
    updateCommunity,
  }
}, {
  persist: true
}) 