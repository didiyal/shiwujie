<template>
  <div class="community-edit">
    <div class="edit-wrap">
      <div class="page-head">
        <div>
          <h2>编辑社区信息</h2>
          <p>修改社区的基本信息</p>
        </div>
      </div>

      <!-- 加载 -->
      <div v-if="loading" class="state-box">
        <a-spin size="large" />
        <p>加载中…</p>
      </div>

      <!-- 表单 -->
      <div v-else-if="communityData" class="panel">
        <form @submit.prevent="handleSubmit" class="edit-form">
          <div class="form-group">
            <label class="label">社区 ID</label>
            <input type="text" :value="communityData.communityId" readonly class="form-input readonly" />
            <span class="hint">社区 ID 不可修改</span>
          </div>

          <div class="form-group">
            <label class="label">社区名称 <span class="req">*</span></label>
            <input
              type="text"
              v-model="formData.communityName"
              class="form-input"
              :class="{ 'has-error': errors.communityName }"
              placeholder="请输入社区名称"
              maxlength="100"
            />
            <span v-if="errors.communityName" class="error-text">{{ errors.communityName }}</span>
          </div>

          <div class="form-group">
            <label class="label">社区描述</label>
            <textarea
              v-model="formData.communityDescription"
              class="form-textarea"
              :class="{ 'has-error': errors.communityDescription }"
              placeholder="请输入社区描述信息"
              rows="4"
              maxlength="500"
            ></textarea>
            <span v-if="errors.communityDescription" class="error-text">{{ errors.communityDescription }}</span>
            <span v-else class="hint">可选，最多 500 字符</span>
          </div>

          <div class="form-actions">
            <button type="button" @click="handleCancel" class="btn btn-outline">取消</button>
            <button type="submit" :disabled="submitting" class="btn btn-primary">
              {{ submitting ? '保存中…' : '保存修改' }}
            </button>
          </div>
        </form>
      </div>

      <!-- 错误 -->
      <div v-else class="state-box">
        <CloseCircleOutlined class="state-icon danger" />
        <h3>加载失败</h3>
        <p>{{ errorMessage || '无法加载社区信息' }}</p>
        <button @click="goBack" class="btn btn-primary">返回</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { communityApi } from '@/api'
import { message } from 'ant-design-vue'
import { CloseCircleOutlined } from '@ant-design/icons-vue'

export default {
  name: 'CommunityEdit',
  components: { CloseCircleOutlined },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(true)
    const submitting = ref(false)
    const errorMessage = ref('')
    const communityData = ref(null)

    const formData = reactive({
      communityId: '',
      communityName: '',
      communityDescription: ''
    })

    const errors = reactive({
      communityName: '',
      communityDescription: ''
    })

    const validateForm = () => {
      let isValid = true
      errors.communityName = ''
      errors.communityDescription = ''

      if (!formData.communityName || formData.communityName.trim() === '') {
        errors.communityName = '社区名称不能为空'
        isValid = false
      } else if (formData.communityName.trim().length < 2) {
        errors.communityName = '社区名称至少需要 2 个字符'
        isValid = false
      } else if (formData.communityName.trim().length > 100) {
        errors.communityName = '社区名称不能超过 100 个字符'
        isValid = false
      }

      if (formData.communityDescription && formData.communityDescription.trim().length > 500) {
        errors.communityDescription = '社区描述不能超过 500 个字符'
        isValid = false
      }

      return isValid
    }

    const loadCommunityData = async () => {
      const communityId = route.params.id
      if (!communityId) {
        errorMessage.value = '缺少社区 ID 参数'
        loading.value = false
        return
      }
      try {
        const response = await communityApi.getCommunityById(communityId)
        communityData.value = response
        formData.communityId = response.communityId
        formData.communityName = response.communityName || ''
        formData.communityDescription = response.communityDescription || ''
      } catch (error) {
        errorMessage.value = error.message || '加载社区信息失败'
      } finally {
        loading.value = false
      }
    }

    const handleSubmit = async () => {
      if (!validateForm()) return
      submitting.value = true
      try {
        const updateData = {
          communityId: formData.communityId,
          communityName: formData.communityName.trim(),
          communityDescription: formData.communityDescription.trim()
        }
        await communityApi.updateCommunity(updateData)
        message.success('社区信息更新成功')
        router.push('/community-list')
      } catch (error) {
        message.error(error.message || '更新社区信息失败')
      } finally {
        submitting.value = false
      }
    }

    const handleCancel = () => router.push('/community-list')
    const goBack = () => router.push('/community-list')

    onMounted(() => loadCommunityData())

    return {
      loading, submitting, errorMessage, communityData, formData, errors,
      handleSubmit, handleCancel, goBack
    }
  }
}
</script>

<style scoped>
.community-edit {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.edit-wrap {
  max-width: 640px;
}

.page-head {
  margin-bottom: 18px;
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

.panel {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}
.label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}
.req {
  color: var(--danger);
}
.form-input,
.form-textarea {
  width: 100%;
  padding: 0 12px;
  font-family: var(--font);
  font-size: 13px;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: var(--tr);
}
.form-input {
  height: 40px;
}
.form-textarea {
  min-height: 96px;
  padding: 10px 12px;
  line-height: 1.6;
  resize: vertical;
}
.form-input:hover,
.form-textarea:hover {
  border-color: var(--text-3);
}
.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-t);
}
.form-input::placeholder,
.form-textarea::placeholder {
  color: var(--text-3);
}
.form-input.readonly {
  background: var(--bg);
  color: var(--text-2);
  cursor: not-allowed;
  font-family: var(--font-mono);
  font-size: 12px;
}
.form-input.has-error,
.form-textarea.has-error {
  border-color: var(--danger);
}
.hint {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: var(--text-2);
}
.error-text {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: var(--danger);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-l);
}
.btn {
  height: 38px;
  padding: 0 20px;
  font-family: var(--font);
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 19px;
  cursor: pointer;
  transition: var(--tr);
}
.btn:active {
  transform: scale(0.98);
}
.btn-primary {
  background: var(--primary);
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: var(--primary-h);
}
.btn-outline {
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
}
.btn-outline:hover {
  background: var(--bg);
  border-color: var(--text-3);
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.state-box {
  text-align: center;
  padding: 60px 20px;
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}
.state-icon {
  font-size: 40px;
  margin-bottom: 12px;
}
.state-icon.danger {
  color: var(--danger);
  opacity: 0.7;
}
.state-box h3 {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
}
.state-box p {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 18px;
}

@media (max-width: 576px) {
  .form-actions {
    flex-direction: column-reverse;
  }
  .btn {
    width: 100%;
  }
}
</style>
