import { createRouter, createWebHistory } from 'vue-router';
import LoginView from './views/LoginView.vue';
import DashboardView from './views/DashboardView.vue';
import DocumentsView from './views/DocumentsView.vue';
import UserManageView from './views/UserManageView.vue';
import DepartmentManageView from './views/DepartmentManageView.vue';
import PositionManageView from './views/PositionManageView.vue';
import RoleManageView from './views/RoleManageView.vue';
import MenuManageView from './views/MenuManageView.vue';
import AttendanceRuleView from './views/AttendanceRuleView.vue';
import MyAttendanceView from './views/MyAttendanceView.vue';
import AttendanceStatsView from './views/AttendanceStatsView.vue';
import MyApplicationsView from './views/MyApplicationsView.vue';
import ApprovalTasksView from './views/ApprovalTasksView.vue';
import ColleaguesView from './views/ColleaguesView.vue';
import ChatView from './views/ChatView.vue';
import AllApplicationsView from './views/AllApplicationsView.vue';
import { authStore } from './store/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginView },
    { path: '/dashboard', component: DashboardView },
    { path: '/documents', component: DocumentsView },
    { path: '/colleagues', component: ColleaguesView },
    { path: '/chat', component: ChatView },
    { path: '/system', redirect: '/system/user' },
    { path: '/system/user', component: UserManageView },
    { path: '/system/dept', component: DepartmentManageView },
    { path: '/system/position', component: PositionManageView },
    { path: '/system/role', component: RoleManageView },
    { path: '/system/menu', component: MenuManageView },
    { path: '/attendance', redirect: () => (authStore.hasPermission('attendance:rule:query') ? '/attendance/rules' : '/attendance/my') },
    { path: '/attendance/rules', component: AttendanceRuleView },
    { path: '/attendance/my', component: MyAttendanceView },
    { path: '/attendance/stats', component: AttendanceStatsView },
    { path: '/approvals', redirect: '/approvals/my' },
    { path: '/approvals/my', component: MyApplicationsView },
    { path: '/approvals/tasks', component: ApprovalTasksView },
    { path: '/approvals/all', component: AllApplicationsView },
    { path: '/organization', redirect: '/organization/users' },
    { path: '/organization/users', component: UserManageView },
    { path: '/organization/departments', component: DepartmentManageView },
    { path: '/organization/positions', component: PositionManageView },
    { path: '/organization/roles', component: RoleManageView },
    { path: '/organization/menus', component: MenuManageView },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
});

router.beforeEach((to) => {
  if (to.path !== '/login' && !authStore.token) {
    return '/login';
  }
  if (to.path === '/login' && authStore.token) {
    return '/dashboard';
  }
  if (to.path === '/approvals/tasks' && !authStore.hasPermission('approval:handle')) {
    return '/approvals/my';
  }
  if (to.path === '/approvals/all' && !authStore.hasPermission('approval:all')) {
    return '/approvals/my';
  }
  return true;
});

export default router;
