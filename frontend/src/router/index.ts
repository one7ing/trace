import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/KnowledgeChat.vue'),
  },
  {
    path: '/practice',
    name: 'PracticeHome',
    component: () => import('@/views/practice/PracticeHome.vue'),
  },
  {
    path: '/practice/start',
    name: 'PracticeSession',
    component: () => import('@/views/practice/PracticeSession.vue'),
  },
  {
    path: '/practice/history',
    name: 'PracticeHistory',
    component: () => import('@/views/practice/PracticeHistory.vue'),
  },
  {
    path: '/practice/history/:id',
    name: 'PracticeDetail',
    component: () => import('@/views/practice/PracticeDetail.vue'),
  },
  {
    path: '/diary',
    name: 'DiaryList',
    component: () => import('@/views/diary/DiaryList.vue'),
  },
  {
    path: '/diary/write',
    name: 'DiaryWrite',
    component: () => import('@/views/diary/DiaryWrite.vue'),
  },
  {
    path: '/diary/:id',
    name: 'DiaryDetail',
    component: () => import('@/views/diary/DiaryDetail.vue'),
  },
  {
    path: '/weekly-report',
    name: 'WeeklyReport',
    component: () => import('@/views/WeeklyReport.vue'),
  },
  {
    path: '/plan',
    name: 'StudyPlan',
    component: () => import('@/views/StudyPlan.vue'),
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    component: () => import('@/views/KnowledgeBase.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录时跳转到登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('trace-accessToken')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
