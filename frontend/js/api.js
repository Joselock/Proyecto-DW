// ==========================================================
// api.js — Capa de comunicación con el backend Spring Boot
// ==========================================================

const API_BASE = "http://localhost:8080/api";

/**
 * Wrapper de fetch que:
 *  - incluye cookies de sesión (credentials: "include")
 *  - normaliza errores en { ok: false, error }
 */
async function apiFetch(path, { method = "GET", body } = {}) {
  const options = {
    method,
    credentials: "include",
  };
  if (body !== undefined) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(API_BASE + path, options);
  } catch {
    return { ok: false, error: "No se pudo conectar con el servidor." };
  }

  let data;
  try {
    data = await res.json();
  } catch {
    return {
      ok: false,
      error: res.ok
        ? "El servidor devolvió una respuesta inválida."
        : `Error ${res.status} del servidor.`,
    };
  }

  if (!res.ok) {
    return {
      ok: false,
      error:
        (typeof data?.error === "string" && data.error) ||
        `Error ${res.status} del servidor.`,
    };
  }

  if (data === null || typeof data !== "object" || typeof data.ok !== "boolean") {
    return { ok: false, error: "El servidor devolvió una respuesta inválida." };
  }

  return data;
}

// ---------- Endpoints ----------

export const api = {
  /** POST /api/login  → { ok, user } | { ok:false, error } */
  login(email, password) {
    return apiFetch("/login", { method: "POST", body: { email, password } });
  },

  /** POST /api/register */
  register(data) {
    // data = { username, email, password, nombre, especialidad, direccion, telefono }
    return apiFetch("/register", { method: "POST", body: data });
  },

  /** POST /api/forgot-password */
  forgotPassword(email) {
    return apiFetch("/forgot-password", { method: "POST", body: { email } });
  },

  /** POST /api/reset-password */
  resetPassword(email, token, password) {
    return apiFetch("/reset-password", {
      method: "POST",
      body: { email, token, password },
    });
  },

  /** GET /api/me  → devuelve el usuario logueado (para proteger dashboard) */
  me() {
    return apiFetch("/me");
  },

  /** POST /api/logout */
  logout() {
    return apiFetch("/logout", { method: "POST" });
  },
};
