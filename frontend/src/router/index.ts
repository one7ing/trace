import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/knowledge',
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/KnowledgeChat.vue'),
  },
  {
    path: '/interview',
    name: 'InterviewHome',
    component: () => import('@/views/interview/InterviewHome.vue'),
  },
  {
    path: '/interview/start',
    name: 'InterviewSession',
    component: () => import('@/views/interview/InterviewSession.vue'),
  },
  {
    path: '/interview/history',
    name: 'InterviewHistory',
    component: () => import('@/views/interview/InterviewHistory.vue'),
  },
  {
    path: '/interview/history/:id',
    name: 'InterviewDetail',
    component: () => import('@/views/interview/InterviewDetail.vue'),
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
  const token = localStorage.getItem('trace-token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
