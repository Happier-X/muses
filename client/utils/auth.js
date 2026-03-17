export function getToken() {
  return uni.getStorageSync('token');
}

export function setToken(token) {
  uni.setStorageSync('token', token);
}

export function removeToken() {
  uni.removeStorageSync('token');
}

export function isLoggedIn() {
  return !!getToken();
}

export async function checkAuth() {
  if (!isLoggedIn()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return false;
  }
  return true;
}
