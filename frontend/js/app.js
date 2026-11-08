import { api } from "./api.js";

/* ============================================================
   Estado del dashboard
   ============================================================ */
const state = {
  user: null,
  role: "MEDICO",
  section: "Resumen",
  demoMode: false,
  resumenPatients: [],
  resumenAppointments: [],
  viewPatients: [],
  viewAppointments: [],
  queue: [],
  turnoFilters: { search: "", specialty: "all", status: "all" },
  pacienteFilters: { search: "", status: "all" },
};

const ADMIN_ONLY = new Set(["Profesionales", "Reportes"]);

const GENERIC_SECTIONS = {
  Profesionales: {
    eyebrow: "Equipo médico",
    title: "Profesionales",
    subhead: "Administra especialidades, disponibilidad y asignación del equipo.",
    icon: "stethoscope",
    badge: "Solo administradores",
    badgeClass: "confirmed",
    heading: "Gestión del equipo profesional",
    text: "Este módulo permitirá administrar médicos, especialidades y horarios de atención. Está reservado para los administradores del sistema.",
  },
  Reportes: {
    eyebrow: "Analítica",
    title: "Reportes",
    subhead: "Consulta los indicadores y reportes de gestión del hospital.",
    icon: "bar-chart-3",
    badge: "Solo administradores",
    badgeClass: "confirmed",
    heading: "Reportes y analítica",
    text: "Aquí se concentrarán los reportes de pacientes, médicos, turnos y consultas. Este módulo está reservado para los administradores del sistema.",
  },
  Configuración: {
    eyebrow: "Administración",
    title: "Configuración",
    subhead: "Ajusta las preferencias generales del sistema.",
    icon: "settings-2",
    badge: "En preparación",
    badgeClass: "pending",
    heading: "Configuración del sistema",
    text: "Muy pronto podrás ajustar notificaciones, permisos y preferencias generales del hospital desde este panel.",
  },
  Ayuda: {
    eyebrow: "Soporte",
    title: "Centro de ayuda",
    subhead: "Encuentra respuestas y acompaña tu trabajo diario.",
    icon: "circle-help",
    badge: "Soporte",
    badgeClass: "pending",
    heading: "Estamos para acompañarte",
    text: "Aquí encontrarás guías rápidas, preguntas frecuentes y canales de contacto para resolver cualquier duda sobre el sistema.",
  },
};

const AVATAR_HEX = {
  peach: "#f3c3a8",
  lavender: "#cfc8ec",
  mint: "#a9ded3",
  sky: "#b8d6ed",
  sand: "#edd4a7",
};
const AVATAR_ORDER = ["peach", "lavender", "mint", "sky", "sand"];
const SPECIALTY_COLORS = [
  "#1ea99a",
  "#4c84c4",
  "#d99a35",
  "#ee806f",
  "#7c8fbf",
  "#2f9c71",
];

const DEMO_PATIENTS = [
  {
    id: "HC-1042",
    name: "Valentina Cruz",
    age: 34,
    phone: "+58 412 555 0142",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    lastVisit: "Hoy · 09:30",
    status: "active",
    isNew: false,
  },
  {
    id: "HC-1043",
    name: "Mateo Rivas",
    age: 8,
    phone: "+58 414 555 0198",
    specialty: "Pediatría",
    doctor: "Dra. Valentina Cruz",
    lastVisit: "Hoy · 09:00",
    status: "active",
    isNew: true,
  },
  {
    id: "HC-1044",
    name: "Camila Ferrer",
    age: 27,
    phone: "+58 424 555 0131",
    specialty: "Dermatología",
    doctor: "Dr. Pablo Méndez",
    lastVisit: "Hoy · 08:45",
    status: "active",
    isNew: false,
  },
  {
    id: "HC-1045",
    name: "Lucas Navarro",
    age: 45,
    phone: "+58 416 555 0177",
    specialty: "Traumatología",
    doctor: "Dra. Lucía Herrera",
    lastVisit: "Ayer · 16:20",
    status: "pending",
    isNew: false,
  },
  {
    id: "HC-1046",
    name: "Isabella Romero",
    age: 52,
    phone: "+58 412 555 0164",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    lastVisit: "12 oct · 11:10",
    status: "active",
    isNew: false,
  },
  {
    id: "HC-1047",
    name: "Diego Salas",
    age: 39,
    phone: "+58 426 555 0182",
    specialty: "Medicina general",
    doctor: "Dr. Pablo Méndez",
    lastVisit: "12 oct · 10:30",
    status: "inactive",
    isNew: false,
  },
  {
    id: "HC-1048",
    name: "Emma Duarte",
    age: 6,
    phone: "+58 414 555 0120",
    specialty: "Pediatría",
    doctor: "Dra. Valentina Cruz",
    lastVisit: "11 oct · 15:40",
    status: "pending",
    isNew: true,
  },
  {
    id: "HC-1049",
    name: "Martín Aguirre",
    age: 61,
    phone: "+58 412 555 0111",
    specialty: "Dermatología",
    doctor: "Dr. Pablo Méndez",
    lastVisit: "10 oct · 09:15",
    status: "active",
    isNew: false,
  },
  {
    id: "HC-1050",
    name: "Sara Paredes",
    age: 31,
    phone: "+58 424 555 0105",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    lastVisit: "09 oct · 14:00",
    status: "pending",
    isNew: true,
  },
  {
    id: "HC-1051",
    name: "Tomás Leal",
    age: 22,
    phone: "+58 416 555 0102",
    specialty: "Medicina general",
    doctor: "Dr. Pablo Méndez",
    lastVisit: "08 oct · 17:25",
    status: "inactive",
    isNew: false,
  },
];

const DEMO_APPOINTMENTS = [
  {
    id: "T-2401",
    time: "08:30",
    patient: "Valentina Cruz",
    patientId: "HC-1042",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    status: "confirmed",
    room: "Consultorio 3",
  },
  {
    id: "T-2402",
    time: "09:00",
    patient: "Mateo Rivas",
    patientId: "HC-1043",
    specialty: "Pediatría",
    doctor: "Dra. Valentina Cruz",
    status: "done",
    room: "Consultorio 1",
  },
  {
    id: "T-2403",
    time: "09:30",
    patient: "Camila Ferrer",
    patientId: "HC-1044",
    specialty: "Dermatología",
    doctor: "Dr. Pablo Méndez",
    status: "confirmed",
    room: "Consultorio 2",
  },
  {
    id: "T-2404",
    time: "10:00",
    patient: "Lucas Navarro",
    patientId: "HC-1045",
    specialty: "Traumatología",
    doctor: "Dra. Lucía Herrera",
    status: "pending",
    room: "Consultorio 4",
  },
  {
    id: "T-2405",
    time: "10:30",
    patient: "Isabella Romero",
    patientId: "HC-1046",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    status: "confirmed",
    room: "Consultorio 3",
  },
  {
    id: "T-2406",
    time: "11:00",
    patient: "Diego Salas",
    patientId: "HC-1047",
    specialty: "Medicina general",
    doctor: "Dr. Pablo Méndez",
    status: "done",
    room: "Consultorio 2",
  },
  {
    id: "T-2407",
    time: "11:30",
    patient: "Emma Duarte",
    patientId: "HC-1048",
    specialty: "Pediatría",
    doctor: "Dra. Valentina Cruz",
    status: "pending",
    room: "Consultorio 1",
  },
  {
    id: "T-2408",
    time: "12:00",
    patient: "Martín Aguirre",
    patientId: "HC-1049",
    specialty: "Dermatología",
    doctor: "Dr. Pablo Méndez",
    status: "confirmed",
    room: "Consultorio 2",
  },
  {
    id: "T-2409",
    time: "12:30",
    patient: "Sara Paredes",
    patientId: "HC-1050",
    specialty: "Cardiología",
    doctor: "Dra. Sofía Núñez",
    status: "pending",
    room: "Consultorio 3",
  },
];

const DEMO_ACTIVITIES = [
  {
    title: "Nuevo paciente registrado",
    detail: "Tomás Leal · Medicina general",
    time: "Hace 12 min",
    tone: "teal",
  },
  {
    title: "Turno confirmado",
    detail: "Valentina Cruz · Cardiología",
    time: "Hace 28 min",
    tone: "blue",
  },
  {
    title: "Ficha actualizada",
    detail: "Camila Ferrer · Dermatología",
    time: "Hace 1 h",
    tone: "amber",
  },
  {
    title: "Paciente atendido",
    detail: "Mateo Rivas · Pediatría",
    time: "Hace 2 h",
    tone: "coral",
  },
];

/* ============================================================
   Utilidades
   ============================================================ */
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) =>
  Array.from(root.querySelectorAll(selector));

const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (char) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[char],
  );

const slug = (value) =>
  String(value ?? "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

const initialsOf = (name) =>
  String(name || "P")
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0] || "")
    .join("")
    .toUpperCase() || "P";

const avatarClass = (index) => AVATAR_ORDER[index % AVATAR_ORDER.length];

const avatarStyle = (index) =>
  `background:${AVATAR_HEX[avatarClass(index)]};color:#536677`;

const formatNumber = (value) => Number(value || 0).toLocaleString("es-ES");

const statusLabel = (status) =>
  ({
    confirmed: "Confirmado",
    pending: "Pendiente",
    done: "Atendido",
    active: "Activo",
    inactive: "Inactivo",
  })[status] || "Pendiente";

const refreshIcons = () => window.lucide?.createIcons();

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) element.textContent = value;
}

function toast(message) {
  const element = $("#toast");
  if (!element) return;
  element.querySelector("span").textContent = message;
  element.classList.add("show");
  clearTimeout(element._timer);
  element._timer = setTimeout(() => element.classList.remove("show"), 3000);
}

/* ============================================================
   Sesión y roles
   ============================================================ */
function isAdmin() {
  return state.role === "ADMIN";
}

function applyRoleVisibility() {
  document.body.dataset.role = isAdmin() ? "admin" : "medico";
  $$("[data-admin-only]").forEach((element) => {
    element.hidden = !isAdmin();
  });
}

function updateUserChrome() {
  const name = state.user?.nombre || state.user?.username || "Usuario";
  const roleLabel = isAdmin() ? "Administrador" : "Médico";
  const initials = initialsOf(name);
  const firstName = name.trim().split(/\s+/)[0];

  setText("sidebarUserName", name);
  setText("sidebarUserRole", roleLabel);
  setText("sidebarAvatar", initials);
  setText("topAvatar", initials);
  setText("accountMenuName", name);
  setText("accountMenuEmail", state.user?.email || "");

  const title = $("#welcomeTitle");
  if (title) {
    title.innerHTML = `${greeting()}, ${escapeHtml(firstName)} <span class="wave">✦</span>`;
  }

  const subhead = $("#welcomeSubhead");
  if (subhead) {
    subhead.textContent = isAdmin()
      ? "Aquí tienes lo que está pasando hoy en tu hospital."
      : "Aquí tienes el resumen de tu actividad y tus pacientes de hoy.";
  }
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Buenos días";
  if (hour < 20) return "Buenas tardes";
  return "Buenas noches";
}

async function initSession() {
  const response = await api.me();

  if (response.ok && response.user) {
    state.user = response.user;
    state.role = String(response.user.role || "MEDICO").toUpperCase();
    updateUserChrome();
    applyRoleVisibility();
    return true;
  }

  if (response.status === 401) {
    window.location.replace("./index.html");
    return false;
  }

  // Si el backend no está disponible, se mantiene visible la interfaz de
  // administrador para poder revisar el diseño sin bloquear el dashboard.
  console.warn("No se pudo validar la sesión:", response.error);
  state.role = "ADMIN";
  applyRoleVisibility();
  return true;
}

/* ============================================================
   Navegación entre secciones
   ============================================================ */
function navigate(section, updateHash = true) {
  const requested = section || "Resumen";

  if (ADMIN_ONLY.has(requested) && !isAdmin()) {
    toast("Este módulo es exclusivo para administradores");
    return;
  }

  let target = document.getElementById(`view-${requested}`);

  if (!target) {
    target = document.getElementById("view-generic");
    fillGenericView(requested);
  }

  if (!target) return;

  $$(".view").forEach((view) => {
    view.hidden = view !== target;
  });

  // Reinicia la animación de entrada al mostrar la vista. Sin esto, la
  // animación se ejecuta mientras la vista está oculta y queda congelada.
  target.style.animation = "none";
  void target.offsetHeight;
  target.style.animation = "";

  state.section = requested;

  $$(".nav-item").forEach((button) => {
    const active = button.dataset.section === requested;
    button.classList.toggle("active", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });

  const breadcrumb = $(".breadcrumb strong");
  if (breadcrumb) breadcrumb.textContent = requested;

  document.title = `${requested} · Clara Health OS`;

  if (window.innerWidth <= 700) {
    $("#sidebar")?.classList.remove("open");
  }

  if (updateHash) {
    const nextHash = `#${encodeURIComponent(requested)}`;
    if (window.location.hash !== nextHash) {
      history.replaceState(null, "", nextHash);
    }
  }
}

function fillGenericView(section) {
  const config = GENERIC_SECTIONS[section] || {
    eyebrow: "Módulo",
    title: section,
    subhead: "Contenido disponible próximamente.",
    icon: "sparkles",
    badge: "En preparación",
    badgeClass: "pending",
    heading: "Estamos trabajando en este módulo",
    text: "Muy pronto podrás gestionar esta sección desde el mismo panel.",
  };

  setText("genericEyebrow", config.eyebrow);
  setText("genericTitle", config.title);
  setText("genericSubhead", config.subhead);
  setText("genericHeading", config.heading);
  setText("genericText", config.text);

  const badge = $("#genericBadge");
  if (badge) {
    badge.textContent = config.badge;
    badge.className = `status-chip ${config.badgeClass || "pending"}`;
  }

  const icon = $("#genericIcon");
  if (icon) {
    icon.innerHTML = `<i data-lucide="${config.icon}"></i>`;
    refreshIcons();
  }
}

/* ============================================================
   Fechas
   ============================================================ */
function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function obtenerFecha(fecha = new Date()) {
  return capitalizar(
    new Intl.DateTimeFormat("es-ES", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    }).format(fecha),
  );
}

function obtenerFechaCorta(fecha = new Date()) {
  return capitalizar(
    new Intl.DateTimeFormat("es-ES", {
      weekday: "short",
      day: "numeric",
      month: "short",
    }).format(fecha),
  ).replace(".", "");
}

function renderFecha() {
  const hoy = new Date();

  const fechaLarga = document.getElementById("fechahoy");
  if (fechaLarga) {
    fechaLarga.textContent = obtenerFecha(hoy);
    fechaLarga.setAttribute("datetime", hoy.toISOString().slice(0, 10));
  }

  const fechaCorta = obtenerFechaCorta(hoy);
  ["agendaDate", "turnosAgendaDate"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.textContent = fechaCorta;
  });

  const dateInput = $('input[name="date"]');
  if (dateInput) dateInput.value = hoy.toISOString().slice(0, 10);
}

/* ============================================================
   Carga de datos
   ============================================================ */
function calculateAge(dateValue) {
  if (!dateValue) return "—";
  const birth = new Date(dateValue);
  if (Number.isNaN(birth.getTime())) return "—";
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age >= 0 ? age : "—";
}

function extractBackendPatients(hospitals) {
  const rows = [];

  for (const hospital of hospitals) {
    if (!Array.isArray(hospital?.departamentos)) continue;

    for (const department of hospital.departamentos) {
      if (!Array.isArray(department?.unidades)) continue;

      for (const unit of department.unidades) {
        const unitPatients =
          unit?.registro?.pacientes || unit?.pacientes || [];

        if (!Array.isArray(unitPatients)) continue;

        for (const patient of unitPatients) {
          rows.push({
            id: patient.numHisCli || `HC-${rows.length + 1}`,
            name: patient.nombrePac || "Paciente",
            age: calculateAge(patient.fechaN),
            phone: patient.telefono || "Sin teléfono",
            specialty: department.nombreDep || "Medicina general",
            doctor: unit.nombreUni || "Equipo médico",
            lastVisit: "Reciente",
            status: "active",
            isNew: false,
          });
        }
      }
    }
  }

  return rows;
}

function buildAppointmentsFromPatients(patients) {
  const times = [
    "08:30",
    "09:00",
    "09:30",
    "10:00",
    "10:30",
    "11:00",
    "11:30",
    "12:00",
    "12:30",
    "13:00",
  ];
  const statuses = ["confirmed", "done", "pending", "confirmed", "pending"];

  return patients.slice(0, 10).map((patient, index) => ({
    id: `T-${2401 + index}`,
    time: times[index] || "08:30",
    patient: patient.name,
    patientId: patient.id,
    specialty: patient.specialty,
    doctor: patient.doctor,
    status: statuses[index % statuses.length],
    room: `Consultorio ${(index % 4) + 1}`,
  }));
}

function updateResumenStats(hospitals) {
  const selected = hospitals.slice(0, 4);
  const total = selected.reduce(
    (sum, hospital) => sum + (hospital.cantPac || 0),
    0,
  );

  const cards = $$("#view-Resumen .stat-card");
  const values = [total, total, null, total];

  cards.forEach((card, index) => {
    if (values[index] === null) return;
    const value = card.querySelector(".stat-value");
    if (value) value.textContent = formatNumber(values[index]);
  });
}

async function loadDashboardData() {
  let backendPatients = [];

  try {
    const response = await api.resumenHospitales();

    if (response.ok && Array.isArray(response.data)) {
      backendPatients = extractBackendPatients(response.data);
      updateResumenStats(response.data);
    } else {
      console.warn("No se pudo cargar el resumen:", response.error);
    }
  } catch (error) {
    console.warn(
      "No se pudo cargar la data del backend, se usa la vista de ejemplo:",
      error,
    );
  }

  state.resumenPatients = backendPatients;
  state.resumenAppointments = backendPatients.length
    ? buildAppointmentsFromPatients(backendPatients).slice(0, 6)
    : [];

  if (backendPatients.length) {
    state.viewPatients = backendPatients;
    state.viewAppointments = buildAppointmentsFromPatients(backendPatients);
    state.demoMode = false;
  } else {
    state.viewPatients = DEMO_PATIENTS;
    state.viewAppointments = DEMO_APPOINTMENTS;
    state.demoMode = true;
  }

  updateDemoPills();
  renderTimeline();
  renderQueue();
  renderPatients();
  renderTurnosView();
  renderPacientesView();
}

function updateDemoPills() {
  ["turnosDemoPill", "pacientesDemoPill"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.hidden = !state.demoMode;
  });
}

/* ============================================================
   Resumen
   ============================================================ */
function renderTimeline(filter = "all") {
  const root = $("#timeline");
  if (!root) return;

  const items = state.resumenAppointments.filter(
    (appointment) =>
      filter === "all" || slug(appointment.specialty) === slug(filter),
  );

  if (!items.length) {
    root.innerHTML =
      '<div class="empty-state">No hay turnos para esta especialidad.</div>';
    return;
  }

  root.innerHTML = items
    .map(
      (appointment) => `
        <div class="time-row">
          <div class="time-label">${escapeHtml(appointment.time)}</div>
          <div class="appointment-item ${appointment.status === "done" ? "past" : ""}">
            <div class="appointment-main">
              <strong>${escapeHtml(appointment.patient)}</strong>
              <span>${escapeHtml(appointment.specialty)} · ${escapeHtml(appointment.patientId)}</span>
            </div>
            <div class="appointment-meta">
              <span class="doctor-mini">${escapeHtml(appointment.doctor)}</span>
              <span class="status-chip ${appointment.status}">${statusLabel(appointment.status)}</span>
            </div>
          </div>
        </div>`,
    )
    .join("");
}

function renderQueue() {
  const root = $("#queueList");
  if (!root) return;

  if (!state.queue.length) {
    root.innerHTML =
      '<div class="empty-state">No hay pacientes en espera en este momento.</div>';
    return;
  }

  root.innerHTML = state.queue
    .map(
      (item, index) => `
        <div class="queue-item">
          <span class="queue-num">0${index + 1}</span>
          <span class="patient-avatar" style="${avatarStyle(index)}">${escapeHtml(item.initials)}</span>
          <div class="patient-info">
            <strong>${escapeHtml(item.name)}</strong>
            <span>${escapeHtml(item.detail)}</span>
          </div>
          <span class="${item.time === "En consulta" ? "consulting" : "wait-time"}">${escapeHtml(item.time)}</span>
        </div>`,
    )
    .join("");
}

function renderPatients(query = "") {
  const root = $("#patientsTable");
  if (!root) return;

  const filtered = state.resumenPatients.filter((patient) =>
    `${patient.name} ${patient.id} ${patient.specialty}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );

  if (!filtered.length) {
    root.innerHTML =
      '<tr><td colspan="6"><div class="empty-state">No encontramos pacientes con esa búsqueda.</div></td></tr>';
    refreshIcons();
    return;
  }

  root.innerHTML = filtered
    .map(
      (patient, index) => `
        <tr>
          <td>
            <div class="table-patient">
              <span class="patient-avatar" style="${avatarStyle(index)}">${escapeHtml(initialsOf(patient.name))}</span>
              <div>
                <strong>${escapeHtml(patient.name)}</strong>
                <span>${escapeHtml(patient.id)}</span>
              </div>
            </div>
          </td>
          <td>${escapeHtml(patient.lastVisit)}</td>
          <td>${escapeHtml(patient.specialty)}</td>
          <td>${escapeHtml(patient.doctor)}</td>
          <td><span class="status-chip ${patient.status === "pending" ? "pending" : "confirmed"}">${patient.status === "pending" ? "Pendiente" : "Activo"}</span></td>
          <td>
            <div class="table-actions">
              <button class="icon-btn" data-patient-id="${escapeHtml(patient.id)}" title="Ver ficha de ${escapeHtml(patient.name)}">
                <i data-lucide="more-horizontal"></i>
              </button>
            </div>
          </td>
        </tr>`,
    )
    .join("");

  refreshIcons();
}

/* ============================================================
   Turnos
   ============================================================ */
function filterAppointments(includeStatus = false) {
  const search = state.turnoFilters.search.trim().toLowerCase();
  const specialty = state.turnoFilters.specialty;
  const status = state.turnoFilters.status;

  return state.viewAppointments.filter((appointment) => {
    const haystack =
      `${appointment.patient} ${appointment.patientId} ${appointment.doctor} ${appointment.specialty}`.toLowerCase();
    const matchesSearch = !search || haystack.includes(search);
    const matchesSpecialty =
      specialty === "all" || appointment.specialty === specialty;
    const matchesStatus =
      !includeStatus || status === "all" || appointment.status === status;
    return matchesSearch && matchesSpecialty && matchesStatus;
  });
}

function populateTurnoSpecialties() {
  const select = $("#turnoSpecialtyFilter");
  if (!select) return;

  const current = state.turnoFilters.specialty;
  const specialties = [
    ...new Set(state.viewAppointments.map((item) => item.specialty)),
  ].sort((a, b) => a.localeCompare(b, "es"));

  select.innerHTML = [
    '<option value="all">Todas las especialidades</option>',
    ...specialties.map(
      (specialty) =>
        `<option value="${escapeHtml(specialty)}">${escapeHtml(specialty)}</option>`,
    ),
  ].join("");

  select.value = specialties.includes(current) ? current : "all";
  state.turnoFilters.specialty = select.value;
}

function renderTurnoTimeline() {
  const root = $("#turnoTimeline");
  if (!root) return;

  const items = filterAppointments(false);

  if (!items.length) {
    root.innerHTML =
      '<div class="empty-state">No hay turnos que coincidan con los filtros.</div>';
    return;
  }

  root.innerHTML = items
    .map(
      (appointment) => `
        <div class="time-row">
          <div class="time-label">${escapeHtml(appointment.time)}</div>
          <div class="appointment-item ${appointment.status === "done" ? "past" : ""}">
            <div class="appointment-main">
              <strong>${escapeHtml(appointment.patient)}</strong>
              <span>${escapeHtml(appointment.specialty)} · ${escapeHtml(appointment.room)}</span>
            </div>
            <div class="appointment-meta">
              <span class="doctor-mini">${escapeHtml(appointment.doctor)}</span>
              <span class="status-chip ${appointment.status}">${statusLabel(appointment.status)}</span>
            </div>
          </div>
        </div>`,
    )
    .join("");
}

function renderTurnosTable() {
  const root = $("#turnosTable");
  if (!root) return;

  const items = filterAppointments(true);

  setText(
    "turnosCount",
    `${items.length} de ${state.viewAppointments.length} turnos`,
  );

  if (!items.length) {
    root.innerHTML =
      '<tr><td colspan="7"><div class="empty-state">No encontramos turnos con esos filtros.</div></td></tr>';
    refreshIcons();
    return;
  }

  root.innerHTML = items
    .map(
      (appointment, index) => `
        <tr>
          <td><span class="turno-id">${escapeHtml(appointment.id)}</span></td>
          <td>
            <div class="table-patient">
              <span class="patient-avatar" style="${avatarStyle(index)}">${escapeHtml(initialsOf(appointment.patient))}</span>
              <div>
                <strong>${escapeHtml(appointment.patient)}</strong>
                <span>${escapeHtml(appointment.patientId)}</span>
              </div>
            </div>
          </td>
          <td>
            <div class="cell-stack">
              <strong>Hoy</strong>
              <span>${escapeHtml(appointment.time)} · ${escapeHtml(appointment.room)}</span>
            </div>
          </td>
          <td>${escapeHtml(appointment.specialty)}</td>
          <td>${escapeHtml(appointment.doctor)}</td>
          <td><span class="status-chip ${appointment.status}">${statusLabel(appointment.status)}</span></td>
          <td>
            <div class="table-actions">
              <button class="icon-btn" data-turno-id="${escapeHtml(appointment.id)}" title="Ver detalle del turno">
                <i data-lucide="more-horizontal"></i>
              </button>
            </div>
          </td>
        </tr>`,
    )
    .join("");

  refreshIcons();
}

function renderTurnosView() {
  const appointments = state.viewAppointments;

  setText("turnosHoyValue", formatNumber(appointments.length));
  setText(
    "turnosConfirmadosValue",
    formatNumber(
      appointments.filter((item) => item.status === "confirmed").length,
    ),
  );
  setText(
    "turnosPendientesValue",
    formatNumber(appointments.filter((item) => item.status === "pending").length),
  );
  setText(
    "turnosAtendidosValue",
    formatNumber(appointments.filter((item) => item.status === "done").length),
  );
  setText("turnosBadge", formatNumber(appointments.length));

  populateTurnoSpecialties();
  renderTurnoTimeline();
  renderTurnosTable();
  renderSpecialtySummary($("#turnoSpecialtySummary"), appointments);
}

/* ============================================================
   Pacientes
   ============================================================ */
function renderPacientesTable() {
  const root = $("#pacientesTable");
  if (!root) return;

  const search = state.pacienteFilters.search.trim().toLowerCase();
  const status = state.pacienteFilters.status;

  const items = state.viewPatients.filter((patient) => {
    const haystack =
      `${patient.name} ${patient.id} ${patient.specialty} ${patient.phone}`.toLowerCase();
    const matchesSearch = !search || haystack.includes(search);
    const matchesStatus = status === "all" || patient.status === status;
    return matchesSearch && matchesStatus;
  });

  setText("pacientesCount", `${items.length} de ${state.viewPatients.length} pacientes`);

  if (!items.length) {
    root.innerHTML =
      '<tr><td colspan="7"><div class="empty-state">No encontramos pacientes con esos filtros.</div></td></tr>';
    refreshIcons();
    return;
  }

  root.innerHTML = items
    .map(
      (patient) => `
        <tr>
          <td>
            <div class="table-patient">
              <span class="patient-avatar" style="${avatarStyle(state.viewPatients.indexOf(patient))}">${escapeHtml(initialsOf(patient.name))}</span>
              <div>
                <strong>${escapeHtml(patient.name)}</strong>
                <span>${escapeHtml(patient.phone)}</span>
              </div>
            </div>
          </td>
          <td>${escapeHtml(patient.id)}</td>
          <td>${patient.age === "—" ? "—" : `${escapeHtml(patient.age)} años`}</td>
          <td>${escapeHtml(patient.lastVisit)}</td>
          <td>${escapeHtml(patient.specialty)}</td>
          <td><span class="status-chip ${patient.status === "pending" ? "pending" : patient.status === "inactive" ? "inactive" : "active"}">${statusLabel(patient.status)}</span></td>
          <td>
            <div class="table-actions">
              <button class="icon-btn" data-patient-id="${escapeHtml(patient.id)}" title="Ver ficha de ${escapeHtml(patient.name)}">
                <i data-lucide="more-horizontal"></i>
              </button>
            </div>
          </td>
        </tr>`,
    )
    .join("");

  refreshIcons();
}

function renderPacienteActivity() {
  const root = $("#pacientesActivity");
  if (!root) return;

  const activities = state.demoMode
    ? DEMO_ACTIVITIES
    : state.viewPatients.slice(0, 4).map((patient, index) => ({
        title: index % 2 === 0 ? "Última visita registrada" : "Paciente en seguimiento",
        detail: `${patient.name} · ${patient.specialty}`,
        time: patient.lastVisit,
        tone: ["teal", "blue", "amber", "coral"][index % 4],
      }));

  if (!activities.length) {
    root.innerHTML =
      '<div class="empty-state">Todavía no hay actividad para mostrar.</div>';
    return;
  }

  root.innerHTML = activities
    .map(
      (activity) => `
        <div class="activity-item">
          <span class="activity-dot ${activity.tone}"></span>
          <div class="activity-copy">
            <strong>${escapeHtml(activity.title)}</strong>
            <span>${escapeHtml(activity.detail)}</span>
          </div>
          <span class="activity-time">${escapeHtml(activity.time)}</span>
        </div>`,
    )
    .join("");
}

function renderPacientesView() {
  const patients = state.viewPatients;

  setText("pacientesTotalValue", formatNumber(patients.length));
  setText(
    "pacientesActivosValue",
    formatNumber(patients.filter((item) => item.status === "active").length),
  );
  setText(
    "pacientesNuevosValue",
    formatNumber(patients.filter((item) => item.isNew).length),
  );
  setText(
    "pacientesTurnoValue",
    formatNumber(patients.filter((item) => item.status === "pending").length),
  );

  renderPacientesTable();
  renderPacienteActivity();
  renderSpecialtySummary($("#pacientesSpecialtySummary"), patients);
}

/* ============================================================
   Resumen por especialidad
   ============================================================ */
function renderSpecialtySummary(root, items) {
  if (!root) return;

  const counts = new Map();
  items.forEach((item) => {
    counts.set(item.specialty, (counts.get(item.specialty) || 0) + 1);
  });

  const entries = [...counts.entries()].sort((a, b) => b[1] - a[1]);

  if (!entries.length) {
    root.innerHTML =
      '<div class="specialty-empty">Sin datos para mostrar.</div>';
    return;
  }

  const max = entries[0][1];

  root.innerHTML = entries
    .map(
      ([specialty, count], index) => `
        <div class="specialty-row">
          <div class="specialty-top">
            <span>${escapeHtml(specialty)}</span>
            <strong>${escapeHtml(count)}</strong>
          </div>
          <div class="specialty-bar">
            <span style="width:${Math.max(8, Math.round((count / max) * 100))}%;background:${SPECIALTY_COLORS[index % SPECIALTY_COLORS.length]}"></span>
          </div>
        </div>`,
    )
    .join("");
}

/* ============================================================
   Modales
   ============================================================ */
function openModal() {
  const backdrop = $("#modalBackdrop");
  if (!backdrop) return;
  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  $('input[name="patient"]')?.focus();
}

function closeModal() {
  const backdrop = $("#modalBackdrop");
  if (!backdrop) return;
  backdrop.classList.remove("open");
  backdrop.setAttribute("aria-hidden", "true");
}

function openPatientModal() {
  const backdrop = $("#patientModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  $('input[name="patientName"]')?.focus();
}

function closePatientModal() {
  const backdrop = $("#patientModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.remove("open");
  backdrop.setAttribute("aria-hidden", "true");
}

/* ============================================================
   Cuenta y perfil
   ============================================================ */
function toggleAccountMenu(forceOpen) {
  const dropdown = $("#accountDropdown");
  const trigger = $("#accountMenuBtn");
  if (!dropdown || !trigger) return;

  const open =
    typeof forceOpen === "boolean" ? forceOpen : dropdown.hidden === true;
  dropdown.hidden = !open;
  trigger.setAttribute("aria-expanded", String(open));
}

function closeAccountMenu() {
  toggleAccountMenu(false);
}

function openProfileModal() {
  closeAccountMenu();

  const backdrop = $("#profileModalBackdrop");
  const form = $("#profileForm");
  if (!backdrop || !form) return;

  const user = state.user || {};
  const name = user.nombre || user.username || "Usuario";

  form.elements.username.value = user.username || "";
  form.elements.email.value = user.email || "";
  form.elements.nombre.value = user.nombre || "";
  form.elements.especialidad.value = user.especialidad || "";
  form.elements.direccion.value = user.direccion || "";
  form.elements.telefono.value = user.telefono || "";

  setText("profileDisplayName", name);
  setText("profileDisplayRole", isAdmin() ? "Administrador" : "Médico");
  setText("profileAvatar", initialsOf(name));

  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  refreshIcons();
  form.elements.nombre.focus();
}

function closeProfileModal() {
  const backdrop = $("#profileModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.remove("open");
  backdrop.setAttribute("aria-hidden", "true");
}

async function logout() {
  closeAccountMenu();

  const response = await api.logout();
  if (!response.ok) {
    toast(response.error || "No se pudo cerrar sesión");
    return;
  }

  window.location.replace("./index.html");
}

/* ============================================================
   Eventos
   ============================================================ */
function bindEvents() {
  $$(".nav-item").forEach((button) => {
    button.addEventListener("click", () => navigate(button.dataset.section));
  });

  $$("[data-goto]").forEach((button) => {
    button.addEventListener("click", () => navigate(button.dataset.goto));
  });

  $("#mobileMenu")?.addEventListener("click", () => {
    $("#sidebar")?.classList.toggle("open");
  });

  $("#accountMenuBtn")?.addEventListener("click", (event) => {
    event.stopPropagation();
    toggleAccountMenu();
  });

  $$("[data-account-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const action = button.dataset.accountAction;
      if (action === "edit") openProfileModal();
      if (action === "logout") logout();
    });
  });

  document.addEventListener("click", (event) => {
    if (!event.target.closest(".account-menu")) closeAccountMenu();
  });

  $("#closeProfileModal")?.addEventListener("click", closeProfileModal);
  $("#cancelProfileModal")?.addEventListener("click", closeProfileModal);
  $("#profileModalBackdrop")?.addEventListener("click", (event) => {
    if (event.target.id === "profileModalBackdrop") closeProfileModal();
  });

  $("#profileForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = form.querySelector('button[type="submit"]');
    if (submit) submit.disabled = true;

    const response = await api.updateProfile({
      nombre: form.elements.nombre.value.trim(),
      especialidad: form.elements.especialidad.value.trim(),
      direccion: form.elements.direccion.value.trim(),
      telefono: form.elements.telefono.value.trim(),
    });

    if (submit) submit.disabled = false;

    if (!response.ok) {
      toast(response.error || "No se pudo actualizar el perfil");
      return;
    }

    state.user = { ...state.user, ...response.user };
    state.role = String(state.user.role || state.role).toUpperCase();
    updateUserChrome();
    applyRoleVisibility();
    closeProfileModal();
    toast("Perfil actualizado correctamente");
  });

  $("#specialtyFilter")?.addEventListener("change", (event) => {
    renderTimeline(event.target.value);
  });

  $("#patientSearch")?.addEventListener("input", (event) => {
    renderPatients(event.target.value);
  });

  [$("#newAppointment"), $("#newTurnoBtn")].forEach((button) => {
    button?.addEventListener("click", openModal);
  });

  $("#closeModal")?.addEventListener("click", closeModal);
  $("#cancelModal")?.addEventListener("click", closeModal);
  $("#modalBackdrop")?.addEventListener("click", (event) => {
    if (event.target.id === "modalBackdrop") closeModal();
  });

  $("#appointmentForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const patient = form.querySelector('input[name="patient"]')?.value.trim();
    closeModal();
    form.reset();
    renderFecha();
    toast(
      patient
        ? `Turno de ${patient} creado correctamente`
        : "Turno creado correctamente",
    );
  });

  $("#newPacienteBtn")?.addEventListener("click", openPatientModal);
  $("#closePatientModal")?.addEventListener("click", closePatientModal);
  $("#cancelPatientModal")?.addEventListener("click", closePatientModal);
  $("#patientModalBackdrop")?.addEventListener("click", (event) => {
    if (event.target.id === "patientModalBackdrop") closePatientModal();
  });

  $("#patientForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const patient = form
      .querySelector('input[name="patientName"]')
      ?.value.trim();
    closePatientModal();
    form.reset();
    toast(
      patient
        ? `${patient} se registró correctamente`
        : "Paciente registrado correctamente",
    );
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeModal();
      closePatientModal();
      closeProfileModal();
      closeAccountMenu();
    }
  });

  $("#exportBtn")?.addEventListener("click", () => {
    toast("Reporte preparado para descargar");
  });
  $("#exportTurnos")?.addEventListener("click", () => {
    toast("Listado de turnos preparado para descargar");
  });
  $("#exportPacientes")?.addEventListener("click", () => {
    toast("Directorio de pacientes preparado para descargar");
  });

  $("#manageQueue")?.addEventListener("click", () => {
    toast("Cola actualizada en tiempo real");
  });

  $("#viewCalendar")?.addEventListener("click", () => navigate("Turnos"));
  $("#viewPatients")?.addEventListener("click", () => navigate("Pacientes"));

  ["#prevDay", "#nextDay", "#turnosPrevDay", "#turnosNextDay"].forEach(
    (selector) => {
      $(selector)?.addEventListener("click", () => {
        toast(
          selector.includes("prev")
            ? "Mostrando el día anterior"
            : "Mostrando el día siguiente",
        );
      });
    },
  );

  $("#turnoSearch")?.addEventListener("input", (event) => {
    state.turnoFilters.search = event.target.value;
    renderTurnoTimeline();
    renderTurnosTable();
  });

  $("#turnoSpecialtyFilter")?.addEventListener("change", (event) => {
    state.turnoFilters.specialty = event.target.value;
    renderTurnoTimeline();
    renderTurnosTable();
  });

  $("#turnoStatusFilter")?.addEventListener("change", (event) => {
    state.turnoFilters.status = event.target.value;
    renderTurnosTable();
  });

  $("#pacientesSearch")?.addEventListener("input", (event) => {
    state.pacienteFilters.search = event.target.value;
    renderPacientesTable();
  });

  $("#pacientesEstadoFilter")?.addEventListener("change", (event) => {
    state.pacienteFilters.status = event.target.value;
    renderPacientesTable();
  });

  $("#turnosTable")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-turno-id]");
    if (!button) return;
    toast(`Abriendo detalle del turno ${button.dataset.turnoId}`);
  });

  $("#pacientesTable")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-patient-id]");
    if (!button) return;
    const patient = state.viewPatients.find(
      (item) => item.id === button.dataset.patientId,
    );
    toast(patient ? `Abriendo ficha de ${patient.name}` : "Abriendo ficha");
  });

  $("#patientsTable")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-patient-id]");
    if (!button) return;
    const patient = state.resumenPatients.find(
      (item) => item.id === button.dataset.patientId,
    );
    toast(patient ? `Abriendo ficha de ${patient.name}` : "Abriendo ficha");
  });

  window.addEventListener("hashchange", () => {
    const section = decodeURIComponent(window.location.hash.slice(1));
    navigate(section || "Resumen", false);
  });
}

/* ============================================================
   Inicio
   ============================================================ */
document.addEventListener("DOMContentLoaded", async () => {
  refreshIcons();
  renderFecha();
  bindEvents();

  const sessionReady = await initSession();
  if (!sessionReady) return;

  const requested = decodeURIComponent(window.location.hash.slice(1));
  const initial =
    requested && (!ADMIN_ONLY.has(requested) || isAdmin())
      ? requested
      : "Resumen";

  navigate(initial, false);
  await loadDashboardData();
  refreshIcons();
});
