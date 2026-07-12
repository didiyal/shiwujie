<template>
  <div class="official-layout">
    <!-- ==================== NAVBAR ==================== -->
    <header class="navbar" :class="{ scrolled: isScrolled }">
      <router-link to="/" class="navbar-brand">
        <div class="brand-icon">视</div>
        <div class="brand-text">视<span>无</span>界</div>
      </router-link>

      <!-- Desktop nav -->
      <nav class="nav-links">
        <router-link to="/">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="20" rx="5"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
          产品
        </router-link>
        <router-link to="/changelog">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          更新日志
        </router-link>
        <a href="https://github.com" target="_blank" rel="noopener">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
          GitHub
        </a>
      </nav>

      <!-- Right side actions -->
      <div class="navbar-actions">
        <button class="btn-admin" @click="goAdmin">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
          管理端入口
        </button>
        <!-- Hamburger -->
        <button class="hamburger" :class="{ open: menuOpen }" @click="menuOpen = !menuOpen" aria-label="菜单">
          <span></span><span></span><span></span>
        </button>
      </div>
    </header>

    <!-- Mobile nav overlay -->
    <div class="mobile-nav-overlay" :class="{ open: menuOpen }" @click="menuOpen = false"></div>
    <nav class="mobile-nav" :class="{ open: menuOpen }">
      <router-link to="/" @click="menuOpen = false">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="20" rx="5"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
        产品
      </router-link>
      <router-link to="/changelog" @click="menuOpen = false">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        更新日志
      </router-link>
      <a href="https://github.com" target="_blank" rel="noopener" @click="menuOpen = false">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
        GitHub
      </a>
      <button class="btn-admin" @click="goAdmin(); menuOpen = false">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
        管理端入口
      </button>
    </nav>

    <!-- ==================== CONTENT ==================== -->
    <main class="official-content">
      <router-view />
    </main>

    <!-- ==================== FOOTER ==================== -->
    <footer class="footer">
      <div class="footer-left">
        <div class="footer-brand-icon">视</div>
        <span>&copy; {{ year }} 视无界. All rights reserved.</span>
      </div>
      <div class="footer-links">
        <a href="#">隐私政策</a>
        <a href="#">服务条款</a>
        <a href="#">联系我们</a>
        <a href="#" @click.prevent="goAdmin">管理端</a>
      </div>
    </footer>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'OfficialLayout',
  setup() {
    const router = useRouter()
    const isScrolled = ref(false)
    const menuOpen = ref(false)
    const year = new Date().getFullYear()

    const onScroll = () => {
      isScrolled.value = window.scrollY > 50
    }

    const goAdmin = () => {
      router.push('/admin')
    }

    return {
      isScrolled,
      menuOpen,
      year,
      goAdmin,
      onScroll
    }
  },
  mounted() {
    window.addEventListener('scroll', this.onScroll, { passive: true })
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.onScroll)
  }
}
</script>

<style scoped>
/* ============================================================
   NAVBAR
   ============================================================ */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 56px;
  background: rgba(245,245,247,.72);
  backdrop-filter: saturate(180%) blur(24px);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  border-bottom: 1px solid rgba(0,0,0,.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  transition: background .25s ease, box-shadow .25s ease;
}
.navbar.scrolled {
  background: rgba(245,245,247,.88);
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}

/* Logo */
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text);
  flex-shrink: 0;
}
.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0071e3 0%, #40a9ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}
.brand-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -.01em;
  color: var(--text);
}
.brand-text span { color: var(--primary); }

/* Desktop nav links */
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}
.nav-links a {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
  text-decoration: none;
  transition: var(--tr);
  white-space: nowrap;
}
.nav-links a:hover {
  color: var(--text);
  background: rgba(0,0,0,.04);
}
.nav-links a.router-link-active {
  color: var(--text);
  background: rgba(0,0,0,.04);
}
.nav-links a svg {
  width: 16px;
  height: 16px;
  opacity: .6;
}

/* Right side */
.navbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* Desktop admin button */
.btn-admin {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border-radius: 17px;
  border: none;
  background: #1d1d1f;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: var(--tr);
  white-space: nowrap;
}
.btn-admin:hover {
  background: #000;
  transform: scale(1.02);
}
.btn-admin:active { transform: scale(.98); }

/* Hamburger button — hidden on desktop */
.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}
.hamburger:hover { background: rgba(0,0,0,.04); }
.hamburger span {
  display: block;
  width: 18px;
  height: 2px;
  border-radius: 1px;
  background: var(--text);
  transition: var(--tr);
  margin: 0 auto;
}
.hamburger.open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.hamburger.open span:nth-child(2) {
  opacity: 0;
}
.hamburger.open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* Mobile nav overlay */
.mobile-nav-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.3);
  z-index: 998;
  opacity: 0;
  transition: opacity .25s ease;
}
.mobile-nav-overlay.open {
  opacity: 1;
}

/* Mobile nav panel */
.mobile-nav {
  display: none;
  position: fixed;
  top: 56px;
  right: 0;
  width: 260px;
  bottom: 0;
  background: var(--surface);
  z-index: 999;
  padding: 16px;
  transform: translateX(100%);
  transition: transform .3s cubic-bezier(.25,.1,.25,1);
  flex-direction: column;
  gap: 4px;
  border-left: 1px solid var(--border-l);
  box-shadow: -4px 0 24px rgba(0,0,0,.08);
}
.mobile-nav.open {
  transform: translateX(0);
}
.mobile-nav a {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  padding: 0 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  text-decoration: none;
  transition: var(--tr);
}
.mobile-nav a:hover,
.mobile-nav a.router-link-active {
  background: var(--primary-t);
  color: var(--primary);
}
.mobile-nav a svg {
  width: 18px;
  height: 18px;
  opacity: .6;
}
.mobile-nav .btn-admin {
  margin-top: 12px;
  justify-content: center;
  height: 40px;
  border-radius: 20px;
  font-size: 14px;
}

/* ============================================================
   FOOTER
   ============================================================ */
.footer {
  border-top: 1px solid var(--border-l);
  padding: 32px 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  background: var(--bg);
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-3);
}
.footer-brand-icon {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: linear-gradient(135deg, #0071e3 0%, #40a9ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.footer-links {
  display: flex;
  align-items: center;
  gap: 16px;
}
.footer-links a {
  font-size: 12px;
  color: var(--text-2);
  text-decoration: none;
  transition: var(--tr);
}
.footer-links a:hover { color: var(--primary); }

/* ============================================================
   RESPONSIVE — Multi-breakpoint
   ============================================================ */

/* Tablet: 768px - 1024px */
@media (max-width: 1024px) {
  .navbar {
    padding: 0 24px;
  }
  .nav-links a {
    padding: 0 10px;
    font-size: 12px;
  }
  .btn-admin {
    padding: 0 12px;
    font-size: 12px;
  }
}

/* Mobile: < 768px */
@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }
  .navbar-brand .brand-text {
    font-size: 15px;
  }
  .brand-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
    border-radius: 7px;
  }

  /* Hide desktop nav, show hamburger */
  .nav-links { display: none; }
  .navbar-actions .btn-admin { display: none; }
  .hamburger { display: flex; }

  /* Show mobile components */
  .mobile-nav-overlay { display: block; }
  .mobile-nav { display: flex; }

  .footer {
    flex-direction: column;
    text-align: center;
    padding: 24px 20px;
  }
}
</style>
