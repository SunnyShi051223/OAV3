import http from './http';

export function listDepartments() {
  return http.get('/departments');
}

export function createDepartment(payload) {
  return http.post('/departments', payload);
}

export function updateDepartment(payload) {
  return http.put('/departments', payload);
}

export function deleteDepartment(id) {
  return http.delete(`/departments/${id}`);
}
