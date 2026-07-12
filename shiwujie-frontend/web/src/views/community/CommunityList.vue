<template>
  <div class="community-list">
    <!-- 页面头 -->
    <div class="page-head">
      <div>
        <h2>我的社区</h2>
        <p>管理您创建的社区信息</p>
      </div>
      <a-button class="ghost-btn" @click="handleRefresh">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <a-input-search
        v-model:value="searchForm.communityId"
        placeholder="输入社区 ID 搜索"
        @search="handleSearch"
        allow-clear
        class="search-input"
      />
      <span class="count"><TeamOutlined /> 共 {{ communityList.length }} 个社区</span>
    </div>

    <!-- 社区卡片列表 -->
    <div class="comm-grid" v-if="communityList.length > 0">
      <div v-for="community in communityList" :key="community.communityId" class="comm-card">
        <div class="comm-head">
          <div>
            <div class="comm-name">{{ community.communityName }}</div>
            <div class="comm-meta">
              <span class="status-chip" :class="getStatusClass(community.communityStatus)">
                <span class="dot"></span>{{ getStatusText(community.communityStatus) }}
              </span>
            </div>
          </div>
          <a-dropdown>
            <button class="more-btn" type="button"><MoreOutlined /></button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="view" @click="viewDetail(community)"><EyeOutlined /> 查看详情</a-menu-item>
                <a-menu-item key="edit" @click="editCommunity(community)"><EditOutlined /> 编辑</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete" @click="confirmDelete(community)" danger><DeleteOutlined /> 删除社区</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>

        <div class="comm-body">
          <!-- 基本信息 -->
          <div class="info-block">
            <div class="block-label"><InfoCircleOutlined /> 基本信息</div>
            <div class="info-grid">
              <div class="info-item"><span class="k">社区类型</span><span class="v">{{ community.communityTypeName || '未设置' }}</span></div>
              <div class="info-item"><span class="k">社区等级</span><span class="v">{{ community.communityLevelName || '未设置' }}</span></div>
              <div class="info-item"><span class="k">注册人 ID</span><span class="v mono">{{ community.registerVolunteerId || '未设置' }}</span></div>
              <div class="info-item"><span class="k">父级社区</span><span class="v">{{ community.parentCommunityId || '无' }}</span></div>
            </div>
          </div>

          <!-- 地址 -->
          <div class="info-block">
            <div class="block-label"><EnvironmentOutlined /> 地址信息</div>
            <div class="address-text">{{ getFullAddress(community) }}</div>
          </div>

          <!-- 描述 -->
          <div class="info-block" v-if="community.communityDescription || community.registrationInfo">
            <div class="block-label"><FileTextOutlined /> 详细信息</div>
            <div v-if="community.communityDescription" class="desc-row">
              <span class="desc-k">社区介绍</span>
              <p class="desc-v">{{ community.communityDescription }}</p>
            </div>
            <div v-if="community.registrationInfo" class="desc-row">
              <span class="desc-k">注册信息</span>
              <p class="desc-v">{{ community.registrationInfo }}</p>
            </div>
          </div>

          <!-- 子社区 -->
          <div class="info-block">
            <div class="block-label">
              <span><BranchesOutlined /> 子社区列表</span>
              <a-button type="link" size="small" @click="loadSubCommunities(community.communityId)" :loading="loadingSubCommunities === community.communityId">
                <ReloadOutlined /> 刷新
              </a-button>
            </div>
            <div class="sub-content">
              <div v-if="community.subCommunities && community.subCommunities.length > 0" class="sub-list">
                <div v-for="sub in community.subCommunities" :key="sub.communityId" class="sub-item">
                  <div class="sub-top">
                    <span class="sub-name">{{ sub.communityName }}</span>
                    <span class="status-chip sm" :class="getStatusClass(sub.communityStatus)">{{ getStatusText(sub.communityStatus) }}</span>
                  </div>
                  <div class="sub-meta">{{ sub.communityTypeName || '未设置类型' }} · {{ getFullAddress(sub) }}</div>
                </div>

                <div v-if="community.subCommunitiesTotal > 0" class="sub-pager">
                  <a-pagination
                    :current="community.subCommunitiesCurrent || 1"
                    :page-size="community.subCommunitiesPageSize || 10"
                    :total="community.subCommunitiesTotal || 0"
                    :show-size-changer="true"
                    :show-quick-jumper="true"
                    :show-total="(total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`"
                    @change="(page, pageSize) => handleSubCommunityPageChange(community.communityId, page, pageSize)"
                    @show-size-change="(current, size) => handleSubCommunityPageChange(community.communityId, 1, size)"
                    size="small"
                  />
                </div>
              </div>
              <div v-else-if="community.subCommunitiesLoaded" class="sub-empty">暂无子社区</div>
              <div v-else class="sub-empty">点击「刷新」查看子社区列表</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="big-empty">
      <HomeOutlined class="big-empty-icon" />
      <h3>暂无社区</h3>
      <p>您还没有创建任何社区，请先注册社区入驻</p>
      <a-button type="primary" @click="$router.push('/login')"><PlusOutlined /> 去注册社区</a-button>
    </div>

    <!-- 加载 -->
    <div v-if="loading" class="big-loading">
      <a-spin size="large" />
      <p>加载中…</p>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { communityApi } from '@/api'
import { message, Modal } from 'ant-design-vue'
import { getSafeIdList, removeSafeIdFromList } from '@/utils/bigIntUtils'
import {
  ReloadOutlined, TeamOutlined, MoreOutlined, EyeOutlined, EditOutlined,
  DeleteOutlined, InfoCircleOutlined, EnvironmentOutlined, FileTextOutlined,
  BranchesOutlined, HomeOutlined, PlusOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'CommunityList',
  components: {
    ReloadOutlined, TeamOutlined, MoreOutlined, EyeOutlined, EditOutlined,
    DeleteOutlined, InfoCircleOutlined, EnvironmentOutlined, FileTextOutlined,
    BranchesOutlined, HomeOutlined, PlusOutlined
  },
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()

    const loading = ref(false)
    const communityList = ref([])
    const loadingSubCommunities = ref(null)
    const deletingCommunityId = ref(null)

    const searchForm = reactive({ communityId: '' })

    const handleSearch = async () => {
      if (!searchForm.communityId) {
        message.warning('请输入社区ID')
        return
      }
      loading.value = true
      try {
        const response = await communityApi.getCommunityById(searchForm.communityId)
        communityList.value = [response]
        response.subCommunities = []
        response.subCommunitiesLoaded = false
        response.subCommunitiesTotal = 0
        response.subCommunitiesCurrent = 1
        response.subCommunitiesPageSize = 10
        message.success('搜索成功')
      } catch (error) {
        message.error('未找到该社区或搜索失败')
        communityList.value = []
      } finally {
        loading.value = false
      }
    }

    const loadUserCommunities = async () => {
      if (!authStore.volunteer) {
        message.error('请先登录')
        router.push('/login')
        return
      }
      loading.value = true
      try {
        const volunteerId = authStore.volunteer.volunteerId
        if (!volunteerId) {
          message.error('用户信息不完整，请重新登录')
          router.push('/login')
          return
        }
        const userCommunities = getSafeIdList('userCommunities')
        if (userCommunities.length > 0) {
          const communities = []
          for (const communityId of userCommunities) {
            try {
              const response = await communityApi.getCommunityById(communityId)
              response.subCommunities = []
              response.subCommunitiesLoaded = false
              response.subCommunitiesTotal = 0
              response.subCommunitiesCurrent = 1
              response.subCommunitiesPageSize = 10
              communities.push(response)
            } catch (error) {
              removeSafeIdFromList('userCommunities', communityId)
            }
          }
          communityList.value = communities
        } else {
          communityList.value = []
        }
      } catch (error) {
        message.error('获取社区列表失败')
        communityList.value = []
      } finally {
        loading.value = false
      }
    }

    const loadSubCommunities = async (communityId, page = 1, pageSize = 10) => {
      loadingSubCommunities.value = communityId
      try {
        const response = await communityApi.getSubCommunityList(communityId, page, pageSize)
        const idx = communityList.value.findIndex(c => c.communityId === communityId)
        if (idx !== -1) {
          communityList.value[idx].subCommunities = response.records || []
          communityList.value[idx].subCommunitiesLoaded = true
          communityList.value[idx].subCommunitiesTotal = response.total || 0
          communityList.value[idx].subCommunitiesCurrent = page
          communityList.value[idx].subCommunitiesPageSize = pageSize
        }
      } catch (error) {
        message.error('获取子社区列表失败')
        const idx = communityList.value.findIndex(c => c.communityId === communityId)
        if (idx !== -1) {
          communityList.value[idx].subCommunities = []
          communityList.value[idx].subCommunitiesLoaded = true
          communityList.value[idx].subCommunitiesTotal = 0
          communityList.value[idx].subCommunitiesCurrent = 1
          communityList.value[idx].subCommunitiesPageSize = 10
        }
      } finally {
        loadingSubCommunities.value = null
      }
    }

    const handleSubCommunityPageChange = async (communityId, page, pageSize) => {
      await loadSubCommunities(communityId, page, pageSize)
    }

    const confirmDelete = (community) => {
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除社区"${community.communityName}"吗？此操作不可恢复。`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteCommunity(community)
      })
    }

    const deleteCommunity = async (community) => {
      deletingCommunityId.value = community.communityId
      try {
        await communityApi.deleteCommunity(community.communityId)
        message.success('社区删除成功')
        communityList.value = communityList.value.filter(item => item.communityId !== community.communityId)
        removeSafeIdFromList('userCommunities', community.communityId)
      } catch (error) {
        message.error('删除社区失败')
      } finally {
        deletingCommunityId.value = null
      }
    }

    const handleRefresh = () => loadUserCommunities()
    const viewDetail = (community) => router.push(`/community/${community.communityId}`)
    const editCommunity = (community) => router.push(`/community/${community.communityId}/edit`)

    const getStatusText = (status) => {
      switch (status) {
        case 0: return '未审核'
        case 1: return '已审核'
        case 2: return '已停用'
        default: return '未知状态'
      }
    }

    const getStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-pending'
        case 1: return 'status-approved'
        case 2: return 'status-disabled'
        default: return 'status-unknown'
      }
    }

    const getFullAddress = (community) => {
      const parts = [community.province, community.city, community.district, community.address]
      const address = parts.filter(part => part && part.trim() !== '').join(' ')
      return address || '地址信息未设置'
    }

    onMounted(() => {
      loadUserCommunities()
    })

    return {
      loading, loadingSubCommunities, deletingCommunityId, communityList, searchForm,
      handleSearch, handleRefresh, loadSubCommunities, handleSubCommunityPageChange,
      confirmDelete, deleteCommunity, viewDetail, editCommunity,
      getStatusText, getStatusClass, getFullAddress
    }
  }
}
</script>

<style scoped>
.community-list {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  gap: 12px;
}
.page-head h2 {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.page-head p {
  font-size: 13px;
  color: var(--text-2);
  margin-top: 2px;
}
.ghost-btn {
  border: 1px solid var(--border) !important;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.search-input {
  max-width: 360px;
  flex: 1;
}
.count {
  font-size: 13px;
  color: var(--text-2);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 卡片网格 */
.comm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 16px;
}
.comm-card {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
  overflow: hidden;
  transition: var(--tr);
}
.comm-card:hover {
  border-color: var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.comm-head {
  padding: 16px 18px;
  border-bottom: 1px solid var(--border-l);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.comm-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  padding: 0 8px;
  height: 22px;
  border-radius: 11px;
}
.status-chip .dot {
  width: 6px;
  height: 6px;
  border-radius: 3px;
  background: currentColor;
}
.status-pending { color: #9c5d00; background: rgba(255, 149, 0, 0.12); }
.status-approved { color: #14753a; background: rgba(52, 199, 89, 0.12); }
.status-disabled { color: #b82c24; background: rgba(255, 59, 48, 0.12); }
.status-unknown { color: var(--text-2); background: rgba(0, 0, 0, 0.06); }
.status-chip.sm { height: 18px; font-size: 10px; padding: 0 6px; }

.more-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--tr);
}
.more-btn:hover {
  background: var(--bg);
  color: var(--text);
}

.comm-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.info-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.block-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.block-label > span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.block-label :deep(.anticon) {
  color: var(--text-3);
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.info-item .k {
  font-size: 11px;
  color: var(--text-2);
}
.info-item .v {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}
.info-item .v.mono {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--primary);
}
.address-text {
  font-size: 13px;
  color: var(--text);
  line-height: 1.5;
}
.desc-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.desc-k {
  font-size: 11px;
  color: var(--text-2);
  font-weight: 600;
}
.desc-v {
  font-size: 12px;
  color: var(--text);
  line-height: 1.6;
  padding: 10px 12px;
  background: var(--bg);
  border-radius: 8px;
  border-left: 3px solid var(--primary);
}

.sub-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sub-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sub-item {
  padding: 10px 12px;
  border: 1px solid var(--border-l);
  border-radius: 8px;
}
.sub-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.sub-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text);
}
.sub-meta {
  font-size: 12px;
  color: var(--text-2);
}
.sub-pager {
  margin-top: 8px;
}
.sub-empty {
  text-align: center;
  padding: 16px;
  color: var(--text-3);
  font-size: 13px;
}

/* 大空状态 / 加载 */
.big-empty,
.big-loading {
  text-align: center;
  padding: 60px 20px;
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}
.big-empty-icon {
  font-size: 44px;
  color: var(--text-3);
  opacity: 0.4;
  margin-bottom: 12px;
}
.big-empty h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}
.big-empty p {
  font-size: 14px;
  color: var(--text-2);
  margin-bottom: 18px;
}
.big-loading p {
  margin-top: 14px;
  color: var(--text-2);
}

@media (max-width: 768px) {
  .comm-grid {
    grid-template-columns: 1fr;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .page-head {
    flex-direction: column;
  }
}
</style>
