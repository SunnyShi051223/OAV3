import { createRouter, createWebHistory } from 'vue-router';
import LoginView from './views/LoginView.vue';
import DashboardView from './views/DashboardView.vue';
import DocumentsView from './views/DocumentsView.vue';
import { authStore } from './store/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginView },
    { path: '/dashboard', component: DashboardView },
    { path: '/documents', component: DocumentsView }
  ]
});

router.beforeEach((to) => {
  if (to.path !== '/login' && !authStore.token) {
    return '/login';
  }
  if (to.path === '/login' && authStore.token) {
    return '/dashboard';
  }
  return true;
});

export default router;
