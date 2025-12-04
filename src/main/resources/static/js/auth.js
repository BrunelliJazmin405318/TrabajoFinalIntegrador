// /js/auth.js

// Guarda el token Basic en ambos storages para que funcione entre pestañas
export function saveAuth(token, remember = false) {
    if (!token) return;
    // Lo guardamos siempre en localStorage (compartido entre pestañas)
    localStorage.setItem("authToken", token);
    // Y también en sessionStorage por si en algún lado lo usan
    sessionStorage.setItem("authToken", token);
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