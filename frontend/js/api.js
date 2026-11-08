// ==========================================================
// Capa de comunicación con el backend Spring Boot
// ==========================================================

// El backend sirve también el frontend, así que la API debe apuntar al mismo
// origen desde el que se abrió la página. Así la cookie de sesión funciona
// tanto en localhost como en 127.0.0.1, otra IP o un puerto distinto.
const API_BASE =
  window.location.protocol === "file:"
    ? "http://localhost:8080/api"
    : `${window.location.origin}/api`;

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
    return {
      ok: false,
      status: 0,
      error: "No se pudo conectar con el servidor.",
    };
  }

  let data;
  try {
    data = await res.json();
  } catch {
    return {
      ok: false,
      status: res.status,
      error: res.ok
        ? "El servidor devolvió una respuesta inválida."
        : `Error ${res.status} del servidor.`,
    };
  }

  if (!res.ok) {
    return {
      ok: false,
      status: res.status,
      error:
        (typeof data?.error === "string" && data.error) ||
        `Error ${res.status} del servidor.`,
    };
  }

  if (
    data === null ||
    typeof data !== "object" ||
    typeof data.ok !== "boolean"
  ) {
    return {
      ok: false,
      status: res.status,
      error: "El servidor devolvió una respuesta inválida.",
    };
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

  /** PUT /api/me → actualiza los datos del perfil del usuario logueado */
  updateProfile(data) {
    return apiFetch("/me", { method: "PUT", body: data });
  },

  /** POST /api/logout */
  logout() {
    return apiFetch("/logout", { method: "POST" });
  },

  /** Reportes del backend */
  resumenHospitales() {
    return apiFetch("/reportes/hospitales/resumen");
  },

  hospitalesMayorCantidadPacientes() {
    return apiFetch("/reportes/hospitales/mayores-de-cien");
  },

  resumenProceso() {
    return apiFetch("/reportes/proceso");
  },

  listadoMedicos() {
    return apiFetch("/reportes/medicos");
  },

  listadoPacientes(area, codigo) {
    return apiFetch(
      `/reportes/pacientes?area=${encodeURIComponent(area)}&codigo=${encodeURIComponent(codigo)}`,
    );
  },

  pacientesNoAtendidos(area, codigo) {
    return apiFetch(
      `/reportes/pacientes/no-atendidos?area=${encodeURIComponent(area)}&codigo=${encodeURIComponent(codigo)}`,
    );
  },

  informeDuranteConsultas(area, codigo) {
    return apiFetch(
      `/reportes/consultas/informe?area=${encodeURIComponent(area)}&codigo=${encodeURIComponent(codigo)}`,
    );
  },

  consultasExitosas(area, codigo) {
    return apiFetch(
      `/reportes/consultas/exitosas?area=${encodeURIComponent(area)}&codigo=${encodeURIComponent(codigo)}`,
    );
  },

  unidadesTurnosPorRevisar() {
    return apiFetch("/reportes/unidades/turnos-por-revisar");
  },
};
