<template>
  <div class="activity-stats">
    <div class="page-head">
      <div>
        <h2>活动统计</h2>
        <p>活动开展数据概览</p>
      </div>
    </div>
    <a-card :bordered="false">
      <a-empty description="暂无统计数据" />
    </a-card>
  </div>
</template>

<script>
export default {
  name: 'ActivityStats'
};
</script>

<style scoped>
.activity-stats {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.page-head {
  margin-bottom: 18px;
}
.page-head h2 {
  margin: 0 0 2px 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
}
.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
}
</style>
