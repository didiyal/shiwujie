<template>
  <div class="home-page">
    <!-- ==================== HERO ==================== -->
    <section class="hero">
      <div class="hero-bg">
        <div class="dot-grid"></div>
        <div class="glow glow-1"></div>
        <div class="glow glow-2"></div>
        <div class="glow glow-3"></div>
        <div class="tech-lines">
          <div class="tech-line"></div>
          <div class="tech-line"></div>
          <div class="tech-line"></div>
        </div>
      </div>

      <div class="hero-visual" aria-hidden="true">
        <div class="visual-core"></div>
        <div class="orbit orbit-1"><div class="orbit-dot"></div></div>
        <div class="orbit orbit-2"><div class="orbit-dot"></div></div>
        <div class="orbit orbit-3"><div class="orbit-dot"></div></div>
        <div class="float-dots">
          <div class="float-dot"></div><div class="float-dot"></div><div class="float-dot"></div>
          <div class="float-dot"></div><div class="float-dot"></div>
        </div>
      </div>

      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span> v3.0 全新升级
        </div>
        <h1 class="hero-title">
          连接视障群体<br>用科技<span>看见</span>世界
        </h1>
        <p class="hero-desc">
          视无界是一个专为视障群体打造的社区服务平台，通过智能匹配连接志愿者与视障人士，
          让每一次帮助都精准、温暖、高效。
        </p>
        <a href="/app-download.apk" download="视无界.apk" class="btn-download">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          下载 Android App
        </a>
        <div class="hero-platforms">
          <span>Android</span>
        </div>
      </div>

      <div class="scroll-hint" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
        <span>向下滚动了解更多</span>
      </div>
    </section>

    <!-- ==================== FEATURES ==================== -->
    <section class="section" id="features">
      <div class="section-header">
        <div class="section-label">产品功能</div>
        <h2 class="section-title">为视障群体打造的<br>全场景服务平台</h2>
        <p class="section-desc">从社区互动到活动组织，从求助响应到数据洞察，视无界提供完整的服务闭环。</p>
      </div>

      <!-- 场景一：社区网络 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">社区网络</div>
            </div>
            <div class="demo-frame-body">
              <div class="conn-lines">
                <div class="conn-line"></div><div class="conn-line"></div><div class="conn-line"></div>
              </div>
              <div class="scenario-community">
                <div class="community-card" v-for="(c, i) in communityCards" :key="i">
                  <div class="comm-avatar" :class="c.avatarClass">{{ c.initial }}</div>
                  <div class="comm-info">
                    <div class="comm-name">{{ c.name }}</div>
                    <div class="comm-role">{{ c.role }}</div>
                  </div>
                  <div class="comm-dot"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>
          </div>
          <h3>社区网络</h3>
          <p>创建和加入多元化的兴趣社区，让视障人士与志愿者在同一个平台上相遇、互动、互助。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 支持按兴趣、地域精准匹配社区</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 志愿者与视障人士双向选择加入</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 实时在线状态，快速发起互动</li>
          </ul>
        </div>
      </div>

      <!-- 场景二：活动组织 -->
      <div class="feature-row reverse">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">活动管理</div>
            </div>
            <div class="demo-frame-body">
              <div class="scenario-activity">
                <div class="cal-header">
                  <div class="cal-month">2026年 7月</div>
                  <div class="cal-nav"><div class="cal-nav-btn">&lt;</div><div class="cal-nav-btn">&gt;</div></div>
                </div>
                <div class="cal-grid">
                  <div class="cal-day-name">日</div><div class="cal-day-name">一</div><div class="cal-day-name">二</div><div class="cal-day-name">三</div><div class="cal-day-name">四</div><div class="cal-day-name">五</div><div class="cal-day-name">六</div>
                  <div class="cal-day other">29</div><div class="cal-day other">30</div><div class="cal-day">1</div><div class="cal-day">2</div><div class="cal-day">3</div><div class="cal-day">4</div><div class="cal-day has-event">5</div>
                  <div class="cal-day">6</div><div class="cal-day">7</div><div class="cal-day">8</div><div class="cal-day">9</div><div class="cal-day">10</div><div class="cal-day">11</div><div class="cal-day today has-event">12</div>
                  <div class="cal-day has-event">13</div><div class="cal-day">14</div><div class="cal-day">15</div><div class="cal-day">16</div><div class="cal-day">17</div><div class="cal-day">18</div><div class="cal-day">19</div>
                </div>
                <div class="event-list">
                  <div class="event-item">
                    <div class="event-time">14:00</div>
                    <div class="event-title">视障人士智能手机培训班</div>
                    <div class="event-badge upcoming">即将开始</div>
                  </div>
                  <div class="event-item">
                    <div class="event-time">16:30</div>
                    <div class="event-title">社区读书分享会</div>
                    <div class="event-badge ongoing">进行中</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          </div>
          <h3>活动组织</h3>
          <p>线上线下一体化的活动管理，从创建发布到报名签到，全流程数字化追踪。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 活动创建、发布、报名、签到一站式管理</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 日历视图直观展示活动排期</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 活动数据自动统计，参与率一目了然</li>
          </ul>
        </div>
      </div>

      <!-- 场景三：求助响应 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">求助响应</div>
            </div>
            <div class="demo-frame-body">
              <div class="scenario-help">
                <div class="help-badge-row">
                  <div class="help-tag active">全部</div>
                  <div class="help-tag">出行</div>
                  <div class="help-tag">阅读</div>
                  <div class="help-tag">购物</div>
                </div>
                <div class="chat-list">
                  <div class="chat-item">
                    <div class="chat-avatar urgent">急</div>
                    <div class="chat-content">
                      <div class="chat-name">视障人士 张先生</div>
                      <div class="chat-text">需要帮忙阅读药品说明书…</div>
                      <div class="chat-meta"><span class="chat-time">2分钟前</span><span class="chat-status waiting">等待匹配</span></div>
                    </div>
                    <div class="chat-badge">1</div>
                  </div>
                  <div class="chat-item">
                    <div class="chat-avatar normal">助</div>
                    <div class="chat-content">
                      <div class="chat-name">视障人士 李阿姨</div>
                      <div class="chat-text">下午去社区医院需要陪同…</div>
                      <div class="chat-meta"><span class="chat-time">15分钟前</span><span class="chat-status matched">已匹配</span></div>
                    </div>
                  </div>
                  <div class="chat-item">
                    <div class="chat-avatar normal">帮</div>
                    <div class="chat-content">
                      <div class="chat-name">视障人士 王大爷</div>
                      <div class="chat-text">想找人一起去公园散步…</div>
                      <div class="chat-meta"><span class="chat-time">1小时前</span><span class="chat-status matched">已匹配</span></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z"/></svg>
          </div>
          <h3>求助响应</h3>
          <p>即时求助发布与智能志愿者匹配系统，确保每一次求助都能得到快速、精准的响应。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 一键发布求助，按类别精准分类</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 智能匹配附近志愿者，缩短响应时间</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 实时状态追踪，从求助到完成全程可见</li>
          </ul>
        </div>
      </div>

      <!-- 场景四：数据洞察 -->
      <div class="feature-row reverse">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">数据洞察</div>
            </div>
            <div class="demo-frame-body">
              <div class="scenario-stats">
                <div class="stats-cards">
                  <div class="stat-mini">
                    <div class="stat-mini-val">1,286</div>
                    <div class="stat-mini-label">活跃用户</div>
                    <div class="stat-mini-change">↑ 12.5%</div>
                  </div>
                  <div class="stat-mini">
                    <div class="stat-mini-val">56</div>
                    <div class="stat-mini-label">活跃社区</div>
                    <div class="stat-mini-change">↑ 8.3%</div>
                  </div>
                  <div class="stat-mini">
                    <div class="stat-mini-val">98.6%</div>
                    <div class="stat-mini-label">响应率</div>
                    <div class="stat-mini-change">↑ 2.1%</div>
                  </div>
                </div>
                <div class="chart-area">
                  <div class="chart-title">近7天求助响应趋势</div>
                  <div class="bars">
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周一</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周二</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周三</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周四</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周五</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周六</div></div>
                    <div class="bar-wrap"><div class="bar"></div><div class="bar-label">周日</div></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
          </div>
          <h3>数据洞察</h3>
          <p>多维度数据分析看板，实时掌握社区活跃度、活动参与率和服务质量，用数据驱动决策。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 社区与活动数据双看板，关键指标一目了然</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 趋势图表自动生成，可视化分析更直观</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 数据驱动服务优化，提升匹配效率与满意度</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'HomePage',
  setup() {
    const communityCards = [
      { initial: '李', avatarClass: 'a1', name: '李家社区', role: '志愿者团队 · 12人' },
      { initial: '张', avatarClass: 'a3', name: '张江读书会', role: '兴趣社区 · 28人' },
      { initial: '王', avatarClass: 'a4', name: '王港助盲团', role: '公益组织 · 45人' },
      { initial: '陈', avatarClass: 'a6', name: '陈行服务队', role: '社区服务 · 19人' }
    ]
    return { communityCards }
  }
}
</script>

<style scoped>
/* ============================================================
   HERO
   ============================================================ */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 120px 40px 80px;
}
.hero-bg { position: absolute; inset: 0; z-index: 0; }
.dot-grid {
  position: absolute; inset: 0;
  background-image: radial-gradient(circle, rgba(0,113,227,.12) 1px, transparent 1px);
  background-size: 32px 32px;
}
.glow { position: absolute; border-radius: 50%; }
.glow-1 {
  top: -20%; left: -10%; width: 600px; height: 600px;
  background: radial-gradient(circle, rgba(0,113,227,.12) 0%, transparent 70%);
  animation: float1 8s ease-in-out infinite;
}
.glow-2 {
  bottom: -20%; right: -10%; width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(0,113,227,.08) 0%, transparent 70%);
  animation: float2 10s ease-in-out infinite;
}
.glow-3 {
  top: 40%; right: 20%; width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(30,138,158,.10) 0%, transparent 70%);
  animation: float3 7s ease-in-out infinite;
}
@keyframes float1 {
  0%,100% { transform: translate(0,0) scale(1); }
  50% { transform: translate(40px,-30px) scale(1.05); }
}
@keyframes float2 {
  0%,100% { transform: translate(0,0) scale(1); }
  50% { transform: translate(-30px,40px) scale(1.08); }
}
@keyframes float3 {
  0%,100% { transform: translate(0,0) scale(1); }
  50% { transform: translate(-20px,-20px) scale(1.06); }
}

.tech-lines { position: absolute; inset: 0; opacity: .15; }
.tech-line {
  position: absolute;
  background: linear-gradient(90deg, transparent, var(--primary), transparent);
  height: 1px;
}
.tech-line:nth-child(1) { top:20%; left:0; width:60%; animation:lineMove 6s linear infinite; }
.tech-line:nth-child(2) { top:50%; right:0; width:40%; animation:lineMove 8s linear infinite reverse; }
.tech-line:nth-child(3) { top:75%; left:10%; width:30%; animation:lineMove 7s linear infinite; }
@keyframes lineMove {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(200%); }
}

/* 中央科技图形 */
.hero-visual {
  position: absolute; top:50%; left:50%;
  transform: translate(-50%,-50%);
  z-index: 1; pointer-events: none;
}
.visual-core {
  width: 280px; height: 280px; border-radius: 50%;
  background: radial-gradient(circle, rgba(0,113,227,.06) 0%, rgba(0,113,227,.02) 50%, transparent 70%);
  animation: corePulse 4s ease-in-out infinite;
}
@keyframes corePulse {
  0%,100% { transform: scale(1); opacity: .8; }
  50% { transform: scale(1.1); opacity: 1; }
}
.orbit {
  position: absolute; top:50%; left:50%;
  border-radius: 50%;
  border: 1px solid rgba(0,113,227,.1);
  transform: translate(-50%,-50%);
}
.orbit-1 { width:340px; height:340px; animation:spin1 20s linear infinite; }
.orbit-2 { width:420px; height:420px; animation:spin2 25s linear infinite; }
.orbit-3 { width:500px; height:500px; animation:spin3 30s linear infinite; }
@keyframes spin1 { from{transform:translate(-50%,-50%) rotate(0deg);} to{transform:translate(-50%,-50%) rotate(360deg);} }
@keyframes spin2 { from{transform:translate(-50%,-50%) rotate(0deg);} to{transform:translate(-50%,-50%) rotate(-360deg);} }
@keyframes spin3 { from{transform:translate(-50%,-50%) rotate(0deg);} to{transform:translate(-50%,-50%) rotate(360deg);} }

.orbit-dot {
  position: absolute; width:8px; height:8px; border-radius:50%;
  background: var(--primary);
  box-shadow: 0 0 12px rgba(0,113,227,.4);
}
.orbit-1 .orbit-dot { top:0; left:50%; transform:translate(-50%,-50%); }
.orbit-2 .orbit-dot { bottom:0; left:50%; transform:translate(-50%,50%); }
.orbit-3 .orbit-dot { top:50%; right:0; transform:translate(50%,-50%); }

.float-dots { position: absolute; inset: 0; }
.float-dot {
  position: absolute; width:6px; height:6px; border-radius:50%;
  background: var(--primary); opacity:.3;
}
.float-dot:nth-child(1){ top:15%; left:25%; animation:dotFloat 5s ease-in-out infinite; }
.float-dot:nth-child(2){ top:70%; left:20%; animation:dotFloat 6s ease-in-out infinite .5s; }
.float-dot:nth-child(3){ top:30%; right:20%; animation:dotFloat 7s ease-in-out infinite 1s; }
.float-dot:nth-child(4){ top:60%; right:25%; animation:dotFloat 5.5s ease-in-out infinite 1.5s; }
.float-dot:nth-child(5){ top:45%; left:45%; animation:dotFloat 6.5s ease-in-out infinite 2s; }
@keyframes dotFloat {
  0%,100% { transform: translateY(0); opacity:.3; }
  50% { transform: translateY(-20px); opacity:.6; }
}

/* 主文案 */
.hero-content { position: relative; z-index:2; text-align:center; max-width:720px; }
.hero-badge {
  display: inline-flex; align-items:center; gap:6px;
  height:28px; padding:0 12px; border-radius:14px;
  background: rgba(0,113,227,.08); color: var(--primary);
  font-size:12px; font-weight:600; margin-bottom:24px;
}
.badge-dot {
  width:6px; height:6px; border-radius:50%;
  background: var(--primary);
  animation: corePulse 2s ease-in-out infinite;
}
.hero-title {
  font-size: clamp(36px, 6vw, 60px);
  font-weight:800; letter-spacing:-.03em; line-height:1.1;
  color: var(--text); margin-bottom:20px;
}
.hero-title span {
  background: linear-gradient(135deg, #0071e3 0%, #40a9ff 50%, #1e8a9e 100%);
  -webkit-background-clip:text; -webkit-text-fill-color:transparent; background-clip:text;
}
.hero-desc {
  font-size: clamp(16px, 2vw, 19px);
  color: var(--text-2); line-height:1.6;
  max-width:540px; margin:0 auto 36px;
}
.btn-download {
  display: inline-flex; align-items:center; gap:10px;
  height:50px; padding:0 32px; border-radius:25px; border:none;
  background: var(--primary); color:#fff;
  font-size:16px; font-weight:600; font-family: var(--font);
  cursor: pointer; transition: var(--tr);
  box-shadow: 0 4px 20px rgba(0,113,227,.3);
  text-decoration: none;
}
.btn-download:hover {
  background: var(--primary-h);
  box-shadow: 0 6px 28px rgba(0,113,227,.4);
  transform: translateY(-1px);
}
.btn-download:active { transform: scale(.98); }
.btn-download svg { width:20px; height:20px; }

.hero-platforms {
  display: flex; align-items:center; justify-content:center;
  gap:20px; margin-top:32px;
  font-size:12px; color: var(--text-3);
}
.hero-platforms .sep { width:1px; height:12px; background: var(--border); }

.scroll-hint {
  position: absolute; bottom:32px; left:50%; transform:translateX(-50%);
  z-index:2; display:flex; flex-direction:column; align-items:center; gap:6px;
  color: var(--text-3); font-size:11px;
  animation: bounce 2s ease-in-out infinite;
}
.scroll-hint svg { width:16px; height:16px; }
@keyframes bounce {
  0%,100% { transform:translateX(-50%) translateY(0); }
  50% { transform:translateX(-50%) translateY(8px); }
}

/* ============================================================
   FEATURES SECTION — Trae-style large alternating cards
   ============================================================ */
.section { padding:120px 40px; position:relative; z-index:1; }
.section-label {
  display:inline-block;
  font-size:11px; font-weight:700; text-transform:uppercase;
  letter-spacing:.08em; color:var(--primary); margin-bottom:12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 40px);
  font-weight:800; letter-spacing:-.02em; color:var(--text); margin-bottom:16px;
}
.section-desc { font-size:16px; color:var(--text-2); max-width:520px; line-height:1.6; }
.section-header { text-align:center; margin-bottom:80px; }
.section-header .section-desc { margin:0 auto; }

/* Large alternating feature rows */
.feature-row {
  display:flex; align-items:center; gap:80px;
  max-width:1100px; margin:0 auto 100px; min-height:420px;
}
.feature-row:last-child { margin-bottom:0; }
.feature-row.reverse { flex-direction:row-reverse; }

/* Demo visual area */
.feature-visual {
  flex:1; position:relative;
  min-width:380px; height:420px;
  display:flex; align-items:center; justify-content:center;
}
.demo-frame {
  width:340px; height:380px;
  background:var(--surface); border:1px solid var(--border-l);
  border-radius:24px;
  box-shadow:0 20px 60px rgba(0,0,0,.06), 0 4px 12px rgba(0,0,0,.03);
  position:relative; overflow:hidden;
  display:flex; flex-direction:column;
}
.demo-frame-header {
  height:44px; background:var(--bg); border-bottom:1px solid var(--border-l);
  display:flex; align-items:center; padding:0 16px; gap:8px; flex-shrink:0;
}
.demo-frame-header .dot-row { display:flex; gap:5px; }
.demo-frame-header .dot-row span {
  width:8px; height:8px; border-radius:50%; background:var(--border);
}
.demo-frame-header .dot-row span:first-child { background:#ff5f57; }
.demo-frame-header .dot-row span:nth-child(2) { background:#ffbd2e; }
.demo-frame-header .dot-row span:nth-child(3) { background:#28ca41; }
.demo-frame-title {
  flex:1; text-align:center;
  font-size:11px; font-weight:600; color:var(--text-3); margin-right:36px;
}
.demo-frame-body {
  flex:1; padding:20px;
  display:flex; flex-direction:column; gap:0; overflow:hidden; position:relative;
}

/* --- Scenario: 社区网络 --- */
.scenario-community { display:flex; flex-direction:column; gap:10px; }
.community-card {
  display:flex; align-items:center; gap:10px;
  padding:10px 12px; background:var(--bg); border-radius:12px;
  animation:cardSlideIn .6s ease-out both;
}
.community-card:nth-child(1){ animation-delay:0s; }
.community-card:nth-child(2){ animation-delay:.15s; }
.community-card:nth-child(3){ animation-delay:.3s; }
.community-card:nth-child(4){ animation-delay:.45s; }
@keyframes cardSlideIn {
  from { opacity:0; transform:translateX(-20px); }
  to { opacity:1; transform:translateX(0); }
}
.comm-avatar {
  width:36px; height:36px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:14px; font-weight:700; color:#fff; flex-shrink:0;
  animation:avatarPulse 3s ease-in-out infinite;
}
@keyframes avatarPulse {
  0%,100% { box-shadow:0 0 0 0 rgba(0,113,227,.3); }
  50% { box-shadow:0 0 0 6px rgba(0,113,227,0); }
}
.comm-avatar.a1 { background:linear-gradient(135deg,#0071e3,#40a9ff); }
.comm-avatar.a3 { background:linear-gradient(135deg,#ff9500,#ff9f0a); }
.comm-avatar.a4 { background:linear-gradient(135deg,#af52de,#bf5af2); }
.comm-avatar.a6 { background:linear-gradient(135deg,#1e8a9e,#30b4c8); }
.comm-info { flex:1; min-width:0; }
.comm-name { font-size:12px; font-weight:600; color:var(--text); }
.comm-role { font-size:10px; color:var(--text-3); }
.comm-dot {
  width:8px; height:8px; border-radius:50%; background:#34c759; flex-shrink:0;
  animation:corePulse 2s ease-in-out infinite;
}
.conn-lines { position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; }
.conn-line {
  position:absolute; height:1px;
  background:linear-gradient(90deg, rgba(0,113,227,.2), rgba(0,113,227,.05));
  animation:lineReveal 4s ease-in-out infinite;
}
.conn-line:nth-child(1){ top:28%; left:8%; width:30%; animation-delay:0s; }
.conn-line:nth-child(2){ top:48%; right:8%; width:25%; animation-delay:1s; }
.conn-line:nth-child(3){ top:68%; left:12%; width:28%; animation-delay:2s; }
@keyframes lineReveal {
  0%,100% { opacity:0; transform:scaleX(0); }
  20%,80% { opacity:1; transform:scaleX(1); }
}

/* --- Scenario: 活动管理 --- */
.scenario-activity { display:flex; flex-direction:column; }
.cal-header {
  display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;
}
.cal-month { font-size:13px; font-weight:700; color:var(--text); }
.cal-nav { display:flex; gap:4px; }
.cal-nav-btn {
  width:24px; height:24px; border-radius:6px; background:var(--bg); border:none;
  display:flex; align-items:center; justify-content:center;
  cursor:pointer; font-size:12px; color:var(--text-2);
}
.cal-grid {
  display:grid; grid-template-columns:repeat(7,1fr); gap:4px;
  text-align:center; font-size:11px; margin-bottom:14px;
}
.cal-day-name { color:var(--text-3); font-weight:600; font-size:10px; }
.cal-day {
  width:100%; aspect-ratio:1; border-radius:8px;
  display:flex; align-items:center; justify-content:center;
  font-weight:500; color:var(--text); position:relative;
}
.cal-day.today {
  background:var(--primary); color:#fff; font-weight:700;
  animation:todayGlow 2s ease-in-out infinite;
}
@keyframes todayGlow {
  0%,100% { box-shadow:0 0 0 0 rgba(0,113,227,.3); }
  50% { box-shadow:0 0 0 8px rgba(0,113,227,0); }
}
.cal-day.has-event::after {
  content:''; position:absolute; bottom:3px; left:50%; transform:translateX(-50%);
  width:4px; height:4px; border-radius:50%; background:var(--primary);
}
.cal-day.other { color:var(--text-3); }
.event-list { display:flex; flex-direction:column; gap:6px; }
.event-item {
  display:flex; align-items:center; gap:8px;
  padding:8px 10px; background:var(--bg); border-radius:8px;
  border-left:3px solid var(--primary); font-size:11px;
  animation:eventSlide .5s ease-out both;
}
.event-item:nth-child(1){ animation-delay:.2s; }
.event-item:nth-child(2){ animation-delay:.4s; }
@keyframes eventSlide {
  from { opacity:0; transform:translateY(8px); }
  to { opacity:1; transform:translateY(0); }
}
.event-time { font-weight:700; color:var(--primary); font-family:var(--font-mono); font-size:10px; }
.event-title { flex:1; color:var(--text); }
.event-badge { font-size:10px; font-weight:600; padding:2px 6px; border-radius:4px; }
.event-badge.upcoming { background:rgba(0,113,227,.1); color:var(--primary); }
.event-badge.ongoing { background:rgba(52,199,89,.12); color:#34c759; }

/* --- Scenario: 求助响应 --- */
.help-badge-row { display:flex; gap:6px; margin-bottom:12px; }
.help-tag {
  font-size:10px; font-weight:600; padding:4px 8px;
  border-radius:10px; background:var(--bg); color:var(--text-2);
}
.help-tag.active { background:rgba(0,113,227,.1); color:var(--primary); }
.chat-list { display:flex; flex-direction:column; gap:8px; }
.chat-item {
  display:flex; gap:10px; padding:10px; background:var(--bg);
  border-radius:12px; animation:chatSlide .5s ease-out both; position:relative;
}
.chat-item:nth-child(1){ animation-delay:0s; }
.chat-item:nth-child(2){ animation-delay:.15s; }
.chat-item:nth-child(3){ animation-delay:.3s; }
@keyframes chatSlide {
  from { opacity:0; transform:translateY(10px); }
  to { opacity:1; transform:translateY(0); }
}
.chat-avatar {
  width:34px; height:34px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:13px; color:#fff; font-weight:700; flex-shrink:0;
}
.chat-avatar.urgent {
  background:linear-gradient(135deg,#ff3b30,#ff453a);
  animation:urgentBlink 1.5s ease-in-out infinite;
}
@keyframes urgentBlink {
  0%,100% { box-shadow:0 0 0 0 rgba(255,59,48,.4); }
  50% { box-shadow:0 0 0 8px rgba(255,59,48,0); }
}
.chat-avatar.normal { background:linear-gradient(135deg,#0071e3,#40a9ff); }
.chat-content { flex:1; min-width:0; }
.chat-name { font-size:11px; font-weight:600; color:var(--text); }
.chat-text {
  font-size:11px; color:var(--text-2);
  white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}
.chat-meta { display:flex; align-items:center; gap:6px; margin-top:2px; }
.chat-time { font-size:10px; color:var(--text-3); font-family:var(--font-mono); }
.chat-status { font-size:9px; font-weight:700; padding:1px 6px; border-radius:3px; }
.chat-status.waiting { background:rgba(255,149,0,.12); color:#ff9500; }
.chat-status.matched { background:rgba(52,199,89,.12); color:#34c759; animation:corePulse 2s ease-in-out infinite; }
.chat-badge {
  position:absolute; top:-3px; right:-3px;
  width:18px; height:18px; border-radius:50%;
  background:#ff3b30; color:#fff;
  font-size:9px; font-weight:700;
  display:flex; align-items:center; justify-content:center;
  animation:urgentBlink 1.5s ease-in-out infinite;
}

/* --- Scenario: 数据洞察 --- */
.scenario-stats { display:flex; flex-direction:column; gap:12px; }
.stats-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:8px; }
.stat-mini {
  background:var(--bg); border-radius:10px; padding:10px;
  text-align:center; animation:statPop .5s ease-out both;
}
.stat-mini:nth-child(1){ animation-delay:0s; }
.stat-mini:nth-child(2){ animation-delay:.1s; }
.stat-mini:nth-child(3){ animation-delay:.2s; }
@keyframes statPop {
  from { opacity:0; transform:scale(.9); }
  to { opacity:1; transform:scale(1); }
}
.stat-mini-val {
  font-size:18px; font-weight:800; color:var(--primary);
  font-family:var(--font-mono); letter-spacing:-.02em;
}
.stat-mini-label { font-size:9px; color:var(--text-3); margin-top:2px; }
.stat-mini-change { font-size:9px; font-weight:600; color:#34c759; margin-top:1px; }
.chart-area { background:var(--bg); border-radius:12px; padding:14px; }
.chart-title { font-size:11px; font-weight:600; color:var(--text); margin-bottom:10px; }
.bars { display:flex; align-items:flex-end; gap:8px; height:80px; }
.bar-wrap { flex:1; display:flex; flex-direction:column; align-items:center; gap:4px; }
.bar {
  width:100%;
  background:linear-gradient(180deg, var(--primary) 0%, rgba(0,113,227,.3) 100%);
  border-radius:4px 4px 0 0;
  animation:barGrow 1.5s ease-out both;
}
@keyframes barGrow { from { height:0!important; } }
.bar-wrap:nth-child(1) .bar { height:45px; animation-delay:0s; }
.bar-wrap:nth-child(2) .bar { height:68px; animation-delay:.1s; }
.bar-wrap:nth-child(3) .bar { height:52px; animation-delay:.2s; }
.bar-wrap:nth-child(4) .bar { height:75px; animation-delay:.3s; }
.bar-wrap:nth-child(5) .bar { height:60px; animation-delay:.4s; }
.bar-wrap:nth-child(6) .bar { height:80px; animation-delay:.5s; }
.bar-wrap:nth-child(7) .bar { height:55px; animation-delay:.6s; }
.bar-label { font-size:9px; color:var(--text-3); }

/* Feature text */
.feature-text { flex:1; min-width:280px; }
.feature-text .feature-icon {
  width:44px; height:44px; border-radius:12px;
  background:var(--primary-t);
  display:flex; align-items:center; justify-content:center;
  margin-bottom:20px; color:var(--primary);
}
.feature-text h3 {
  font-size:clamp(22px, 3vw, 28px);
  font-weight:800; letter-spacing:-.02em; margin-bottom:12px; color:var(--text);
}
.feature-text p { font-size:15px; color:var(--text-2); line-height:1.7; margin-bottom:20px; }
.feature-points { list-style:none; display:flex; flex-direction:column; gap:8px; }
.feature-points li {
  display:flex; align-items:flex-start; gap:8px;
  font-size:13px; color:var(--text-2); line-height:1.5;
}
.feature-points li svg { flex-shrink:0; margin-top:2px; color:var(--primary); }

/* ============================================================
   RESPONSIVE — Multi-breakpoint
   ============================================================ */

/* Tablet: 768px - 1024px */
@media (max-width: 1024px) {
  .hero { padding: 100px 32px 80px; }
  .hero-visual { transform: translate(-50%,-50%) scale(.7); }
  .section { padding: 80px 32px; }
  .section-header { margin-bottom: 60px; }
  .feature-row { gap: 40px; }
  .demo-frame { width: 280px; height: 320px; }
  .feature-visual { min-width: 280px; height: 360px; }
  .feature-text h3 { font-size: 22px; }
}

/* Mobile wide: 640px - 768px */
@media (max-width: 768px) {
  .hero { padding: 100px 24px 60px; }
  .hero-title { font-size: clamp(28px, 7vw, 36px); }
  .hero-desc { font-size: 15px; }
  .hero-visual { display: none; }
  .btn-download { height: 46px; padding: 0 28px; font-size: 15px; }
  .section { padding: 64px 24px; }
  .section-title { font-size: clamp(24px, 5vw, 32px); }
  .section-header { margin-bottom: 48px; }
  .feature-row {
    flex-direction: column !important;
    gap: 24px;
    margin-bottom: 48px;
    min-height: auto;
  }
  .feature-visual { min-width: auto; width: 100%; height: auto; }
  .demo-frame { width: 100%; max-width: 340px; height: 340px; margin: 0 auto; }
  .feature-text { min-width: auto; }
  .feature-text h3 { font-size: 20px; }
  .feature-text p { font-size: 14px; }
}

/* Mobile small: < 640px */
@media (max-width: 640px) {
  .hero { padding: 88px 16px 48px; min-height: auto; }
  .hero-badge { height: 24px; padding: 0 10px; font-size: 11px; }
  .hero-title { font-size: 26px; }
  .hero-desc { font-size: 14px; }
  .btn-download {
    width: 100%;
    max-width: 280px;
    height: 44px;
    padding: 0 20px;
    font-size: 14px;
    justify-content: center;
  }
  .btn-download svg { width: 18px; height: 18px; }
  .hero-platforms { gap: 0; }
  .section { padding: 48px 16px; }
  .section-label { font-size: 10px; }
  .section-title { font-size: 22px; }
  .section-desc { font-size: 14px; }
  .section-header { margin-bottom: 36px; }
  .demo-frame { width: 100%; max-width: 320px; height: auto; min-height: 280px; border-radius: 18px; margin: 0 auto; }
  .demo-frame-header { height: 36px; padding: 0 12px; }
  .demo-frame-title { font-size: 10px; margin-right: 28px; }
  .demo-frame-header .dot-row span { width: 6px; height: 6px; }
  .demo-frame-body { padding: 10px; }
  .feature-text h3 { font-size: 18px; }
  .feature-text p { font-size: 13px; }
  .feature-points li { font-size: 12px; }

  /* --- 社区卡片内部 --- */
  .scenario-community { gap: 6px; }
  .comm-avatar { width: 26px; height: 26px; font-size: 11px; }
  .community-card { padding: 6px 8px; gap: 6px; }
  .comm-name { font-size: 10px; }
  .comm-role { font-size: 9px; }
  .comm-dot { width: 6px; height: 6px; }

  /* --- 日历内部 --- */
  .cal-header { margin-bottom: 8px; }
  .cal-month { font-size: 11px; }
  .cal-nav-btn { width: 20px; height: 20px; font-size: 10px; }
  .cal-grid { gap: 2px; margin-bottom: 8px; }
  .cal-day-name { font-size: 8px; }
  .cal-day { font-size: 9px; border-radius: 6px; }
  .cal-day.has-event::after { width: 3px; height: 3px; bottom: 2px; }
  .event-list { gap: 4px; }
  .event-item { padding: 5px 8px; gap: 4px; font-size: 10px; }
  .event-time { font-size: 9px; }
  .event-badge { font-size: 8px; padding: 1px 4px; }

  /* --- 求助响应内部 --- */
  .help-badge-row { gap: 3px; margin-bottom: 8px; }
  .help-tag { font-size: 8px; padding: 2px 6px; }
  .chat-list { gap: 5px; }
  .chat-item { padding: 6px 8px; gap: 6px; }
  .chat-avatar { width: 24px; height: 24px; font-size: 10px; }
  .chat-name { font-size: 10px; }
  .chat-text { font-size: 9px; }
  .chat-time { font-size: 8px; }
  .chat-status { font-size: 7px; padding: 0 4px; }
  .chat-badge { width: 14px; height: 14px; font-size: 7px; top: -2px; right: -2px; }

  /* --- 数据洞察内部 --- */
  .scenario-stats { gap: 8px; }
  .stats-cards { gap: 3px; }
  .stat-mini { padding: 6px 4px; border-radius: 8px; }
  .stat-mini-val { font-size: 13px; }
  .stat-mini-label { font-size: 7px; }
  .stat-mini-change { font-size: 7px; }
  .chart-area { padding: 10px; border-radius: 10px; }
  .chart-title { font-size: 9px; margin-bottom: 6px; }
  .bars { height: 50px; gap: 4px; }
  .bar-label { font-size: 7px; }
}
</style>
