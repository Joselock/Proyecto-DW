// ==========================================================
// Capa de comunicación con el backend Spring Boot
// ==========================================================

/* Spring Boot sirve el frontend y la API en el mismo puerto (8080). Pero si el
HTML se abre con otro servidor (Live Server, http-server, etc.) el POST a
/api/login llega a ese servidor estático y devuelve 405 Method Not Allowed.
Por eso probamos primero el backend en el puerto 8080 del mismo host y luego
el mismo origen. Las cookies no distinguen puertos, así que la sesión se
mantiene aunque el frontend venga de otro puerto.*/

const BACKEND_PORT = "8080";

function apiBaseCandidates() {
  const { protocol, hostname, origin, port } = window.location;
  const bases = [];

  if (protocol === "file:") {
    bases.push(`http://localhost:${BACKEND_PORT}/api`);
  } else {
    // Si el propio Spring sirve la página, el mismo origen es el backend.
    if (port === BACKEND_PORT) bases.push(`${origin}/api`);

    // Si la página viene de otro servidor, el backend sigue en el puerto 8080.
    bases.push(`${protocol}//${hostname}:${BACKEND_PORT}/api`);

    // Último recurso: mismo origen (útil si se configuró un proxy /api).
    bases.push(`${origin}/api`);
  }

  return [...new Set(bases)];
}

// Una vez que encontramos el backend, dejamos de probar candidatos.
let resolvedApiBase = null;

/**
 * Wrapper de fetch que:
 *  - incluye cookies de sesión (credentials: "include")
 *  - detecta automáticamente la URL real del backend
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

  const candidates = resolvedApiBase ? [resolvedApiBase] : apiBaseCandidates();
  const attempts = [];

  for (const base of candidates) {
    let res;
    try {
      res = await fetch(base + path, options);
    } catch {
      attempts.push({
        base,
        error: {
          ok: false,
          status: 0,
          error: `No se pudo conectar con el backend en ${base}. Verificá que el servidor esté corriendo.`,
        },
      });
      continue;
    }

    /* El backend siempre responde JSON con un campo booleano "ok". Si la
    respuesta no tiene esa forma, este origen no es la API.*/
    const contentType = res.headers.get("content-type") || "";
    let data = null;
    if (contentType.includes("json")) {
      try {
        data = await res.json();
      } catch {
        data = null;
      }
    }

    if (data && typeof data === "object" && typeof data.ok === "boolean") {
      if (!res.ok && !data.error) {
        data.error = `Error ${res.status} del servidor.`;
      }
      resolvedApiBase = base;
      return data;
    }

    attempts.push({
      base,
      error: {
        ok: false,
        status: res.status,
        error:
          res.status === 404 || res.status === 405
            ? `La API no está disponible en ${base} (HTTP ${res.status}).`
            : res.ok
              ? "El servidor devolvió una respuesta inválida."
              : `Error ${res.status} del servidor.`,
      },
    });
  }

  /* Si ningún candidato respondió como backend, mostramos el error del
  candidato que apunta al puerto del backend, que es el más informativo.*/
  const backendAttempt =
    attempts.find((attempt) => attempt.base.includes(`:${BACKEND_PORT}/api`)) ||
    attempts[attempts.length - 1];

  return (
    backendAttempt?.error || {
      ok: false,
      status: 0,
      error: "No se pudo conectar con el servidor.",
    }
  );
}

/* ---------- Endpoints -------------------------------
-------------------------------------------------------
-------------------------------------------------------*/

export const api = {
  /** POST /api/login  */
  login(email, password) {
    return apiFetch("/login", { method: "POST", body: { email, password } });
  },

  /** POST /api/register */
  register(data) {
    // data = { username, email, password, codMed }
    // codMed se omite cuando el registro crea al primer administrador.
    return apiFetch("/register", { method: "POST", body: data });
  },

  /** GET /api/registro/opciones → hospitales, unidades y médicos sin cuenta */
  registroOpciones() {
    return apiFetch("/registro/opciones");
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

  /** Pacientes visibles según el rol de la sesión (ADMIN = todos, MEDICO = los suyos) */
  pacientesPanel(q = "", limit = 5000, offset = 0) {
    const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
    if (q) params.set("q", q);
    return apiFetch(`/panel/pacientes?${params}`);
  },

  /** Turnos visibles según el rol de la sesión (ADMIN = todos, MEDICO = los suyos) */
  turnosPanel(q = "", limit = 5000, offset = 0) {
    const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
    if (q) params.set("q", q);
    return apiFetch(`/panel/turnos?${params}`);
  },

  /** Catálogos y perfil para los formularios de altas (filtrados por rol) */
  catalogosAltas() {
    return apiFetch("/altas/catalogos");
  },

  /** Alta real de paciente (Registro + Paciente) */
  crearPaciente(data) {
    return apiFetch("/altas/pacientes", { method: "POST", body: data });
  },

  /** Alta real de turno (Turno + Consulta) */
  crearTurno(data) {
    return apiFetch("/altas/turnos", { method: "POST", body: data });
  },

  /** Alta real de médico. El backend solo la permite para ADMIN. */
  crearMedico(data) {
    return apiFetch("/altas/medicos", { method: "POST", body: data });
  },

  /** Contexto hospitalario del usuario (hospital, jerarquía y alcance) */
  contextoReportes() {
    return apiFetch("/reportes/contexto");
  },

  /** Médicos del hospital vinculado al usuario (o todos si no tiene hospital) */
  profesionalesHospital() {
    return apiFetch("/reportes/profesionales");
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
