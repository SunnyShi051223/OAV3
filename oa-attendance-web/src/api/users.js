import http from './http';

export function listUsers() {
  return http.get('/users');
}

export function createUser(payload) {
  return http.post('/users', payload);
}

export function updateUser(payload) {
  return http.put('/users', payload);
}

export function deleteUser(id) {
  return http.delete(`/users/${id}`);
}

export function updateProfile(payload) {
  return http.put('/users/profile', payload);
}
