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
