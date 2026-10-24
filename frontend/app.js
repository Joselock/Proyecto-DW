const appointments = [
  {
    time: "08:30",
    patient: "Elena Vargas",
    id: "HC-20491",
    specialty: "Cardiología",
    key: "cardiologia",
    doctor: "Dra. Valentina Cruz",
    status: "done",
    statusLabel: "Atendido",
  },
  {
    time: "09:15",
    patient: "Luis Fernández",
    id: "HC-18802",
    specialty: "Dermatología",
    key: "dermatologia",
    doctor: "Dr. Pablo Méndez",
    status: "done",
    statusLabel: "Atendido",
  },
  {
    time: "10:00",
    patient: "Mariana Torres",
    id: "HC-21038",
    specialty: "Pediatría",
    key: "pediatria",
    doctor: "Dra. Sofía Núñez",
    status: "confirmed",
    statusLabel: "Confirmado",
  },
  {
    time: "11:30",
    patient: "Roberto Suárez",
    id: "HC-19876",
    specialty: "Traumatología",
    key: "traumatologia",
    doctor: "Dr. Andrés León",
    status: "confirmed",
    statusLabel: "Confirmado",
  },
  {
    time: "13:00",
    patient: "Claudia Pérez",
    id: "HC-22014",
    specialty: "Cardiología",
    key: "cardiologia",
    doctor: "Dra. Valentina Cruz",
    status: "pending",
    statusLabel: "Pendiente",
  },
  {
    time: "15:30",
    patient: "Jorge Molina",
    id: "HC-22051",
    specialty: "Dermatología",
    key: "dermatologia",
    doctor: "Dr. Pablo Méndez",
    status: "confirmed",
    statusLabel: "Confirmado",
  },
  {
    time: "16:00",
    patient: "Ana Beltrán",
    id: "HC-22103",
    specialty: "Pediatría",
    key: "pediatria",
    doctor: "Dra. Sofía Núñez",
    status: "pending",
    statusLabel: "Pendiente",
  },
];
const queue = [
  {
    name: "Sara Gómez",
    detail: "Cardiología · Turno 10:45",
    initials: "SG",
    cls: "peach",
    time: "12 min",
  },
  {
    name: "Daniel Rojas",
    detail: "Pediatría · Turno 11:00",
    initials: "DR",
    cls: "lavender",
    time: "8 min",
  },
  {
    name: "Camila Santos",
    detail: "Dermatología · Turno 11:15",
    initials: "CS",
    cls: "mint",
    time: "5 min",
  },
  {
    name: "Nicolás Pérez",
    detail: "Traumatología · Turno 11:30",
    initials: "NP",
    cls: "sky",
    time: "En consulta",
  },
];
const patients = [
  {
    name: "Elena Vargas",
    id: "HC-20491",
    initials: "EV",
    cls: "peach",
    visit: "Hoy, 08:30",
    specialty: "Cardiología",
    doctor: "Dra. Valentina Cruz",
    status: "Atendido",
    statusClass: "done",
  },
  {
    name: "Mariana Torres",
    id: "HC-21038",
    initials: "MT",
    cls: "lavender",
    visit: "Hoy, 10:00",
    specialty: "Pediatría",
    doctor: "Dra. Sofía Núñez",
    status: "Confirmado",
    statusClass: "confirmed",
  },
  {
    name: "Roberto Suárez",
    id: "HC-19876",
    initials: "RS",
    cls: "mint",
    visit: "Hoy, 11:30",
    specialty: "Traumatología",
    doctor: "Dr. Andrés León",
    status: "Confirmado",
    statusClass: "confirmed",
  },
  {
    name: "Claudia Pérez",
    id: "HC-22014",
    initials: "CP",
    cls: "sky",
    visit: "Ayer, 16:15",
    specialty: "Cardiología",
    doctor: "Dra. Valentina Cruz",
    status: "Pendiente",
    statusClass: "pending",
  },
  {
    name: "Jorge Molina",
    id: "HC-22051",
    initials: "JM",
    cls: "sand",
    visit: "Ayer, 14:00",
    specialty: "Dermatología",
    doctor: "Dr. Pablo Méndez",
    status: "Atendido",
    statusClass: "done",
  },
];

const initialsClass = {
  peach: "#f3c3a8",
  lavender: "#cfc8ec",
  mint: "#a9ded3",
  sky: "#b8d6ed",
  sand: "#edd4a7",
};
const refreshIcons = () => window.lucide?.createIcons();
function renderTimeline(filter = "all") {
  const items = appointments.filter(
    (a) => filter === "all" || a.key === filter,
  );
  const root = document.querySelector("#timeline");
  if (!items.length) {
    root.innerHTML =
      '<div class="empty-state">No hay turnos para esta especialidad.</div>';
    return;
  }
  root.innerHTML = items
    .map(
      (a) =>
        `<div class="time-row"><div class="time-label">${a.time}</div><div class="appointment-item ${a.status === "done" ? "past" : ""}"><div class="appointment-main"><strong>${a.patient}</strong><span>${a.specialty} · ${a.id}</span></div><div class="appointment-meta"><span class="doctor-mini">${a.doctor}</span><span class="status-chip ${a.status}">${a.statusLabel}</span></div></div></div>`,
    )
    .join("");
}
function renderQueue() {
  document.querySelector("#queueList").innerHTML = queue
    .map(
      (q, i) =>
        `<div class="queue-item"><span class="queue-num">0${i + 1}</span><span class="patient-avatar" style="background:${initialsClass[q.cls]};color:#536677">${q.initials}</span><div class="patient-info"><strong>${q.name}</strong><span>${q.detail}</span></div><span class="${q.time === "En consulta" ? "consulting" : "wait-time"}">${q.time}</span></div>`,
    )
    .join("");
}
function renderPatients(query = "") {
  const filtered = patients.filter((p) =>
    `${p.name} ${p.id} ${p.specialty}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  const root = document.querySelector("#patientsTable");
  root.innerHTML = filtered.length
    ? filtered
        .map(
          (p) =>
            `<tr><td><div class="table-patient"><span class="patient-avatar" style="background:${initialsClass[p.cls]};color:#536677">${p.initials}</span><div><strong>${p.name}</strong><span>${p.id}</span></div></div></td><td>${p.visit}</td><td>${p.specialty}</td><td>${p.doctor}</td><td><span class="status-chip ${p.statusClass}">${p.status}</span></td><td><div class="table-actions"><button class="icon-btn" title="Ver ficha de ${p.name}"><i data-lucide="more-horizontal"></i></button></div></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="6"><div class="empty-state">No encontramos pacientes con esa búsqueda.</div></td></tr>';
  refreshIcons();
}
function openModal() {
  document.querySelector("#modalBackdrop").classList.add("open");
  document.querySelector("#modalBackdrop").setAttribute("aria-hidden", "false");
  document.querySelector('input[name="patient"]').focus();
}
function closeModal() {
  document.querySelector("#modalBackdrop").classList.remove("open");
  document.querySelector("#modalBackdrop").setAttribute("aria-hidden", "true");
}
function toast(message) {
  const t = document.querySelector("#toast");
  t.querySelector("span").textContent = message;
  t.classList.add("show");
  setTimeout(() => t.classList.remove("show"), 3000);
}

document.addEventListener("DOMContentLoaded", () => {
  refreshIcons();
  renderTimeline();
  renderQueue();
  renderPatients();
  document
    .querySelector("#specialtyFilter")
    .addEventListener("change", (e) => renderTimeline(e.target.value));
  document
    .querySelector("#patientSearch")
    .addEventListener("input", (e) => renderPatients(e.target.value));
  document
    .querySelector("#newAppointment")
    .addEventListener("click", openModal);
  document.querySelector("#closeModal").addEventListener("click", closeModal);
  document.querySelector("#cancelModal").addEventListener("click", closeModal);
  document.querySelector("#modalBackdrop").addEventListener("click", (e) => {
    if (e.target.id === "modalBackdrop") closeModal();
  });
  document.querySelector("#appointmentForm").addEventListener("submit", (e) => {
    e.preventDefault();
    closeModal();
    toast("Turno creado correctamente");
  });
  document
    .querySelector("#exportBtn")
    .addEventListener("click", () => toast("Reporte preparado para descargar"));
  document
    .querySelector("#manageQueue")
    .addEventListener("click", () => toast("Cola actualizada en tiempo real"));
  ["#viewCalendar", "#viewPatients"].forEach((sel) =>
    document
      .querySelector(sel)
      .addEventListener("click", () =>
        toast("Vista completa disponible próximamente"),
      ),
  );
  document.querySelectorAll(".nav-item").forEach((btn) =>
    btn.addEventListener("click", () => {
      document
        .querySelectorAll(".nav-item")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      document.querySelector(".breadcrumb strong").textContent =
        btn.dataset.section;
      if (window.innerWidth < 701)
        document.querySelector("#sidebar").classList.remove("open");
    }),
  );
  document
    .querySelector("#mobileMenu")
    .addEventListener("click", () =>
      document.querySelector("#sidebar").classList.toggle("open"),
    );
  document
    .querySelector("#prevDay")
    .addEventListener("click", () => toast("Mostrando el día anterior"));
  document
    .querySelector("#nextDay")
    .addEventListener("click", () => toast("Mostrando el día siguiente"));
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeModal();
  });
});

//==================================================
//FECHA ACTUAL======================================

function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function obtenerFecha(fecha = new Date()) {
  const texto = new Intl.DateTimeFormat("es-Es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(fecha);

  return capitalizar(texto);
}

function renderFecha() {
  ["fechahoy", "agendaDate"].forEach((id) => {
    const fe = document.getElementById(id);
    if (fe) fe.textContent = obtenerFecha();
  });
  fe.setAttribute("datetime", new Date().toISOString().slice(0, 10));
}
renderFecha();
