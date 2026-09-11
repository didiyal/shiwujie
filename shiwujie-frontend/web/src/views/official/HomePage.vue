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
          开口即所得<br>用 AI <span>看见</span>世界
        </h1>
        <p class="hero-desc">
          视无界是专为视障人士打造的 AI 智能助手：开口就能用——拍照识物、联网搜索、
          打开应用、规划路线、一键求助，全部语音完成；退出应用还有 AI 悬浮球，一点即回。
        </p>
        <a href="/api/download/app" class="btn-download">
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
        <h2 class="section-title">一句话，<br>让 AI 成为你<span v-pre>的</span>眼睛</h2>
        <p class="section-desc">不用学习复杂操作，说出需求即可：识别、搜索、导航、求助，AI 全程语音响应。</p>
      </div>

      <!-- 场景一：AI 语音对话 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">AI 对话</div>
            </div>
            <div class="demo-frame-body">
              <div class="chat-list">
                <div class="chat-item">
                  <div class="chat-avatar normal">我</div>
                  <div class="chat-content">
                    <div class="chat-name">我</div>
                    <div class="chat-text">帮我打开微信</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar ai">AI</div>
                  <div class="chat-content">
                    <div class="chat-name">AI 助手</div>
                    <div class="chat-text">好的，正在为您打开微信…</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar normal">我</div>
                  <div class="chat-content">
                    <div class="chat-name">我</div>
                    <div class="chat-text">附近有哪些盲人按摩店？</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar ai">AI</div>
                  <div class="chat-content">
                    <div class="chat-name">AI 助手</div>
                    <div class="chat-text">为您找到 3 家，最近的一家在…</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/><path d="M19 10v2a7 7 0 01-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
          </div>
          <h3>AI 语音对话</h3>
          <p>像和朋友聊天一样说出需求，AI 语音回答。能联网搜索最新信息，也能操控手机——打开应用、规划路线，说一声就办到。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 语音提问、语音回答，全程免打字</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 联网搜索：最新资讯、周边信息即时可查</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 语音操控：说“帮我打开微信”即可打开应用，说“我要去哪里”快捷规划导航</li>
          </ul>
        </div>
      </div>

      <!-- 场景二：AI 拍照识别 -->
      <div class="feature-row reverse">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">拍照识别</div>
            </div>
            <div class="demo-frame-body">
              <div class="photo-demo">
                <div class="photo-frame">
                  <div class="photo-corner tl"></div><div class="photo-corner tr"></div>
                  <div class="photo-corner bl"></div><div class="photo-corner br"></div>
                  <div class="photo-mid"></div>
                </div>
                <div class="photo-result">
                  <div class="photo-result-line"><span class="photo-key">识别</span>这是一盒感冒灵冲剂</div>
                  <div class="photo-result-line"><span class="photo-key">追问</span>一次喝多少？</div>
                  <div class="photo-result-line"><span class="photo-key">回答</span>开水冲服，一次 1 袋…</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>
          </div>
          <h3>AI 拍照识别</h3>
          <p>对 AI 说“帮我识别前面”，自动拍照并告诉你眼前是什么。看不完的信息可以继续追问，让它读药品说明、辨物品、识场景。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 一句“帮我识别前面”，自动拍照语音播报</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 识别后可追问细节，像有个随身讲解员</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 结合联网搜索，识别之外还能补充背景信息</li>
          </ul>
        </div>
      </div>

      <!-- 场景三：紧急与志愿者求助 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">一键求助</div>
            </div>
            <div class="demo-frame-body">
              <div class="sos-demo">
                <div class="sos-btn">
                  <div class="sos-icon">SOS</div>
                  <div class="sos-label">紧急求助</div>
                </div>
                <div class="volunteer-btn">
                  <div class="volunteer-icon">志愿者</div>
                  <div class="volunteer-label">视频帮扶</div>
                </div>
                <div class="sos-status">
                  <div class="sos-dot"></div>
                  <span>已通知家属 · 志愿者接入中…</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z"/></svg>
          </div>
          <h3>紧急与志愿者求助</h3>
          <p>说“帮我紧急求助”，AI 立即向家属发起视频通话求助；需要人帮您“看”时，一键视频连线志愿者——AI 也能自动触发求助。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 紧急求助一键通知家属，视频通话即时接通</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 志愿者视频帮扶，远程“成为您的眼睛”</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> AI 识别到紧急场景可主动协助发起求助</li>
          </ul>
        </div>
      </div>

      <!-- 场景四：AI 悬浮球 -->
      <div class="feature-row reverse">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">AI 悬浮球</div>
            </div>
            <div class="demo-frame-body">
              <div class="ball-demo">
                <div class="phone-mock">
                  <div class="phone-screen">
                    <div class="phone-app-row"><div class="phone-app"></div><div class="phone-app"></div><div class="phone-app"></div></div>
                    <div class="phone-app-row"><div class="phone-app"></div><div class="phone-app"></div><div class="phone-app"></div></div>
                    <div class="floating-ball">
                      <div class="ball-core">AI</div>
                      <div class="ball-ring"></div>
                    </div>
                  </div>
                </div>
                <div class="ball-caption">退出应用后悬浮球常驻<br>点击即回到 AI 助手</div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/></svg>
          </div>
          <h3>AI 悬浮球，一点即回</h3>
          <p>退出视无界去做别的事时，屏幕上会保留一枚 AI 悬浮球；无论当时在用什么应用，点一下悬浮球立刻回到 AI 助手，AI 不离左右。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 退出应用后悬浮球常驻屏幕</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 任何界面点击悬浮球，直达 AI 对话</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 退到后台有语音提示，不担心“回不来”</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- ==================== FEATURES ==================== -->
    <section class="section" id="features">
      <div class="section-header">
        <div class="section-label">产品功能</div>
        <h2 class="section-title">一句话，<br>让 AI 成为你<span v-pre>的</span>眼睛</h2>
        <p class="section-desc">不用学习复杂操作，说出需求即可：识别、搜索、导航、求助，AI 全程语音响应。</p>
      </div>

      <!-- 场景一：AI 语音对话 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">AI 对话</div>
            </div>
            <div class="demo-frame-body">
              <div class="chat-list">
                <div class="chat-item">
                  <div class="chat-avatar normal">我</div>
                  <div class="chat-content">
                    <div class="chat-name">我</div>
                    <div class="chat-text">帮我打开微信</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar ai">AI</div>
                  <div class="chat-content">
                    <div class="chat-name">AI 助手</div>
                    <div class="chat-text">好的，正在为您打开微信…</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar normal">我</div>
                  <div class="chat-content">
                    <div class="chat-name">我</div>
                    <div class="chat-text">附近有哪些盲人按摩店？</div>
                  </div>
                </div>
                <div class="chat-item">
                  <div class="chat-avatar ai">AI</div>
                  <div class="chat-content">
                    <div class="chat-name">AI 助手</div>
                    <div class="chat-text">为您找到 3 家，最近的一家在…</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/><path d="M19 10v2a7 7 0 01-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
          </div>
          <h3>AI 语音对话</h3>
          <p>像和朋友聊天一样说出需求，AI 语音回答。能联网搜索最新信息，也能操控手机——打开应用、规划路线，说一声就办到。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 语音提问、语音回答，全程免打字</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 联网搜索：最新资讯、周边信息即时可查</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 语音操控：说“帮我打开微信”即可打开应用，说“我要去哪里”快捷规划导航</li>
          </ul>
        </div>
      </div>

      <!-- 场景二：AI 拍照识别 -->
      <div class="feature-row reverse">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">拍照识别</div>
            </div>
            <div class="demo-frame-body">
              <div class="photo-demo">
                <div class="photo-frame">
                  <div class="photo-corner tl"></div><div class="photo-corner tr"></div>
                  <div class="photo-corner bl"></div><div class="photo-corner br"></div>
                  <div class="photo-mid"></div>
                </div>
                <div class="photo-result">
                  <div class="photo-result-line"><span class="photo-key">识别</span>这是一盒感冒灵冲剂</div>
                  <div class="photo-result-line"><span class="photo-key">追问</span>一次喝多少？</div>
                  <div class="photo-result-line"><span class="photo-key">回答</span>开水冲服，一次 1 袋…</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>
          </div>
          <h3>AI 拍照识别</h3>
          <p>对 AI 说“帮我识别前面”，自动拍照并告诉你眼前是什么。看不完的信息可以继续追问，让它读药品说明、辨物品、识场景。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 一句“帮我识别前面”，自动拍照语音播报</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 识别后可追问细节，像有个随身讲解员</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 结合联网搜索，识别之外还能补充背景信息</li>
          </ul>
        </div>
      </div>

      <!-- 场景三：紧急与志愿者求助 -->
      <div class="feature-row">
        <div class="feature-visual">
          <div class="demo-frame">
            <div class="demo-frame-header">
              <div class="dot-row"><span></span><span></span><span></span></div>
              <div class="demo-frame-title">一键求助</div>
            </div>
            <div class="demo-frame-body">
              <div class="sos-demo">
                <div class="sos-btn">
                  <div class="sos-icon">SOS</div>
                  <div class="sos-label">紧急求助</div>
                </div>
                <div class="volunteer-btn">
                  <div class="volunteer-icon">志愿者</div>
                  <div class="volunteer-label">视频帮扶</div>
                </div>
                <div class="sos-status">
                  <div class="sos-dot"></div>
                  <span>已通知家属 · 志愿者接入中…</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="feature-text">
          <div class="feature-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z"/></svg>
          </div>
          <h3>紧急与志愿者求助</h3>
          <p>说“帮我紧急求助”，AI 立即向家属发起视频通话求助；需要人帮您“看”时，一键视频连线志愿者——AI 也能自动触发求助。</p>
          <ul class="feature-points">
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 紧急求助一键通知家属，视频通话即时接通</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> 志愿者视频帮扶，远程“成为您的眼睛”</li>
            <li><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> AI 识别到紧急场景可主动协助发起求助</li>
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
  name: 'HomePage'
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
  position: relative; z-index: 10;
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
  pointer-events: none;
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

/* --- Scenario: AI 对话（复用 chat-item，补 AI 头像） --- */
.chat-avatar.ai { background:linear-gradient(135deg,#1e8a9e,#30b4c8); }

/* --- Scenario: 拍照识别 --- */
.photo-demo { display:flex; flex-direction:column; gap:14px; }
.photo-frame {
  position:relative; height:110px; border-radius:12px;
  background:linear-gradient(135deg, rgba(0,113,227,.08), rgba(30,138,158,.12));
}
.photo-corner { position:absolute; width:20px; height:20px; border:2px solid var(--primary); }
.photo-corner.tl { top:10px; left:10px; border-right:none; border-bottom:none; border-radius:6px 0 0 0; }
.photo-corner.tr { top:10px; right:10px; border-left:none; border-bottom:none; border-radius:0 6px 0 0; }
.photo-corner.bl { bottom:10px; left:10px; border-right:none; border-top:none; border-radius:0 0 0 6px; }
.photo-corner.br { bottom:10px; right:10px; border-left:none; border-top:none; border-radius:0 0 6px 0; }
.photo-mid {
  position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);
  width:36px; height:36px; border-radius:50%;
  background:rgba(0,113,227,.15); animation:corePulse 2s ease-in-out infinite;
}
.photo-result { display:flex; flex-direction:column; gap:6px; }
.photo-result-line { font-size:11px; color:var(--text-2); display:flex; align-items:center; gap:6px; }
.photo-key {
  font-size:9px; font-weight:700; padding:1px 6px; border-radius:4px;
  background:rgba(0,113,227,.1); color:var(--primary); flex-shrink:0;
}

/* --- Scenario: 一键求助 --- */
.sos-demo { display:flex; flex-direction:column; gap:12px; align-items:center; }
.sos-btn, .volunteer-btn {
  width:100%; display:flex; align-items:center; gap:10px;
  padding:12px 14px; border-radius:12px; animation:cardSlideIn .6s ease-out both;
}
.sos-btn { background:linear-gradient(135deg,#ff3b30,#ff453a); animation-delay:0s; }
.volunteer-btn { background:linear-gradient(135deg,#0071e3,#40a9ff); animation-delay:.15s; }
.sos-icon {
  width:34px; height:34px; border-radius:50%; background:rgba(255,255,255,.25);
  display:flex; align-items:center; justify-content:center;
  font-size:11px; font-weight:800; color:#fff; flex-shrink:0;
}
.volunteer-icon {
  width:34px; height:34px; border-radius:50%; background:rgba(255,255,255,.25);
  display:flex; align-items:center; justify-content:center;
  font-size:10px; font-weight:700; color:#fff; flex-shrink:0;
}
.sos-label, .volunteer-label { font-size:14px; font-weight:700; color:#fff; }
.sos-status {
  display:flex; align-items:center; gap:8px;
  font-size:10px; color:var(--text-3);
}
.sos-dot {
  width:7px; height:7px; border-radius:50%; background:#34c759;
  animation:corePulse 2s ease-in-out infinite;
}

/* --- Scenario: AI 悬浮球 --- */
.ball-demo { display:flex; flex-direction:column; align-items:center; gap:12px; }
.phone-mock {
  width:130px; height:190px; border-radius:18px;
  border:2px solid var(--border-l); background:var(--bg);
  padding:12px 10px;
}
.phone-screen { position:relative; height:100%; }
.phone-app-row { display:flex; gap:8px; margin-bottom:10px; }
.phone-app {
  width:26px; height:26px; border-radius:7px;
  background:linear-gradient(135deg, rgba(0,113,227,.15), rgba(30,138,158,.15));
}
.floating-ball {
  position:absolute; bottom:14px; right:6px;
  width:40px; height:40px;
}
.ball-core {
  position:absolute; inset:6px; border-radius:50%;
  background:linear-gradient(135deg,#0071e3,#40a9ff);
  display:flex; align-items:center; justify-content:center;
  font-size:12px; font-weight:800; color:#fff;
  animation:corePulse 2s ease-in-out infinite;
}
.ball-ring {
  position:absolute; inset:0; border-radius:50%;
  border:1.5px dashed rgba(0,113,227,.4);
  animation:spin1 10s linear infinite;
}
.ball-caption { font-size:10px; color:var(--text-3); text-align:center; line-height:1.6; }

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
