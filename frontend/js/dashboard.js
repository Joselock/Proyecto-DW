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
  contexto: null,
  contextoReady: false,
  catalogos: null,
  catalogosLoading: false,
  catalogosPromise: null,
  profesionales: null,
  profesionalesLoading: false,
  profesionalFilters: { search: "", specialty: "all" },
  reportes: {
    area: "Hospital",
    scope: "",
    loading: false,
    loaded: false,
    results: {},
    expanded: {},
    lastUpdate: null,
  },
  settings: null,
  help: { search: "", tag: "" },
  // Datos reales de las secciones Pacientes y Turnos, filtrados por rol en el backend.
  panel: {
    pacientes: [],
    turnos: [],
    totalPacientes: 0,
    totalTurnos: 0,
    alcance: null,
    medico: null,
    cargado: false,
  },
  pagina: { pacientes: 0, turnos: 0 },
};

const PAGE_SIZE = 20;
const RECENT_PATIENTS_LIMIT = 6;

const ADMIN_ONLY = new Set(["Profesionales", "Reportes"]);

const GENERIC_SECTIONS = {
  Profesionales: {
    eyebrow: "Equipo médico",
    title: "Profesionales",
    subhead:
      "Administra especialidades, disponibilidad y asignación del equipo.",
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

const SETTINGS_KEY = "clara.dashboard.settings";

const DEFAULT_SETTINGS = {
  density: "comfortable",
  accent: "teal",
  dateFormat: "long",
  animations: "on",
  notifications: true,
  emailDigest: false,
  helpCard: true,
  privacy: false,
};

const ACCENTS = {
  teal: { teal: "#1ea99a", dark: "#087e74" },
  blue: { teal: "#4c84c4", dark: "#31639b" },
  coral: { teal: "#ee806f", dark: "#c65a4a" },
  amber: { teal: "#d99a35", dark: "#a8731b" },
};

const REPORT_DEFS = [
  {
    key: "pacientes",
    number: 1,
    short: "Pacientes",
    title: "Pacientes por área",
    description:
      "Pacientes registrados en el hospital, departamento o unidad seleccionados.",
    scoped: true,
    fetch: (args) => api.listadoPacientes(args.area, args.code),
    columns: [
      {
        label: "Historia",
        render: (row) => `<strong>${escapeHtml(row.historia)}</strong>`,
      },
      {
        label: "Paciente",
        render: (row) =>
          cellStack(
            row.nombre,
            row.nacimiento ? `Nac. ${formatDateValue(row.nacimiento)}` : "",
          ),
      },
      { label: "Edad", render: (row) => escapeHtml(row.edad) },
      { label: "Dirección", render: (row) => escapeHtml(row.direccion || "—") },
      { label: "Ubicación", render: (row) => locationCell(row) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        for (const patient of unit?.registro?.pacientes || []) {
          rows.push({
            historia: patient.numHisCli || "—",
            nombre: patient.nombrePac || "Paciente",
            nacimiento: patient.fechaN,
            edad: calculateAge(patient.fechaN),
            direccion: patient.direccionP,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "medicos",
    number: 2,
    short: "Médicos",
    title: "Listado de médicos",
    description:
      "Profesionales de la red con especialidad, licencia y unidad asignada.",
    scoped: false,
    fetch: () => api.listadoMedicos(),
    columns: [
      {
        label: "Médico",
        render: (row) =>
          cellStack(row.nombre, row.licencia ? `Lic. ${row.licencia}` : ""),
      },
      { label: "Especialidad", render: (row) => escapeHtml(row.especialidad) },
      { label: "Hospital", render: (row) => escapeHtml(row.hospital) },
      {
        label: "Departamento / Unidad",
        render: (row) => cellStack(row.unidad, row.departamento),
      },
      {
        label: "Experiencia",
        render: (row) => `${escapeHtml(row.experiencia)} años`,
      },
      { label: "Teléfono", render: (row) => escapeHtml(row.telefono || "—") },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        for (const doctor of unit?.medicos || []) {
          rows.push({
            nombre: doctor.nombreMed || "Profesional",
            especialidad: doctor.especialidad || "Sin especialidad",
            licencia: doctor.numeroLic || "—",
            experiencia: doctor.experiencia ?? 0,
            telefono: doctor.telefono,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "resumen",
    number: 3,
    short: "Hospitales",
    title: "Resumen de hospitales",
    description:
      "Cantidad de departamentos, unidades, médicos y pacientes por hospital.",
    scoped: false,
    fetch: () => api.resumenHospitales(),
    columns: [
      {
        label: "Hospital",
        render: (row) => `<strong>${escapeHtml(row.hospital)}</strong>`,
      },
      {
        label: "Departamentos",
        render: (row) => formatNumber(row.departamentos),
      },
      { label: "Unidades", render: (row) => formatNumber(row.unidades) },
      { label: "Médicos", render: (row) => formatNumber(row.medicos) },
      { label: "Pacientes", render: (row) => formatNumber(row.pacientes) },
    ],
    rows: (data) =>
      Array.isArray(data)
        ? data.map((hospital) => ({
            hospital: hospital.nombreHos || "Hospital",
            departamentos: hospital.cantDep || 0,
            unidades: hospital.cantUni || 0,
            medicos: hospital.cantMed || 0,
            pacientes: hospital.cantPac || 0,
          }))
        : [],
  },
  {
    key: "mayores",
    number: 4,
    short: "Más de 100",
    title: "Hospitales con más de 100 pacientes",
    description: "Hospitales que superan el centenar de pacientes registrados.",
    scoped: false,
    fetch: () => api.hospitalesMayorCantidadPacientes(),
    columns: [
      {
        label: "Hospital",
        render: (row) => `<strong>${escapeHtml(row.hospital)}</strong>`,
      },
      { label: "Pacientes", render: (row) => formatNumber(row.pacientes) },
      {
        label: "Nivel",
        render: (row) =>
          row.pacientes > 250
            ? '<span class="status-chip confirmed">Alta demanda</span>'
            : '<span class="status-chip pending">En seguimiento</span>',
      },
    ],
    rows: (data) =>
      Array.isArray(data)
        ? data.map((hospital) => ({
            hospital: hospital.nombreHos || "Hospital",
            pacientes: hospital.cantPac || 0,
          }))
        : [],
  },
  {
    key: "informe",
    number: 5,
    short: "Informe consultas",
    title: "Informe durante las consultas",
    description: "Movimiento de pacientes reportado en cada turno de consulta.",
    scoped: true,
    fetch: (args) => api.informeDuranteConsultas(args.area, args.code),
    columns: [
      { label: "Turno", render: (row) => turnoCell(row.turno) },
      {
        label: "Fecha / Hora",
        render: (row) =>
          cellStack(formatDateValue(row.fecha), formatTimeValue(row.hora)),
      },
      { label: "Informe", render: (row) => escapeHtml(row.informe || "—") },
      { label: "Pac. inicial", render: (row) => formatNumber(row.inicial) },
      { label: "Admitidos", render: (row) => formatNumber(row.admitidos) },
      { label: "Altas", render: (row) => formatNumber(row.altas) },
      { label: "Total día", render: (row) => formatNumber(row.total) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        const informes = unit?.informes || [];
        const turnos = unit?.turnos || [];
        const length = Math.max(informes.length, turnos.length);
        for (let index = 0; index < length; index += 1) {
          const informe = informes[index] || {};
          const turno = turnos[index] || {};
          rows.push({
            turno: turno.numTurn,
            fecha: informe.fecha,
            hora: informe.hora,
            informe: informe.numIn,
            inicial: informe.cantIni,
            admitidos: informe.cantAdm,
            altas: informe.pacAlta,
            total: informe.total,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "noAtendidos",
    number: 6,
    short: "No atendidos",
    title: "Pacientes no atendidos",
    description:
      "Pacientes que quedaron sin atención y el motivo registrado en su turno.",
    scoped: true,
    fetch: (args) => api.pacientesNoAtendidos(args.area, args.code),
    columns: [
      {
        label: "Historia",
        render: (row) => `<strong>${escapeHtml(row.historia)}</strong>`,
      },
      { label: "Paciente", render: (row) => escapeHtml(row.nombre) },
      { label: "Turno", render: (row) => turnoCell(row.turno) },
      {
        label: "Causa",
        render: (row) => escapeHtml(row.causa || "Sin causa registrada"),
      },
      { label: "No atendidos", render: (row) => formatNumber(row.noAtendidos) },
      { label: "Ubicación", render: (row) => locationCell(row) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        for (const turno of unit?.turnos || []) {
          const consulta = turno.consulta || {};
          const paciente = consulta.paciente || {};
          rows.push({
            historia: consulta.numH || paciente.numHisCli || "—",
            nombre: paciente.nombrePac || "Paciente",
            turno: turno.numTurn,
            causa: consulta.causa,
            noAtendidos: turno.cantNoAten,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "proceso",
    number: 7,
    short: "Proceso",
    title: "Resumen del proceso",
    description:
      "Pacientes atendidos, altas y porcentaje de atención por turno.",
    scoped: false,
    fetch: () => api.resumenProceso(),
    columns: [
      { label: "Turno", render: (row) => turnoCell(row.turno) },
      { label: "Hora", render: (row) => escapeHtml(formatTimeValue(row.hora)) },
      { label: "Atendidos", render: (row) => formatNumber(row.atendidos) },
      { label: "% atendidos", render: (row) => meterCell(row.porcentaje) },
      { label: "Altas", render: (row) => formatNumber(row.altas) },
      { label: "No atendidos", render: (row) => formatNumber(row.noAtendidos) },
      { label: "Total", render: (row) => formatNumber(row.total) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        const informes = unit?.informes || [];
        const turnos = unit?.turnos || [];
        const length = Math.max(informes.length, turnos.length);
        for (let index = 0; index < length; index += 1) {
          const informe = informes[index] || {};
          const turno = turnos[index] || {};
          rows.push({
            turno: turno.numTurn,
            hora: informe.hora,
            atendidos: informe.pacAtend,
            porcentaje: informe.porcentajePacAtend,
            altas: informe.pacAlta,
            noAtendidos: turno.cantNoAten,
            total: informe.total,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "revisar",
    number: 8,
    short: "Por revisar",
    title: "Unidades con turnos por revisar",
    description:
      "Unidades ordenadas por médicos y porcentaje de pacientes atendidos.",
    scoped: false,
    fetch: () => api.unidadesTurnosPorRevisar(),
    columns: [
      {
        label: "Médico",
        render: (row) => `<strong>${escapeHtml(row.medico)}</strong>`,
      },
      {
        label: "Ubicación",
        render: (row) =>
          cellStack(row.unidad, `${row.departamento} · ${row.hospital}`),
      },
      { label: "Total pacientes", render: (row) => formatNumber(row.total) },
      { label: "Atendidos", render: (row) => formatNumber(row.atendidos) },
      { label: "% atención", render: (row) => meterCell(row.porcentaje) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        const medicos = unit?.medicos || [];
        const informes = unit?.informes || [];
        const length = Math.max(medicos.length, informes.length);
        for (let index = 0; index < length; index += 1) {
          const medico = medicos[index] || {};
          const informe = informes[index] || {};
          rows.push({
            medico: medico.nombreMed || "Sin médico asignado",
            total: informe.total,
            atendidos: informe.pacAtend,
            porcentaje: informe.porcentajePacAtend,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
  {
    key: "exitosas",
    number: 9,
    short: "Consultas exitosas",
    title: "Resumen de consultas exitosas",
    description:
      "Atenciones efectivas por médico y turno en el ámbito seleccionado.",
    scoped: true,
    fetch: (args) => api.consultasExitosas(args.area, args.code),
    columns: [
      {
        label: "Médico",
        render: (row) => `<strong>${escapeHtml(row.medico)}</strong>`,
      },
      { label: "Turno", render: (row) => turnoCell(row.turno) },
      {
        label: "Pacientes atendidos",
        render: (row) => formatNumber(row.pacientes),
      },
      { label: "Atenciones", render: (row) => formatNumber(row.atenciones) },
      { label: "Ubicación", render: (row) => locationCell(row) },
    ],
    rows: (data) => {
      const rows = [];
      eachUnit(data, ({ hospital, departamento, unidad, unit }) => {
        const informes = unit?.informes || [];
        const turnos = unit?.turnos || [];
        const length = Math.max(informes.length, turnos.length);
        for (let index = 0; index < length; index += 1) {
          const informe = informes[index] || {};
          const turno = turnos[index] || {};
          rows.push({
            medico: turno.medico?.nombreMed || "Sin médico asignado",
            turno: turno.numTurn,
            pacientes: informe.pacAtend,
            atenciones: turno.canAten,
            hospital,
            departamento,
            unidad,
          });
        }
      });
      return rows;
    },
  },
];

const DEMO_PATIENTS = [];
const DEMO_APPOINTMENTS = [];

/*const DEMO_PATIENTS = [
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
];*/

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

  renderSettings();
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

  if (requested === "Profesionales") {
    loadProfesionales();
  } else if (requested === "Reportes") {
    if (!state.reportes.loaded) loadReportes();
    else renderReportes();
  } else if (requested === "Configuración") {
    renderSettings();
  } else if (requested === "Ayuda") {
    applyHelpFilters();
    loadAyudaStatus();
  }

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
    fechaLarga.textContent =
      state.settings?.dateFormat === "short"
        ? obtenerFechaCorta(hoy)
        : obtenerFecha(hoy);
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
        const unitPatients = unit?.registro?.pacientes || unit?.pacientes || [];

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

/* Convierte las filas del endpoint /api/panel al formato que ya usan las
   tablas del dashboard. El backend es la única fuente de la regla por rol. */
function mapPacientePanel(row) {
  return {
    id: row.id,
    name: row.nombre || "Paciente",
    age: row.edad ?? calculateAge(row.nacimiento),
    phone: "Sin teléfono registrado",
    specialty: row.departamento || row.unidad || "Medicina general",
    doctor: row.medico || "Equipo médico",
    lastVisit: row.ultimaVisita
      ? formatDateValue(row.ultimaVisita)
      : row.admision
        ? formatDateValue(row.admision)
        : "—",
    status: row.estatus || "active",
    isNew: Boolean(row.esNuevo),
    hospital: row.hospital,
    departamento: row.departamento,
    unidad: row.unidad,
    direccion: row.direccion,
  };
}

function mapTurnoPanel(row) {
  return {
    id: row.id,
    numero: row.numero,
    time: row.hora || "—",
    patient: row.paciente || "Sin paciente",
    patientId: row.pacienteId || "—",
    specialty: row.especialidad || row.unidad || "Medicina general",
    doctor: row.profesional || "Sin profesional",
    status: row.estatus || "pending",
    room: row.consultorio || "Consultorio 1",
    fecha: row.fecha,
    hospital: row.hospital,
    unidad: row.unidad,
  };
}

function renderPanelScope() {
  const global = state.panel.alcance !== "medico";
  const medico = state.panel.medico || "";

  setText(
    "pacientesScopePill",
    global
      ? "Todos los hospitales"
      : `Solo mis pacientes${medico ? ` · ${medico}` : ""}`,
  );
  setText(
    "turnosScopePill",
    global ? "Todos los hospitales" : "Solo mis turnos",
  );
  setText(
    "pacientesSubhead",
    global
      ? "Directorio completo de pacientes de todos los hospitales de la red."
      : "Pacientes que atiendes o que están ingresados en tu unidad.",
  );
  setText(
    "turnosSubhead",
    global
      ? "Agenda completa de todos los hospitales de la red."
      : "Turnos asignados exclusivamente a tu ficha profesional.",
  );
}

function updateResumenStats() {
  // Usa el mismo alcance que las secciones Pacientes y Turnos: un ADMIN ve
  // toda la red y un MEDICO solo sus propios datos.
  const totalTurnos =
    state.panel.totalTurnos || state.viewAppointments.length;
  const totalPacientes = state.panel.totalPacientes || state.viewPatients.length;
  const atendidos = state.viewAppointments.filter(
    (turno) => turno.status === "done",
  ).length;

  const cards = $$("#view-Resumen .stat-card");
  const values = [totalTurnos, atendidos, null, totalPacientes];

  cards.forEach((card, index) => {
    if (values[index] === null) return;
    const value = card.querySelector(".stat-value");
    if (value) value.textContent = formatNumber(values[index]);
  });
}

async function loadDashboardData() {
  let backendPatients = [];
  let backendTurnos = [];
  let datosPanel = false;

  const [pacientesRes, turnosRes, resumenRes] = await Promise.all([
    api.pacientesPanel(),
    api.turnosPanel(),
    api.resumenHospitales(),
  ]);

  if (!resumenRes.ok) {
    console.warn("No se pudo cargar el resumen:", resumenRes.error);
  }

  if (pacientesRes.ok && turnosRes.ok) {
    const filasPacientes = Array.isArray(pacientesRes.data?.rows)
      ? pacientesRes.data.rows
      : [];
    const filasTurnos = Array.isArray(turnosRes.data?.rows)
      ? turnosRes.data.rows
      : [];

    state.panel = {
      pacientes: filasPacientes,
      turnos: filasTurnos,
      totalPacientes: pacientesRes.data?.total ?? filasPacientes.length,
      totalTurnos: turnosRes.data?.total ?? filasTurnos.length,
      alcance: turnosRes.data?.alcance || pacientesRes.data?.alcance,
      medico: turnosRes.data?.medico || pacientesRes.data?.medico,
      cargado: true,
    };

    backendPatients = filasPacientes.map(mapPacientePanel);
    backendTurnos = filasTurnos.map(mapTurnoPanel);
    datosPanel = backendPatients.length > 0 || backendTurnos.length > 0;
  } else {
    console.warn(
      "No se pudieron cargar los listados del panel:",
      pacientesRes.error || turnosRes.error,
    );
  }

  // Respaldo: si la API del panel no responde, se mantiene el comportamiento
  // anterior usando el reporte de resumen (o los datos de ejemplo).
  if (!datosPanel && resumenRes.ok && Array.isArray(resumenRes.data)) {
    backendPatients = extractBackendPatients(resumenRes.data);
    backendTurnos = buildAppointmentsFromPatients(backendPatients);
    state.panel.alcance = "global";
  }

  state.resumenPatients = backendPatients;
  state.resumenAppointments = backendTurnos.slice(0, 6);
  state.viewPatients = backendPatients;
  state.viewAppointments = backendTurnos;
  state.demoMode = !backendPatients.length && !backendTurnos.length;

  if (state.demoMode) {
    state.viewPatients = DEMO_PATIENTS;
    state.viewAppointments = DEMO_APPOINTMENTS;
    state.resumenAppointments = DEMO_APPOINTMENTS.slice(0, 6);
  }

  state.queue = buildQueue(state.viewAppointments);
  state.pagina = { pacientes: 0, turnos: 0 };
  renderPanelScope();
  updateDemoPills();
  renderTimeline();
  renderQueue();
  renderPatients();
  renderTurnosView();
  renderPacientesView();
  updateResumenStats();
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

  root.classList.toggle("is-empty", !items.length);
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

function buildQueue(appointments) {
  return appointments
    .filter((appointment) => appointment.status === "pending")
    .slice(0, 5)
    .map((appointment, index) => ({
      initials: initialsOf(appointment.patient),
      name: appointment.patient,
      detail: `${appointment.id} · ${appointment.specialty}`,
      time: `${(index + 1) * 7} min`,
    }));
}

function renderQueue() {
  const root = $("#queueList");
  if (!root) return;

  const totals = $$("#view-Resumen .queue-summary strong");
  const enEspera = state.viewAppointments.filter(
    (appointment) => appointment.status === "pending",
  ).length;
  const enConsulta = state.viewAppointments.filter(
    (appointment) => appointment.status === "confirmed",
  ).length;
  const atendidos = state.viewAppointments.filter(
    (appointment) => appointment.status === "done",
  ).length;
  [enEspera, enConsulta, atendidos].forEach((value, index) => {
    if (totals[index]) totals[index].textContent = String(value).padStart(2, "0");
  });

  root.classList.toggle("is-empty", !state.queue.length);
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
  const items = filtered.slice(0, RECENT_PATIENTS_LIMIT);

  setText(
    "patientsRecentCount",
    `${formatNumber(items.length)} de ${formatNumber(filtered.length)} pacientes recientes`,
  );

  if (!items.length) {
    root.innerHTML =
      '<tr><td colspan="6"><div class="empty-state">No encontramos pacientes con esa búsqueda.</div></td></tr>';
    refreshIcons();
    return;
  }

  root.innerHTML = items
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

  // La agenda muestra una vista rápida; el listado completo está más abajo.
  const items = filterAppointments(false).slice(0, 8);

  root.classList.toggle("is-empty", !items.length);
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

function renderMoreButton(containerId, clave, mostrados, total) {
  const root = document.getElementById(containerId);
  if (!root) return;

  root.innerHTML =
    mostrados < total
      ? `<button type="button" class="text-btn" data-more="${clave}">
           Mostrar ${Math.min(PAGE_SIZE, total - mostrados)} más
           <i data-lucide="chevron-down"></i>
         </button>`
      : "";
  refreshIcons();
}

function renderTurnosTable() {
  const root = $("#turnosTable");
  if (!root) return;

  const filtrados = filterAppointments(true);
  const inicio = state.pagina.turnos * PAGE_SIZE;
  const items = filtrados.slice(inicio, inicio + PAGE_SIZE);

  setText(
    "turnosCount",
    `${formatNumber(filtrados.length)} de ${formatNumber(state.viewAppointments.length)} turnos`,
  );
  renderMoreButton(
    "turnosMore",
    "turnos",
    inicio + items.length,
    filtrados.length,
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
    formatNumber(
      appointments.filter((item) => item.status === "pending").length,
    ),
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

  const filtrados = state.viewPatients.filter((patient) => {
    const haystack =
      `${patient.name} ${patient.id} ${patient.specialty} ${patient.phone} ${patient.doctor} ${patient.hospital || ""}`.toLowerCase();
    const matchesSearch = !search || haystack.includes(search);
    const matchesStatus = status === "all" || patient.status === status;
    return matchesSearch && matchesStatus;
  });

  const inicio = state.pagina.pacientes * PAGE_SIZE;
  const items = filtrados.slice(inicio, inicio + PAGE_SIZE);

  setText(
    "pacientesCount",
    `${formatNumber(filtrados.length)} de ${formatNumber(state.viewPatients.length)} pacientes`,
  );
  renderMoreButton(
    "pacientesMore",
    "pacientes",
    inicio + items.length,
    filtrados.length,
  );

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
        title:
          index % 2 === 0
            ? "Última visita registrada"
            : "Paciente en seguimiento",
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
   Reportes: utilidades de formato
   ============================================================ */
function formatDateValue(value) {
  if (!value) return "—";
  const raw = String(value);
  const date = new Date(raw.includes("T") ? raw : `${raw}T00:00:00`);

  if (Number.isNaN(date.getTime())) return raw;

  const options =
    state.settings?.dateFormat === "short"
      ? { day: "2-digit", month: "2-digit", year: "numeric" }
      : { day: "numeric", month: "short", year: "numeric" };

  return new Intl.DateTimeFormat("es-ES", options).format(date);
}

function formatTimeValue(value) {
  if (!value) return "—";
  return String(value).slice(0, 5);
}

function formatPercent(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "—";
  return `${new Intl.NumberFormat("es-ES", {
    maximumFractionDigits: 1,
  }).format(number)}%`;
}

function cellStack(strong, sub) {
  const title = strong || "—";
  return `<div class="cell-stack"><strong>${escapeHtml(title)}</strong>${
    sub ? `<span>${escapeHtml(sub)}</span>` : ""
  }</div>`;
}

function locationCell(row) {
  const title = row.unidad || row.hospital || "—";
  const detail = [row.departamento, row.hospital].filter(Boolean).join(" · ");
  return cellStack(title, detail === title ? "" : detail);
}

function turnoCell(value) {
  if (value === undefined || value === null || value === "") return "—";
  return `<span class="turno-id">#${escapeHtml(value)}</span>`;
}

function meterCell(value) {
  const number = Number(value);
  const safe = Number.isFinite(number) ? Math.max(0, Math.min(100, number)) : 0;
  return `<div class="report-metric"><div class="report-meter"><span style="width:${safe}%"></span></div><span>${formatPercent(number)}</span></div>`;
}

function eachUnit(data, callback) {
  if (!Array.isArray(data)) return;

  for (const hospital of data) {
    const hospitalName = hospital?.nombreHos || hospital?.hospital || "—";

    for (const department of hospital?.departamentos || []) {
      const departmentName = department?.nombreDep || "—";

      for (const unit of department?.unidades || []) {
        callback({
          hospital: hospitalName,
          departamento: departmentName,
          unidad: unit?.nombreUni || "—",
          unit,
        });
      }
    }
  }
}

/* ============================================================
   Contexto hospitalario
   ============================================================ */
function hospitalByCode(code) {
  return (
    (state.contexto?.hospitales || []).find(
      (hospital) => hospital.codigo === code,
    ) || null
  );
}

function departamentoByCode(code) {
  return (
    (state.contexto?.departamentos || []).find(
      (department) => department.codigo === code,
    ) || null
  );
}

function unidadByCode(code) {
  return (
    (state.contexto?.unidades || []).find((unit) => unit.codigo === code) ||
    null
  );
}

async function loadContexto() {
  const response = await api.contextoReportes();

  if (!response.ok || !response.data) {
    console.warn("No se pudo cargar el contexto hospitalario:", response.error);
    return;
  }

  state.contexto = response.data;
  state.contextoReady = true;

  const preferred =
    response.data.hospital?.codigo ||
    response.data.hospitales?.[0]?.codigo ||
    "";
  state.reportes.scope = preferred;
  populateReporteScopes(preferred);

  if (state.reportes.loaded && state.section === "Reportes") {
    loadReportes({ scopedOnly: true });
  }

  renderSettings();
}

/* ============================================================
   Catálogos de altas (hospital → departamento → unidad)
   ============================================================ */
async function loadCatalogos(force = false) {
  if (state.catalogos && !force) return state.catalogos;
  // Si ya hay una carga en curso, esperamos a que termine. Así los formularios
  // nunca se pintan con catálogos vacíos cuando se abren justo al entrar.
  if (state.catalogosLoading && state.catalogosPromise) {
    return state.catalogosPromise;
  }

  state.catalogosLoading = true;
  state.catalogosPromise = (async () => {
    try {
      const response = await api.catalogosAltas();
      if (response.ok && response.data) {
        state.catalogos = response.data;
      } else {
        console.warn("No se pudieron cargar los catálogos de altas:", response.error);
        state.catalogos = {
          hospitales: [],
          departamentos: [],
          unidades: [],
          medicos: [],
          perfil: {},
        };
      }
    } finally {
      state.catalogosLoading = false;
    }
    return state.catalogos;
  })();

  try {
    return await state.catalogosPromise;
  } finally {
    state.catalogosPromise = null;
  }
}

function fillSelect(select, items, placeholder, valueKey = "codigo", labelKey = "nombre", selected = "") {
  if (!select) return;
  const valores = items.map((item) => String(item[valueKey] ?? ""));
  select.innerHTML = [
    `<option value="">${escapeHtml(placeholder)}</option>`,
    ...items.map(
      (item) =>
        `<option value="${escapeHtml(String(item[valueKey]))}">${escapeHtml(String(item[labelKey]))}</option>`,
    ),
  ].join("");
  const valor = String(selected || "");
  select.value = valores.includes(valor) ? valor : "";
  select.disabled = false;
}

function unidadesParaSelect() {
  return (state.catalogos?.unidades || []).map((unit) => ({
    ...unit,
    nombre: `${unit.nombre}${unit.codigo ? ` · ${unit.codigo}` : ""}`,
  }));
}

function populateTurnoForm() {
  const perfil = state.catalogos?.perfil || {};
  const medicos = state.catalogos?.medicos || [];
  const pacientes = state.panel.pacientes.map((row) => ({
    codigo: row.id,
    nombre: `${row.nombre} · ${row.id}${row.unidad ? ` · ${row.unidad}` : ""}`,
  }));
  const medicoSelect = $("#turnoMedico");
  const pacienteSelect = $("#turnoPaciente");
  const medicoActual = medicoSelect?.value || "";
  const medicoSeleccionado = isAdmin()
    ? medicoActual
    : perfil.codMed || "";

  fillSelect(
    pacienteSelect,
    pacientes,
    pacientes.length ? "Selecciona un paciente" : "No hay pacientes disponibles",
    "codigo",
    "nombre",
    pacienteSelect?.value || "",
  );
  fillSelect(
    medicoSelect,
    medicos.map((medico) => ({
      ...medico,
      nombre: `${medico.nombre} · ${medico.especialidad}${medico.unidad ? ` · ${medico.unidad}` : ""}`,
    })),
    medicos.length ? "Selecciona un profesional" : "No hay profesionales disponibles",
    "codMed",
    "nombre",
    medicoSeleccionado,
  );
  if (medicoSeleccionado) {
    medicoSelect.value = medicoSeleccionado;
  }
  sincronizarUnidadTurno();

  const submit = $("#appointmentSubmit");
  const help = $("#turnoFormHelp");
  const medicoFijado = !isAdmin() && Boolean(perfil.codMed);
  let mensaje =
    "Selecciona un paciente y un profesional. La unidad se toma de la ficha del profesional.";
  let bloqueado = false;

  if (!isAdmin() && !perfil.codMed) {
    mensaje =
      "Tu cuenta no tiene una ficha médica vinculada. Pide a un administrador que te asocie un profesional para crear turnos.";
    bloqueado = true;
  } else if (!pacientes.length) {
    mensaje = "Todavía no hay pacientes visibles para este perfil.";
    bloqueado = true;
  } else if (!medicos.length) {
    mensaje = "Todavía no hay profesionales disponibles para este perfil.";
    bloqueado = true;
  } else if (medicoFijado) {
    mensaje =
      "Tu turno queda asignado a tu ficha profesional; la unidad se toma de ella.";
  }
  if (help) help.textContent = mensaje;
  if (submit) submit.disabled = bloqueado;
}

function sincronizarUnidadTurno() {
  const unidad = $("#turnoUnidad");
  if (!unidad) return;
  const medico = (state.catalogos?.medicos || []).find(
    (item) => item.codMed === $("#turnoMedico")?.value,
  );
  if (!medico) {
    unidad.innerHTML = '<option value="">Selecciona un profesional</option>';
    unidad.value = "";
    return;
  }
  unidad.innerHTML =
    `<option value="${escapeHtml(String(medico.codUni))}">` +
    `${escapeHtml(medico.unidad || medico.codUni)}</option>`;
  unidad.value = medico.codUni;
}

function populatePacienteForm() {
  const perfil = state.catalogos?.perfil || {};
  const unidades = unidadesParaSelect();
  const unidadSelect = $("#pacienteUnidad");
  const actual = unidadSelect?.value || perfil.codUni || "";

  fillSelect(
    unidadSelect,
    unidades,
    unidades.length ? "Selecciona la unidad" : "No hay unidades disponibles",
    "codigo",
    "nombre",
    actual,
  );

  const submit = $("#patientSubmit");
  const help = $("#patientFormHelp");
  if (help) {
    help.textContent = !unidades.length
      ? "No hay unidades disponibles para este perfil."
      : isAdmin()
        ? "El documento se genera automáticamente si se deja vacío. El hospital y la especialidad se deducen de la unidad."
        : `El alta se registra en tu unidad (${unidades[0]?.nombre || perfil.codUni || "—"}). El documento se genera automáticamente si se deja vacío.`;
  }
  if (submit) submit.disabled = !unidades.length || !isAdmin();
}

function populateMedicoForm() {
  const catalogos = state.catalogos || {};
  const hospitales = catalogos.hospitales || [];
  const departamentos = catalogos.departamentos || [];
  const unidades = catalogos.unidades || [];
  const hospitalSelect = $("#medicoHospital");
  const departamentoSelect = $("#medicoDepartamento");
  const unidadSelect = $("#medicoUnidad");

  const hospital = hospitalSelect?.value || hospitales[0]?.codigo || "";
  fillSelect(
    hospitalSelect,
    hospitales,
    hospitales.length ? "Selecciona el hospital" : "No hay hospitales disponibles",
    "codigo",
    "nombre",
    hospital,
  );

  const departamentosHospital = departamentos.filter(
    (item) => !hospital || item.codHos === hospital,
  );
  const departamento =
    departamentoSelect?.value &&
    departamentosHospital.some((item) => item.codigo === departamentoSelect.value)
      ? departamentoSelect.value
      : departamentosHospital[0]?.codigo || "";
  fillSelect(
    departamentoSelect,
    departamentosHospital,
    departamentosHospital.length
      ? "Selecciona el departamento"
      : "Selecciona primero un hospital",
    "codigo",
    "nombre",
    departamento,
  );

  const unidadesDepartamento = unidades.filter(
    (item) => !departamento || item.codDep === departamento,
  );
  const unidad =
    unidadSelect?.value &&
    unidadesDepartamento.some((item) => item.codigo === unidadSelect.value)
      ? unidadSelect.value
      : unidadesDepartamento[0]?.codigo || "";
  fillSelect(
    unidadSelect,
    unidadesDepartamento,
    unidadesDepartamento.length
      ? "Selecciona la unidad"
      : "Selecciona primero un departamento",
    "codigo",
    "nombre",
    unidad,
  );

  const submit = $("#medicoSubmit");
  if (submit) submit.disabled = !unidadesDepartamento.length;
}

function sortByUserHospital(items, hospitalCodeOf) {
  const userHospital = state.contexto?.hospital?.codigo;
  return [...items].sort((a, b) => {
    const aValue = hospitalCodeOf(a) === userHospital ? 0 : 1;
    const bValue = hospitalCodeOf(b) === userHospital ? 0 : 1;
    return aValue - bValue;
  });
}

function scopeOptionsFor(area) {
  const contexto = state.contexto || {};
  const hospitals = contexto.hospitales || [];
  const departments = contexto.departamentos || [];
  const units = contexto.unidades || [];
  const hospitalMap = new Map(hospitals.map((item) => [item.codigo, item]));
  const departmentMap = new Map(departments.map((item) => [item.codigo, item]));

  if (area === "Hospital") {
    return sortByUserHospital(hospitals, (item) => item.codigo).map((item) => ({
      value: item.codigo,
      label: item.nombre,
    }));
  }

  if (area === "Departamento") {
    return sortByUserHospital(departments, (item) => item.codHos).map(
      (item) => {
        const hospital = hospitalMap.get(item.codHos);
        return {
          value: item.codigo,
          label: [item.nombre, hospital?.nombre].filter(Boolean).join(" · "),
        };
      },
    );
  }

  return sortByUserHospital(units, (item) => {
    const department = departmentMap.get(item.codDep);
    return department?.codHos;
  }).map((item) => {
    const department = departmentMap.get(item.codDep);
    const hospital = department ? hospitalMap.get(department.codHos) : null;
    return {
      value: item.codigo,
      label: [item.nombre, department?.nombre, hospital?.nombre]
        .filter(Boolean)
        .join(" · "),
    };
  });
}

function populateReporteScopes(preferred) {
  const select = $("#reporteScopeFilter");
  if (!select) return;

  const area = state.reportes.area;
  const options = scopeOptionsFor(area);
  const previous = preferred ?? state.reportes.scope;

  let value = options.some((option) => option.value === previous)
    ? previous
    : "";

  if (!value && area === "Hospital") {
    value = state.contexto?.hospital?.codigo || "";
  }
  if (!value) value = options[0]?.value || "";

  select.innerHTML = options.length
    ? options
        .map(
          (option) =>
            `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`,
        )
        .join("")
    : '<option value="">Sin contextos disponibles</option>';

  select.value = value;
  select.disabled = !options.length;
  state.reportes.scope = value;
}

function reportScopeLabel() {
  const { area, scope } = state.reportes;

  if (!scope) return area;

  if (area === "Hospital") {
    return hospitalByCode(scope)?.nombre || `Hospital ${scope}`;
  }
  if (area === "Departamento") {
    return departamentoByCode(scope)?.nombre || `Departamento ${scope}`;
  }
  return unidadByCode(scope)?.nombre || `Unidad ${scope}`;
}

function resolveReportArgs(def) {
  let area = state.reportes.area;
  let code = state.reportes.scope;
  let note = "";

  // El reporte 9 solo acepta hospital o unidad. Si el ámbito es un
  // departamento se escala al hospital al que pertenece.
  if (def.key === "exitosas" && area === "Departamento") {
    const department = departamentoByCode(code);
    const hospital = department ? hospitalByCode(department.codHos) : null;

    if (hospital) {
      area = "Hospital";
      code = hospital.codigo;
      note = `Este reporte no admite departamentos, así que se muestra el hospital ${hospital.nombre}.`;
    }
  }

  return { area, code, note };
}

/* ============================================================
   Reportes: carga y render
   ============================================================ */
let reportRequestId = 0;

async function loadReportes({ scopedOnly = false, notify = false } = {}) {
  if (!isAdmin()) return;

  const definitions = REPORT_DEFS.filter((def) => !scopedOnly || def.scoped);
  const token = ++reportRequestId;
  state.reportes.loading = true;

  if (!scopedOnly && !state.reportes.loaded) renderReportesLoading();

  const entries = await Promise.all(
    definitions.map(async (def) => {
      if (def.scoped && (!state.reportes.area || !state.reportes.scope)) {
        return {
          key: def.key,
          result: {
            ok: false,
            error:
              "Selecciona un hospital, departamento o unidad para consultar este reporte.",
          },
        };
      }

      const args = resolveReportArgs(def);

      try {
        const response = await def.fetch(args);
        return { key: def.key, result: { ...response, note: args.note } };
      } catch (error) {
        return {
          key: def.key,
          result: {
            ok: false,
            error: error?.message || "No se pudo consultar el reporte.",
          },
        };
      }
    }),
  );

  if (token !== reportRequestId) return;

  entries.forEach(({ key, result }) => {
    state.reportes.results[key] = result;
  });

  state.reportes.loading = false;
  state.reportes.loaded = true;
  state.reportes.lastUpdate = new Date();

  renderReportes();

  if (notify) toast("Reportes actualizados");
}

function renderReportesLoading() {
  const root = $("#reportesList");
  if (!root) return;

  root.innerHTML = `
    <div class="panel report-loading">
      <span class="loading-ring"></span>
      <div>
        <strong>Cargando reportes…</strong>
        <span>Consultando la información hospitalaria.</span>
      </div>
    </div>`;
}

function reportBadge(def, result, rows) {
  if (!result) return "Sin consultar";
  if (!result.ok) return "No disponible";
  return rows.length ? `${formatNumber(rows.length)} registros` : "Sin datos";
}

function reportRows(def, result) {
  if (!result?.ok) return [];
  try {
    return def.rows(result.data) || [];
  } catch (error) {
    console.warn(`No se pudo procesar el reporte ${def.number}:`, error);
    return [];
  }
}

function reportTableHtml(def, rows) {
  if (!rows.length) {
    return '<div class="empty-state">No hay información disponible para este reporte con el ámbito actual.</div>';
  }

  const limit = 8;
  const expanded = !!state.reportes.expanded[def.key];
  const visible = expanded ? rows : rows.slice(0, limit);

  const head = def.columns
    .map((column) => `<th>${escapeHtml(column.label)}</th>`)
    .join("");

  const body = visible
    .map(
      (row) =>
        `<tr>${def.columns
          .map(
            (column) =>
              `<td>${
                column.render
                  ? column.render(row)
                  : escapeHtml(row[column.key] ?? "—")
              }</td>`,
          )
          .join("")}</tr>`,
    )
    .join("");

  const footer =
    rows.length > limit
      ? `<div class="report-expand">
          <span>Mostrando ${visible.length} de ${formatNumber(rows.length)} registros</span>
          <button type="button" class="text-btn" data-report-expand="${def.key}">
            ${expanded ? "Ver menos" : "Ver todos"}
            <i data-lucide="${expanded ? "chevron-up" : "chevron-down"}"></i>
          </button>
        </div>`
      : "";

  return `<div class="table-wrap report-table-wrap">
      <table>
        <thead><tr>${head}</tr></thead>
        <tbody>${body}</tbody>
      </table>
    </div>${footer}`;
}

function reportPanelHtml(def) {
  const result = state.reportes.results[def.key];
  const rows = reportRows(def, result);

  let body;
  if (result && !result.ok) {
    body = `<div class="report-error">${escapeHtml(
      result.error || "No se pudo consultar este reporte.",
    )}</div>`;
  } else {
    body = reportTableHtml(def, rows);
  }

  const note = result?.note
    ? `<p class="report-note">${escapeHtml(result.note)}</p>`
    : "";

  return `<article class="panel report-panel" id="report-panel-${def.key}">
      <div class="report-panel-head">
        <div class="report-panel-title">
          <span class="report-number">${def.number}</span>
          <div>
            <h2>${escapeHtml(def.title)}</h2>
            <p>${escapeHtml(def.description)}</p>
          </div>
        </div>
        <span class="report-badge${def.scoped ? " scoped" : ""}">${reportBadge(
          def,
          result,
          rows,
        )}</span>
      </div>
      ${note}
      ${body}
    </article>`;
}

function renderReporteNav() {
  const nav = $("#reportesNav");
  if (!nav) return;

  nav.innerHTML = REPORT_DEFS.map((def) => {
    const result = state.reportes.results[def.key];
    const count = result?.ok ? reportRows(def, result).length : null;
    return `<button type="button" class="report-chip" data-report-jump="${def.key}">
        <span>${def.number}</span>${escapeHtml(def.short)}${
          count === null ? "" : ` · ${formatNumber(count)}`
        }
      </button>`;
  }).join("");
}

function renderReporteStats() {
  const resumen = state.reportes.results.resumen;
  const hospitals = Array.isArray(resumen?.data) ? resumen.data : [];
  const contexto = state.contexto || {};

  let totalHospitals = hospitals.length || contexto.hospitales?.length || 0;
  let totalMedicos = hospitals.reduce(
    (sum, hospital) => sum + (hospital.cantMed || 0),
    0,
  );
  let totalPacientes = hospitals.reduce(
    (sum, hospital) => sum + (hospital.cantPac || 0),
    0,
  );
  let totalUnidades = hospitals.reduce(
    (sum, hospital) => sum + (hospital.cantUni || 0),
    0,
  );

  if (!totalMedicos) {
    const medicosResult = state.reportes.results.medicos;
    if (medicosResult?.ok) {
      totalMedicos = reportRows(
        REPORT_DEFS.find((def) => def.key === "medicos"),
        medicosResult,
      ).length;
    }
  }

  if (!totalPacientes) {
    const pacientesResult = state.reportes.results.pacientes;
    if (pacientesResult?.ok) {
      totalPacientes = reportRows(
        REPORT_DEFS.find((def) => def.key === "pacientes"),
        pacientesResult,
      ).length;
    }
  }

  if (!totalUnidades) {
    totalUnidades = contexto.unidades?.length || 0;
  }

  setText("reportesHospitalesValue", formatNumber(totalHospitals));
  setText("reportesMedicosValue", formatNumber(totalMedicos));
  setText("reportesPacientesValue", formatNumber(totalPacientes));
  setText("reportesUnidadesValue", formatNumber(totalUnidades));
}

function renderReportes() {
  if (!isAdmin()) return;

  renderReporteStats();
  renderReporteNav();

  const root = $("#reportesList");
  if (root) {
    root.innerHTML = REPORT_DEFS.map(reportPanelHtml).join("");
  }

  const pill = $("#reportesScopePill");
  if (pill) {
    pill.hidden = false;
    pill.textContent = reportScopeLabel();
    pill.title = `${state.reportes.area}: ${reportScopeLabel()}`;
  }

  refreshIcons();
}

function renderReportPanel(key) {
  if (!state.reportes.loaded) return;

  const def = REPORT_DEFS.find((item) => item.key === key);
  const host = document.getElementById(`report-panel-${key}`);
  if (!def || !host) return;

  host.outerHTML = reportPanelHtml(def);
  refreshIcons();
}

function exportReportesCsv() {
  const resumen = state.reportes.results.resumen;
  const hospitals =
    resumen?.ok && Array.isArray(resumen.data) ? resumen.data : [];

  if (!hospitals.length) {
    toast("No hay datos de hospitales para exportar");
    return;
  }

  const header = [
    "Hospital",
    "Departamentos",
    "Unidades",
    "Médicos",
    "Pacientes",
  ];
  const lines = hospitals.map((hospital) => [
    hospital.nombreHos || "",
    hospital.cantDep || 0,
    hospital.cantUni || 0,
    hospital.cantMed || 0,
    hospital.cantPac || 0,
  ]);

  const csv = [header, ...lines]
    .map((row) =>
      row
        .map((cell) => `"${String(cell ?? "").replace(/"/g, '""')}"`)
        .join(","),
    )
    .join("\r\n");

  const blob = new Blob([`\uFEFF${csv}`], {
    type: "text/csv;charset=utf-8;",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `reporte-hospitales-${new Date()
    .toISOString()
    .slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);

  toast("Resumen de hospitales exportado en CSV");
}

/* ============================================================
   Profesionales
   ============================================================ */
async function loadProfesionales(force = false) {
  if (!isAdmin()) return;

  if (state.profesionales && !force) {
    renderProfesionales();
    return;
  }
  if (state.profesionalesLoading) return;

  state.profesionalesLoading = true;
  const response = await api.profesionalesHospital();
  state.profesionalesLoading = false;

  if (response.ok && response.data) {
    state.profesionales = response.data;
  } else {
    state.profesionales = {
      hospital: state.contexto?.hospital || null,
      alcance: state.contexto?.hospital ? "hospital" : "global",
      medicos: [],
      error: response.error || "No se pudo cargar el directorio médico.",
    };
  }

  renderProfesionales();
}

function filteredProfesionales() {
  const search = slug(state.profesionalFilters.search.trim());
  const specialty = state.profesionalFilters.specialty;
  const doctors = state.profesionales?.medicos || [];

  return doctors.filter((doctor) => {
    const haystack = slug(
      `${doctor.nombre} ${doctor.especialidad} ${doctor.licencia} ${doctor.hospital} ${doctor.departamento} ${doctor.unidad} ${doctor.email}`,
    );
    const matchesSearch = !search || haystack.includes(search);
    const matchesSpecialty =
      specialty === "all" || doctor.especialidad === specialty;
    return matchesSearch && matchesSpecialty;
  });
}

function renderProfesionalesGrid() {
  const root = $("#profesionalesGrid");
  if (!root) return;

  const doctors = state.profesionales?.medicos || [];
  const filtered = filteredProfesionales();

  setText(
    "profesionalesCount",
    `${formatNumber(filtered.length)} de ${formatNumber(doctors.length)} profesionales`,
  );

  if (!filtered.length) {
    root.innerHTML =
      '<div class="empty-state">No encontramos profesionales con esos filtros.</div>';
    return;
  }

  root.innerHTML = filtered
    .map(
      (doctor, index) => `
        <article class="doctor-card">
          <div class="doctor-card-head">
            <span class="doctor-avatar" style="${avatarStyle(index)}">${escapeHtml(initialsOf(doctor.nombre))}</span>
            <div class="doctor-identity">
              <strong>${escapeHtml(doctor.nombre || "Profesional")}</strong>
              <span>${escapeHtml(doctor.especialidad || "Sin especialidad")}</span>
            </div>
          </div>
          <div class="doctor-meta">
            <div class="doctor-meta-row">
              <i data-lucide="building-2"></i>
              <span>${escapeHtml(
                [doctor.hospital, doctor.departamento]
                  .filter(Boolean)
                  .join(" · ") || "Sin hospital asignado",
              )}</span>
            </div>
            <div class="doctor-meta-row">
              <i data-lucide="map-pin"></i>
              <span>${escapeHtml(doctor.unidad || "Sin unidad asignada")}</span>
            </div>
            <div class="doctor-meta-row private">
              <i data-lucide="phone"></i>
              <span>${escapeHtml(doctor.telefono || "Sin teléfono registrado")}</span>
            </div>
            ${
              doctor.email
                ? `<div class="doctor-meta-row private"><i data-lucide="mail"></i><span>${escapeHtml(doctor.email)}</span></div>`
                : ""
            }
          </div>
          <div class="doctor-card-foot">
            <span class="doctor-experience">${formatNumber(doctor.experiencia || 0)} años de experiencia</span>
            <span class="doctor-license">${escapeHtml(doctor.licencia || "Sin licencia")}</span>
          </div>
        </article>`,
    )
    .join("");

  refreshIcons();
}

function renderProfesionales() {
  const data = state.profesionales || {};
  const doctors = Array.isArray(data.medicos) ? data.medicos : [];
  const hospital = data.hospital;
  const alcance = data.alcance || "global";

  const departments = new Set(
    doctors.map((doctor) => doctor.departamento).filter(Boolean),
  );
  const units = new Set(doctors.map((doctor) => doctor.unidad).filter(Boolean));
  const specialties = [
    ...new Set(doctors.map((doctor) => doctor.especialidad).filter(Boolean)),
  ].sort((a, b) => a.localeCompare(b, "es"));

  setText("profesionalesTotalValue", formatNumber(doctors.length));
  setText("profesionalesEspecialidadesValue", formatNumber(specialties.length));
  setText("profesionalesUnidadesValue", formatNumber(units.size));

  const averageExperience = doctors.length
    ? Math.round(
        doctors.reduce(
          (sum, doctor) => sum + Number(doctor.experiencia || 0),
          0,
        ) / doctors.length,
      )
    : 0;

  const experience = $("#profesionalesExperienciaValue");
  if (experience) {
    experience.innerHTML = `${formatNumber(averageExperience)} <small>años</small>`;
  }

  const pill = $("#profesionalesScopePill");
  if (pill) {
    pill.hidden = false;
    pill.textContent = alcance === "hospital" ? "Mi hospital" : "Vista global";
  }

  setText(
    "profesionalesHospitalName",
    hospital?.nombre || "Red hospitalaria completa",
  );

  setText(
    "profesionalesHospitalMeta",
    data.error
      ? data.error
      : doctors.length
        ? `${formatNumber(doctors.length)} médicos · ${formatNumber(
            departments.size,
          )} departamentos · ${formatNumber(units.size)} unidades`
        : "No hay médicos registrados en el contexto actual.",
  );

  const chip = $("#profesionalesScopeChip");
  if (chip) {
    chip.textContent =
      alcance === "hospital" ? "Hospital de la sesión" : "Vista global";
    chip.className = `status-chip ${
      alcance === "hospital" ? "confirmed" : "pending"
    }`;
  }

  const select = $("#profesionalesSpecialtyFilter");
  if (select) {
    const current = state.profesionalFilters.specialty;
    select.innerHTML = [
      '<option value="all">Todas las especialidades</option>',
      ...specialties.map(
        (specialty) =>
          `<option value="${escapeHtml(specialty)}">${escapeHtml(specialty)}</option>`,
      ),
    ].join("");
    select.value = specialties.includes(current) ? current : "all";
    state.profesionalFilters.specialty = select.value;
  }

  renderProfesionalesGrid();
}

/* ============================================================
   Configuración
   ============================================================ */
function loadSettings() {
  let stored = null;

  try {
    stored = JSON.parse(localStorage.getItem(SETTINGS_KEY) || "null");
  } catch (error) {
    console.warn("No se pudieron leer las preferencias guardadas:", error);
  }

  state.settings = {
    ...DEFAULT_SETTINGS,
    ...(stored && typeof stored === "object" ? stored : {}),
  };
}

function applySettings() {
  const settings = state.settings || DEFAULT_SETTINGS;

  document.body.classList.toggle(
    "compact-mode",
    settings.density === "compact",
  );
  document.body.classList.toggle("hide-help-card", !settings.helpCard);
  document.body.classList.toggle(
    "reduce-motion",
    settings.animations === "off",
  );
  document.body.classList.toggle("privacy-mode", Boolean(settings.privacy));

  const accent = ACCENTS[settings.accent] || ACCENTS.teal;
  document.body.style.setProperty("--teal", accent.teal);
  document.body.style.setProperty("--teal-dark", accent.dark);

  renderFecha();
}

function persistSettings() {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settings));
  } catch (error) {
    console.warn("No se pudieron guardar las preferencias:", error);
  }
}

function readSettingsControls() {
  const settings = state.settings || DEFAULT_SETTINGS;
  const value = (id, fallback) =>
    document.getElementById(id)?.value ?? fallback;
  const checked = (id, fallback) => {
    const element = document.getElementById(id);
    return element ? element.checked : fallback;
  };

  state.settings = {
    density: value("prefDensity", settings.density),
    accent: value("prefAccent", settings.accent),
    dateFormat: value("prefDateFormat", settings.dateFormat),
    animations: value("prefAnimations", settings.animations),
    notifications: checked("prefNotifications", settings.notifications),
    emailDigest: checked("prefEmailDigest", settings.emailDigest),
    helpCard: checked("prefHelpCard", settings.helpCard),
    privacy: checked("prefPrivacy", settings.privacy),
  };
}

function setSettingsStatus(message, saved = false) {
  const element = $("#settingsStatus");
  if (!element) return;
  element.textContent = message;
  element.classList.toggle("saved", saved);
}

function renderSettings() {
  const user = state.user || {};
  const name = user.nombre || user.username || "Usuario";
  const settings = state.settings || DEFAULT_SETTINGS;

  setText("settingsAvatar", initialsOf(name));
  setText("settingsUserName", name);
  setText("settingsUserEmail", user.email || "Sin correo registrado");
  setText("settingsUsername", `@${user.username || "usuario"}`);
  setText("settingsSpecialty", user.especialidad || "Sin especificar");
  setText("settingsPhone", user.telefono || "Sin registrar");
  setText("settingsAddress", user.direccion || "Sin registrar");
  setText(
    "settingsHospital",
    state.contexto?.hospital?.nombre || "Red hospitalaria global",
  );

  setText("settingsSessionRole", isAdmin() ? "Administrador" : "Médico");
  setText("settingsSessionUser", user.username || "—");
  setText("settingsSessionEmail", user.email || "—");
  setText("settingsLastUpdate", "Ahora");

  const role = $("#settingsUserRole");
  if (role) {
    role.textContent = isAdmin() ? "Administrador" : "Médico";
    role.className = `status-chip ${isAdmin() ? "confirmed" : "pending"}`;
  }

  const setSelect = (id, value) => {
    const element = document.getElementById(id);
    if (element) element.value = value;
  };
  const setCheckbox = (id, value) => {
    const element = document.getElementById(id);
    if (element) element.checked = Boolean(value);
  };

  setSelect("prefDensity", settings.density);
  setSelect("prefAccent", settings.accent);
  setSelect("prefDateFormat", settings.dateFormat);
  setSelect("prefAnimations", settings.animations);
  setCheckbox("prefNotifications", settings.notifications);
  setCheckbox("prefEmailDigest", settings.emailDigest);
  setCheckbox("prefHelpCard", settings.helpCard);
  setCheckbox("prefPrivacy", settings.privacy);
}

function saveSettings() {
  readSettingsControls();
  persistSettings();
  applySettings();
  setSettingsStatus("Preferencias guardadas en este navegador.", true);
  toast("Preferencias guardadas");
}

function resetSettings() {
  state.settings = { ...DEFAULT_SETTINGS };

  try {
    localStorage.removeItem(SETTINGS_KEY);
  } catch (error) {
    console.warn("No se pudieron restablecer las preferencias:", error);
  }

  applySettings();
  renderSettings();
  setSettingsStatus("Preferencias restablecidas.", true);
  toast("Preferencias restablecidas");
}

/* ============================================================
   Centro de ayuda
   ============================================================ */
function applyHelpFilters() {
  const query = slug(state.help.search.trim());
  const tag = slug(state.help.tag);
  let visible = 0;

  const matches = (element) => {
    const keywords = slug(element.dataset.helpText || "");
    const haystack = slug(`${keywords} ${element.textContent || ""}`);
    const matchesQuery = !query || haystack.includes(query);
    const matchesTag = !tag || keywords.includes(tag);
    return matchesQuery && matchesTag;
  };

  $$("#view-Ayuda .guide-card").forEach((card) => {
    const show = matches(card);
    card.hidden = !show;
    if (show) visible += 1;
  });

  $$("#view-Ayuda .faq-item").forEach((item) => {
    const show = matches(item);
    item.hidden = !show;
    if (show) visible += 1;
  });

  const empty = $("#helpEmpty");
  if (empty) empty.hidden = visible > 0;

  $$("#view-Ayuda [data-help-tag]").forEach((button) => {
    button.classList.toggle(
      "active",
      slug(button.dataset.helpTag) === state.help.tag,
    );
  });
}

function setHelpTag(tag) {
  state.help.tag = state.help.tag === tag ? "" : tag;
  applyHelpFilters();
}

async function loadAyudaStatus() {
  const element = $("#helpApiStatus");
  if (!element) return;

  element.textContent = "Verificando conexión…";
  const response = await api.me();

  if (response.ok && response.user) {
    element.textContent = `Conectado como ${response.user.email || response.user.username}`;
  } else if (response.status === 401) {
    element.textContent = "Sesión no iniciada";
  } else {
    element.textContent = "Backend no disponible · modo local";
  }
}

/* ============================================================
   Modales
   ============================================================ */
async function openModal() {
  const backdrop = $("#modalBackdrop");
  if (!backdrop) return;
  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  await loadCatalogos(true);
  populateTurnoForm();
  refreshIcons();
  $("#turnoPaciente")?.focus();
}

function closeModal() {
  const backdrop = $("#modalBackdrop");
  if (!backdrop) return;
  backdrop.classList.remove("open");
  backdrop.setAttribute("aria-hidden", "true");
}

async function openPatientModal() {
  const backdrop = $("#patientModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  await loadCatalogos(true);
  populatePacienteForm();
  refreshIcons();
  $("#patientForm input[name='nombre']")?.focus();
}

function closePatientModal() {
  const backdrop = $("#patientModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.remove("open");
  backdrop.setAttribute("aria-hidden", "true");
}

async function openMedicoModal() {
  if (!isAdmin()) {
    toast("Solo un administrador puede registrar médicos.");
    return;
  }
  const backdrop = $("#medicoModalBackdrop");
  if (!backdrop) return;
  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");
  await loadCatalogos(true);
  populateMedicoForm();
  refreshIcons();
  $("#medicoForm input[name='nombre']")?.focus();
}

function closeMedicoModal() {
  const backdrop = $("#medicoModalBackdrop");
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

  // El perfil son los datos del médico vinculado. Un administrador sin
  // médico no tiene campos profesionales que editar.
  const tieneMedico = Boolean(user.codMed);
  const campos = $("#profileProfessionalFields");
  const aviso = $("#profileNoMedico");
  const guardar = $("#profileSubmit");
  if (campos) campos.hidden = !tieneMedico;
  if (aviso) aviso.hidden = tieneMedico;
  if (guardar) guardar.hidden = !tieneMedico;

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
  $("#turnoMedico")?.addEventListener("change", sincronizarUnidadTurno);

  $("#appointmentForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = $("#appointmentSubmit");
    const payload = {
      pacienteId: form.elements.pacienteId.value,
      codMed: form.elements.codMed.value,
      codUni: form.elements.codUni.value,
      estado: form.elements.estado.value,
      nota: form.elements.nota.value.trim(),
    };
    if (!payload.pacienteId || !payload.codMed) {
      toast("Selecciona un paciente y un profesional para continuar.");
      return;
    }

    if (submit) submit.disabled = true;
    const response = await api.crearTurno(payload);
    if (submit) submit.disabled = false;

    if (!response.ok) {
      toast(response.error || "No se pudo crear el turno.");
      return;
    }

    closeModal();
    form.reset();
    renderFecha();
    await loadDashboardData();
    refreshIcons();
    toast(`Turno ${response.data?.id || ""} creado correctamente`);
  });

  $("#newPacienteBtn")?.addEventListener("click", openPatientModal);
  $("#closePatientModal")?.addEventListener("click", closePatientModal);
  $("#cancelPatientModal")?.addEventListener("click", closePatientModal);
  $("#patientModalBackdrop")?.addEventListener("click", (event) => {
    if (event.target.id === "patientModalBackdrop") closePatientModal();
  });

  $("#patientForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = $("#patientSubmit");
    const payload = {
      nombre: form.elements.nombre.value.trim(),
      documento: form.elements.documento.value.trim(),
      fechaNacimiento: form.elements.fechaNacimiento.value,
      direccion: form.elements.direccion.value.trim(),
      estado: form.elements.estado.value,
      codUni: form.elements.codUni.value,
    };

    if (submit) submit.disabled = true;
    const response = await api.crearPaciente(payload);
    if (submit) submit.disabled = false;

    if (!response.ok) {
      toast(response.error || "No se pudo registrar el paciente.");
      return;
    }

    closePatientModal();
    form.reset();
    await loadDashboardData();
    refreshIcons();
    toast(`${response.data?.nombre || "Paciente"} registrado correctamente`);
  });

  $("#newMedicoBtn")?.addEventListener("click", openMedicoModal);
  $("#closeMedicoModal")?.addEventListener("click", closeMedicoModal);
  $("#cancelMedicoModal")?.addEventListener("click", closeMedicoModal);
  $("#medicoModalBackdrop")?.addEventListener("click", (event) => {
    if (event.target.id === "medicoModalBackdrop") closeMedicoModal();
  });
  $("#medicoHospital")?.addEventListener("change", populateMedicoForm);
  $("#medicoDepartamento")?.addEventListener("change", populateMedicoForm);

  $("#medicoForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAdmin()) {
      toast("Solo un administrador puede registrar médicos.");
      return;
    }

    const form = event.currentTarget;
    const submit = $("#medicoSubmit");
    const payload = {
      codMed: form.elements.codMed.value.trim(),
      nombre: form.elements.nombre.value.trim(),
      especialidad: form.elements.especialidad.value.trim(),
      licencia: form.elements.licencia.value.trim(),
      telefono: form.elements.telefono.value.trim(),
      experiencia: form.elements.experiencia.value,
      datosContacto: form.elements.datosContacto.value.trim(),
      direccion: form.elements.direccion.value.trim(),
      codUni: form.elements.codUni.value,
    };

    if (submit) submit.disabled = true;
    const response = await api.crearMedico(payload);
    if (submit) submit.disabled = false;

    if (!response.ok) {
      toast(response.error || "No se pudo registrar el médico.");
      return;
    }

    closeMedicoModal();
    form.reset();
    await Promise.all([loadCatalogos(true), loadProfesionales(true)]);
    refreshIcons();
    toast(`Médico ${response.data?.codMed || ""} registrado correctamente`);
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeModal();
      closePatientModal();
      closeMedicoModal();
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
    state.pagina.turnos = 0;
    renderTurnoTimeline();
    renderTurnosTable();
  });

  $("#turnoSpecialtyFilter")?.addEventListener("change", (event) => {
    state.turnoFilters.specialty = event.target.value;
    state.pagina.turnos = 0;
    renderTurnoTimeline();
    renderTurnosTable();
  });

  $("#turnoStatusFilter")?.addEventListener("change", (event) => {
    state.turnoFilters.status = event.target.value;
    state.pagina.turnos = 0;
    renderTurnosTable();
  });

  $("#pacientesSearch")?.addEventListener("input", (event) => {
    state.pacienteFilters.search = event.target.value;
    state.pagina.pacientes = 0;
    renderPacientesTable();
  });

  $("#pacientesEstadoFilter")?.addEventListener("change", (event) => {
    state.pacienteFilters.status = event.target.value;
    state.pagina.pacientes = 0;
    renderPacientesTable();
  });

  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-more]");
    if (!button) return;
    const clave = button.dataset.more;
    if (clave === "turnos") {
      state.pagina.turnos += 1;
      renderTurnosTable();
    } else if (clave === "pacientes") {
      state.pagina.pacientes += 1;
      renderPacientesTable();
    }
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

  $(".help-card")?.addEventListener("click", () => navigate("Ayuda"));

  /* ------------------------------ Profesionales ----------------------------- */
  $("#refreshProfesionales")?.addEventListener("click", async () => {
    await loadProfesionales(true);
    toast("Directorio de profesionales actualizado");
  });

  $("#profesionalesSearch")?.addEventListener("input", (event) => {
    state.profesionalFilters.search = event.target.value;
    renderProfesionalesGrid();
  });

  $("#profesionalesSpecialtyFilter")?.addEventListener("change", (event) => {
    state.profesionalFilters.specialty = event.target.value;
    renderProfesionalesGrid();
  });

  /* --------------------------------- Reportes -------------------------------- */
  $("#refreshReportes")?.addEventListener("click", () => {
    loadReportes({ notify: true });
  });

  $("#exportReportes")?.addEventListener("click", exportReportesCsv);

  $("#reporteAreaFilter")?.addEventListener("change", (event) => {
    state.reportes.area = event.target.value;
    state.reportes.expanded = {};
    populateReporteScopes("");
    loadReportes({ scopedOnly: true });
  });

  $("#reporteScopeFilter")?.addEventListener("change", (event) => {
    state.reportes.scope = event.target.value;
    state.reportes.expanded = {};
    loadReportes({ scopedOnly: true });
  });

  $("#reportesNav")?.addEventListener("click", (event) => {
    const chip = event.target.closest("[data-report-jump]");
    if (!chip) return;
    const panel = document.getElementById(
      `report-panel-${chip.dataset.reportJump}`,
    );
    panel?.scrollIntoView({
      behavior: state.settings?.animations === "off" ? "auto" : "smooth",
      block: "start",
    });
  });

  $("#reportesList")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-report-expand]");
    if (!button) return;
    const key = button.dataset.reportExpand;
    state.reportes.expanded[key] = !state.reportes.expanded[key];
    renderReportPanel(key);
  });

  /* -------------------------------- Configuración ---------------------------- */
  [
    "#prefDensity",
    "#prefAccent",
    "#prefDateFormat",
    "#prefAnimations",
    "#prefNotifications",
    "#prefEmailDigest",
    "#prefHelpCard",
    "#prefPrivacy",
  ].forEach((selector) => {
    $(selector)?.addEventListener("change", () => {
      readSettingsControls();
      applySettings();
      setSettingsStatus("Cambios sin guardar.", false);
    });
  });

  $("#settingsSave")?.addEventListener("click", saveSettings);
  $("#settingsReset")?.addEventListener("click", resetSettings);
  $("#settingsEditProfile")?.addEventListener("click", openProfileModal);
  $("#settingsLogout")?.addEventListener("click", logout);

  /* -------------------------------- Centro de ayuda -------------------------- */
  $("#helpSearch")?.addEventListener("input", (event) => {
    state.help.search = event.target.value;
    applyHelpFilters();
  });

  $$("#view-Ayuda [data-help-tag]").forEach((button) => {
    button.addEventListener("click", () => setHelpTag(button.dataset.helpTag));
  });

  $("#helpContactShortcut")?.addEventListener("click", () => {
    toast("Abriendo un canal de contacto con soporte");
  });

  $("#helpChatButton")?.addEventListener("click", () => {
    toast("El chat interno estará disponible en la próxima actualización");
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
  loadSettings();
  applySettings();
  refreshIcons();
  bindEvents();

  const sessionReady = await initSession();
  if (!sessionReady) return;

  await loadContexto();
  await loadCatalogos();
  renderSettings();

  const requested = decodeURIComponent(window.location.hash.slice(1));
  const initial =
    requested && (!ADMIN_ONLY.has(requested) || isAdmin())
      ? requested
      : "Resumen";

  navigate(initial, false);
  await loadDashboardData();
  refreshIcons();
});
