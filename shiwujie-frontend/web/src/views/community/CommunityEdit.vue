<template>
  <div class="community-edit">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>编辑社区信息</h2>
      <p>修改社区的基本信息</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 编辑表单 -->
    <div v-else-if="communityData" class="edit-form-container">
      <form @submit.prevent="handleSubmit" class="edit-form">
        <!-- 社区ID (只读) -->
        <div class="form-group">
          <label class="form-label">社区ID</label>
          <input 
            type="text" 
            :value="communityData.communityId" 
            readonly 
            class="form-input readonly"
          />
          <span class="form-hint">社区ID不可修改</span>
        </div>

        <!-- 社区名称 -->
        <div class="form-group">
          <label class="form-label required">社区名称</label>
          <input 
            type="text" 
            v-model="formData.communityName" 
            class="form-input"
            :class="{ 'error': errors.communityName }"
            placeholder="请输入社区名称"
            maxlength="100"
          />
          <span v-if="errors.communityName" class="error-message">{{ errors.communityName }}</span>
        </div>

        <!-- 社区描述 -->
        <div class="form-group">
          <label class="form-label">社区描述</label>
          <textarea 
            v-model="formData.communityDescription" 
            class="form-textarea"
            :class="{ 'error': errors.communityDescription }"
            placeholder="请输入社区描述信息"
            rows="4"
            maxlength="500"
          ></textarea>
          <span v-if="errors.communityDescription" class="error-message">{{ errors.communityDescription }}</span>
          <span class="form-hint">可选，最多500字符</span>
        </div>

        <!-- 表单操作 -->
        <div class="form-actions">
          <button 
            type="button" 
            @click="handleCancel" 
            class="btn btn-secondary"
          >
            取消
          </button>
          <button 
            type="submit" 
            :disabled="submitting" 
            class="btn btn-primary"
          >
            <span v-if="submitting" class="btn-loading"></span>
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-state">
      <div class="error-icon">❌</div>
      <h3>加载失败</h3>
      <p>{{ errorMessage || '无法加载社区信息' }}</p>
      <button @click="goBack" class="btn btn-primary">返回</button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { communityApi } from '@/api';
import { message } from 'ant-design-vue';

export default {
  name: 'CommunityEdit',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const loading = ref(true);
    const submitting = ref(false);
    const errorMessage = ref('');
    const communityData = ref(null);

    // 表单数据
    const formData = reactive({
      communityId: '',
      communityName: '',
      communityDescription: ''
    });

    // 表单验证错误
    const errors = reactive({
      communityName: '',
      communityDescription: ''
    });

    // 验证表单
    const validateForm = () => {
      let isValid = true;
      
      // 重置错误信息
      errors.communityName = '';
      errors.communityDescription = '';

      // 验证社区名称
      if (!formData.communityName || formData.communityName.trim() === '') {
        errors.communityName = '社区名称不能为空';
        isValid = false;
      } else if (formData.communityName.trim().length < 2) {
        errors.communityName = '社区名称至少需要2个字符';
        isValid = false;
      } else if (formData.communityName.trim().length > 100) {
        errors.communityName = '社区名称不能超过100个字符';
        isValid = false;
      }

      // 验证社区描述
      if (formData.communityDescription && formData.communityDescription.trim().length > 500) {
        errors.communityDescription = '社区描述不能超过500个字符';
        isValid = false;
      }

      return isValid;
    };

    // 加载社区信息
    const loadCommunityData = async () => {
      const communityId = route.params.id;
      console.log('🔍 开始加载社区信息，ID:', communityId);
      
      if (!communityId) {
        errorMessage.value = '缺少社区ID参数';
        loading.value = false;
        return;
      }

      try {
        const response = await communityApi.getCommunityById(communityId);
        console.log('✅ 社区信息加载成功:', response);
        
        communityData.value = response;
        
        // 初始化表单数据
        formData.communityId = response.communityId;
        formData.communityName = response.communityName || '';
        formData.communityDescription = response.communityDescription || '';
        
      } catch (error) {
        console.error('❌ 加载社区信息失败:', error);
        errorMessage.value = error.message || '加载社区信息失败';
      } finally {
        loading.value = false;
      }
    };

    // 提交表单
    const handleSubmit = async () => {
      console.log('🔍 开始提交表单数据:', formData);
      
      if (!validateForm()) {
        console.log('❌ 表单验证失败');
        return;
      }

      submitting.value = true;
      
      try {
        const updateData = {
          communityId: formData.communityId,
          communityName: formData.communityName.trim(),
          communityDescription: formData.communityDescription.trim()
        };
        
        console.log('📤 提交的更新数据:', updateData);
        
        const response = await communityApi.updateCommunity(updateData);
        console.log('✅ 社区信息更新成功:', response);
        
        message.success('社区信息更新成功');
        
        // 返回社区列表页面
        router.push('/community-list');
        
      } catch (error) {
        console.error('❌ 更新社区信息失败:', error);
        message.error(error.message || '更新社区信息失败');
      } finally {
        submitting.value = false;
      }
    };

    // 取消编辑
    const handleCancel = () => {
      router.push('/community-list');
    };

    // 返回上一页
    const goBack = () => {
      router.push('/community-list');
    };

    // 组件挂载时加载数据
    onMounted(() => {
      loadCommunityData();
    });

    return {
      loading,
      submitting,
      errorMessage,
      communityData,
      formData,
      errors,
      handleSubmit,
      handleCancel,
      goBack
    };
  }
};
</script>

<style scoped>
.community-edit {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
  text-align: center;
}

.page-header h2 {
  color: #1890ff;
  margin-bottom: 8px;
  font-size: 28px;
}

.page-header p {
  color: #666;
  margin: 0;
  font-size: 16px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-state p {
  color: #666;
  font-size: 16px;
}

.edit-form-container {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e9ecef;
}

.edit-form {
  max-width: 600px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-label.required::after {
  content: ' *';
  color: #ff4d4f;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
}

.form-input.error,
.form-textarea.error {
  border-color: #ff4d4f;
}

.form-input.readonly {
  background: #f5f5f5;
  color: #666;
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

.form-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.error-message {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #ff4d4f;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #40a9ff, #69c0ff);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
}

.btn-secondary {
  background: white;
  color: #666;
  border: 2px solid #e8e8e8;
}

.btn-secondary:hover {
  border-color: #d9d9d9;
  background: #fafafa;
}

.btn-loading {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.error-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-state h3 {
  color: #333;
  margin-bottom: 8px;
  font-size: 20px;
}

.error-state p {
  color: #666;
  margin-bottom: 24px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .community-edit {
    padding: 16px;
  }
  
  .edit-form-container {
    padding: 24px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style> 