import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import HomeView from "@/views/Home/HomePage.vue";
import ActivityContent from "@/views/volunteer/ActivityContent.vue";
import Activity from "@/views/volunteer/Activity.vue";
import Community from "@/views/volunteer/Community.vue";
import RegisterAndLogin from "@/views/login/RegisterAndLogin.vue";
import BasicLayout from "@/layouts/BasicLayout.vue";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    redirect: "/home",
  },
  {
    path: "/home",
    name: "home",
    component: HomeView,
    meta: { hideHeader: true }
  },
  {
    path: "/login",
    name: "login",
    component: RegisterAndLogin,
  },
  {
    path: "/volunteer",
    component: BasicLayout,
    redirect: "/volunteer/community",
    children: [
      {
        path: "community",
        name: "volunteerCommunity",
        component: Community,
        meta: { requiresAuth: true },
      },
      {
        path: "activity",
        name: "volunteerActivity",
        component: Activity,
        meta: { requiresAuth: true },
      },
      {
        path: "activityContent",
        name: "volunteerActivityContent",
        component: ActivityContent,
        meta: { requiresAuth: true },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem("isLogin") === "true";

  if (to.meta.requiresAuth && !isAuthenticated) {
    next("/login");
  } else {
    next();
  }
});

export default router;
