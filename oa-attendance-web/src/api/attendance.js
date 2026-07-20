import http from './http';

export function listAttendanceRules(deptId) {
  return http.get('/attendance/rules', { params: { deptId } });
}

export function getAttendanceRule(ruleId) {
  return http.get(`/attendance/rules/${ruleId}`);
}

export function createAttendanceRule(payload) {
  return http.post('/attendance/rules', payload);
}

export function updateAttendanceRule(payload) {
  return http.put('/attendance/rules', payload);
}

export function deleteAttendanceRule(ruleId) {
  return http.delete(`/attendance/rules/${ruleId}`);
}

export function getTodayAttendance() {
  return http.get('/attendance/today');
}

export function getCurrentAttendanceRule() {
  return http.get('/attendance/current-rule');
}

export function getAvailableAttendanceRules() {
  return http.get('/attendance/available-rules');
}

export function getTodayAttendanceRecords() {
  return http.get('/attendance/today-records');
}

export function checkIn(payload) {
  return http.post('/attendance/check-in', payload);
}

export function checkOut(payload) {
  return http.post('/attendance/check-out', payload);
}

export function getMyAttendanceMonth(month) {
  return http.get('/attendance/my-month', { params: { month } });
}

export function getAttendanceStats(params) {
  return http.get('/attendance/stats', { params });
}
