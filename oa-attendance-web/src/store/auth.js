import { reactive } from 'vue';

const savedToken = localStorage.getItem('oa_token') || '';
const savedUser = localStorage.getItem('oa_user');

export const authStore = reactive({
  token: savedToken,
  user: savedUser ? JSON.parse(savedUser) : null,
  setSession(loginData) {
    this.token = loginData.token;
    this.user = loginData.user;
    localStorage.setItem('oa_token', loginData.token);
    localStorage.setItem('oa_user', JSON.stringify(loginData.user));
  },
  clear() {
    this.token = '';
    this.user = null;
    localStorage.removeItem('oa_token');
    localStorage.removeItem('oa_user');
  },
  hasPermission(code) {
    return Boolean(this.user && this.user.permissions && this.user.permissions.includes(code));
  }
});
