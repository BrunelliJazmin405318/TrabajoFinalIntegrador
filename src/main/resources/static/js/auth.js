// /js/auth.js

// Guarda el token Basic en localStorage o sessionStorage
export function saveAuth(token, remember = false) {
  if (remember) {
    localStorage.setItem("authToken", token);
  } else {
    sessionStorage.setItem("authToken", token);
  }
}

// Obtiene el token desde storage (privado)
function getToken() {
  return localStorage.getItem("authToken") || sessionStorage.getItem("authToken");
}

// Devuelve el header Authorization si hay token
export function getAuthHeader() {
  const t = getToken();
  return t ? { "Authorization": `Basic ${t}` } : {};
}

// ¿Hay sesión?
export function isAuthenticated() {
  return !!getToken();
}

// Limpia sesión
export function clearAuth() {
  localStorage.removeItem("authToken");
  sessionStorage.removeItem("authToken");
}

// Útil por si querés leer el token crudo
export function getAuth() {
  return getToken();
}

// Si no está logueado, redirige al login
export function requireAuthOrRedirect() {
  if (!isAuthenticated()) {
    window.location.href = "/login.html";
  }
}