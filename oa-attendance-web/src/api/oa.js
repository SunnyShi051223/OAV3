import request from './request'

export const authApi = {
  login(username, password) {
    return request.post('/auth/login', { username, password })
  },
  info: () => request.get('/auth/info'),
  logout: () => request.post('/auth/logout'),
}

function crudApi(path) {
  return {
    list: () => request.get(path),
    get: (id) => request.get(`${path}/${id}`),
    create: (data) => request.post(path, data),
    update: (data) => request.put(path, data),
    remove: (id) => request.delete(`${path}/${id}`),
  }
}

export const userApi = {
  ...crudApi('/users'),
  byDepartment: (deptId) => request.get(`/users/dept/${deptId}`),
  updateProfile: (data) => request.put('/users/profile', data),
  changePassword(userId, oldPassword, newPassword) {
    return request.put(`/users/${userId}/password`, null, { params: { oldPassword, newPassword } })
  },
  resetPassword(userId, newPassword = '123456') {
    return request.put(`/users/${userId}/reset-password`, null, { params: { newPassword } })
  },
}

export const departmentApi = {
  ...crudApi('/departments'),
  tree: () => request.get('/departments/tree'),
  children: (parentId) => request.get(`/departments/children/${parentId}`),
}

export const positionApi = {
  ...crudApi('/positions'),
  byDepartment: (deptId) => request.get(`/positions/dept/${deptId}`),
}

export const colleagueApi = {
  search: (keyword) => request.get('/colleagues/search', { params: { keyword } }),
}

export const chatApi = {
  contacts: () => request.get('/chat/contacts'),
  messages: (contactId) => request.get(`/chat/messages/${contactId}`),
  send: (receiverId, content) => request.post('/chat/send', { receiverId, content }),
}

export const applicationApi = {
  types: () => request.get('/applications/types'),
  makeupOptions: () => request.get('/applications/makeup-options'),
  mine: () => request.get('/applications/mine'),
  pending: () => request.get('/applications/tasks/pending'),
  handled: () => request.get('/applications/tasks/handled'),
  detail: (applicationId) => request.get(`/applications/${applicationId}`),
  submit: (data) => request.post('/applications', data),
  cancel: (applicationId) => request.post(`/applications/${applicationId}/cancel`),
  approve: (taskId, comment = '') => request.post(`/applications/tasks/${taskId}/approve`, { comment }),
  reject: (taskId, comment) => request.post(`/applications/tasks/${taskId}/reject`, { comment }),
}

export const attendanceApi = {
  rules: (deptId) => request.get('/attendance/rules', { params: deptId ? { deptId } : {} }),
  rule: (ruleId) => request.get(`/attendance/rules/${ruleId}`),
  createRule: (data) => request.post('/attendance/rules', data),
  updateRule: (data) => request.put('/attendance/rules', data),
  removeRule: (ruleId) => request.delete(`/attendance/rules/${ruleId}`),
  stats: (params) => request.get('/attendance/stats', { params }),
  availableRules: () => request.get('/attendance/available-rules'),
  todayRecords: () => request.get('/attendance/today-records'),
  month: (month) => request.get('/attendance/my-month', { params: { month } }),
  checkIn: (data) => request.post('/attendance/check-in', data),
  checkOut: (data) => request.post('/attendance/check-out', data),
}

export const documentApi = {
  list: () => request.get('/documents'),
  get: (docId) => request.get(`/documents/${docId}`),
  search: (keyword) => request.get('/documents/search', { params: { keyword } }),
  create: (data) => request.post('/documents', data),
  update: (data) => request.put('/documents', data),
  remove: (docId) => request.delete(`/documents/${docId}`),
  rebuildIndex: () => request.post('/documents/index/rebuild'),
}

export const roleApi = {
  list: () => request.get('/roles'),
  get: (roleId) => request.get(`/roles/${roleId}`),
  create: (data) => request.post('/roles', data),
  update: (data) => request.put('/roles', data),
  remove: (roleId) => request.delete(`/roles/${roleId}`),
  assignMenus: (roleId, menuIds) => request.put('/roles/menus', { roleId, menuIds }),
  assignPermissions: (roleId, permissionIds) => request.put('/roles/permissions', { roleId, permissionIds }),
}

export const menuApi = {
  list: () => request.get('/menus'),
  tree: () => request.get('/menus/tree'),
  currentTree: () => request.get('/menus/current/tree'),
  get: (menuId) => request.get(`/menus/${menuId}`),
  byRole: (roleId) => request.get(`/menus/role/${roleId}`),
  create: (data) => request.post('/menus', data),
  update: (data) => request.put('/menus', data),
  remove: (menuId) => request.delete(`/menus/${menuId}`),
}
