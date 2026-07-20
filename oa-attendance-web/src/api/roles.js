import http from './http';

export function listRoles() {
  return http.get('/roles');
}

export function createRole(payload) {
  return http.post('/roles', payload);
}

export function updateRole(payload) {
  return http.put('/roles', payload);
}

export function deleteRole(id) {
  return http.delete(`/roles/${id}`);
}

export function assignRoleMenus(payload) {
  return http.put('/roles/menus', payload);
}
