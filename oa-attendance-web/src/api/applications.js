import http from './http';

export function submitApplication(payload) {
  return http.post('/applications', payload);
}

export function listApplicationTypes() {
  return http.get('/applications/types');
}

export function listMakeupRecordOptions() {
  return http.get('/applications/makeup-options');
}

export function listMyApplications() {
  return http.get('/applications/mine');
}

export function getApplicationDetail(id) {
  return http.get(`/applications/${id}`);
}

export function listPendingTasks() {
  return http.get('/applications/tasks/pending');
}

export function listHandledTasks() {
  return http.get('/applications/tasks/handled');
}

export function approveTask(taskId, payload) {
  return http.post(`/applications/tasks/${taskId}/approve`, payload || {});
}

export function rejectTask(taskId, payload) {
  return http.post(`/applications/tasks/${taskId}/reject`, payload);
}

export function listAllApplications() {
  return http.get('/applications/all');
}

export function cancelApplication(applicationId) {
  return http.post(`/applications/${applicationId}/cancel`);
}
