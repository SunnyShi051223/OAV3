import http from './http';

export function searchColleagues(keyword) {
  return http.get('/colleagues/search', { params: { keyword } });
}
