import http from './http';

export function listDocuments() {
  return http.get('/documents');
}

export function createDocument(payload) {
  return http.post('/documents', payload);
}

export function searchDocuments(keyword) {
  return http.get('/documents/search', { params: { keyword } });
}

export function rebuildDocumentIndex() {
  return http.post('/documents/index/rebuild');
}
