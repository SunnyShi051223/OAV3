import http from './http';

export function getContacts() {
  return http.get('/chat/contacts');
}

export function getMessages(contactId) {
  return http.get(`/chat/messages/${contactId}`);
}

export function sendMessage(payload) {
  return http.post('/chat/send', payload);
}
