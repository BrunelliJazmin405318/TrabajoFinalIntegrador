
const AUTH_KEY = "auth.basicToken";

export function saveAuth(token, remember) {
  if (remember) {
    localStorage.setItem(AUTH_KEY, token);
  } else {
    sessionStorage.setItem(AUTH_KEY, token);
  }
}

export function clearAuth() {
  localStorage.removeItem(AUTH_KEY);
  sessionStorage.removeItem(AUTH_KEY);
}

export function getAuth() {
  return localStorage.getItem(AUTH_KEY) || sessionStorage.getItem(AUTH_KEY);
}

export function getAuthHeader() {
  const t = getAuth();
  return t ? { "Authorization": `Basic ${t}` } : {};
}

export function requireAuthOrRedirect() {
  if (!getAuth()) {
    window.location.href = "/login.html";
  }
}