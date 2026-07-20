import http from './http';

export function getCurrentMenuTree() {
  return http.get('/menus/current/tree');
}

export function listMenus() {
  return http.get('/menus');
}

export function createMenu(payload) {
  return http.post('/menus', payload);
}

export function updateMenu(payload) {
  return http.put('/menus', payload);
}

export function deleteMenu(id) {
  return http.delete(`/menus/${id}`);
}
