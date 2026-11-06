const appointments = [];
const queue = [];
const patients = [];

async function loadDashboardData() {
  try {
    const res = await fetch(
      "http://localhost:8080/api/reportes/hospitales/resumen",
      {
        credentials: "include",
      },
    );
    const data = await res.json();
    if (!data || !data.ok || !Array.isArray(data.data)) {
      throw new Error("No se pudo cargar el resumen");
    }

    const hospitals = data.data.slice(0, 4);
    const cards = [
      {
        label: "Turnos de hoy",
        value:
          hospitals.reduce(
            (sum, hospital) => sum + (hospital.cantPac || 0),
            0,
          ) || 48,
      },
      {
        label: "Pacientes atendidos",
        value:
          hospitals.reduce(
            (sum, hospital) => sum + (hospital.cantPac || 0),
            0,
          ) || 36,
      },
      { label: "Espera promedio", value: "14 min" },
      {
        label: "Pacientes registrados",
        value:
          hospitals.reduce(
            (sum, hospital) => sum + (hospital.cantPac || 0),
            0,
          ) || 1284,
      },
    ];

    cards.forEach((card, idx) => {
      const stat = document.querySelectorAll(".stat-card")[idx];
      if (!stat) return;
      const value = stat.querySelector(".stat-value");
      const label = stat.querySelector(".stat-label");
      if (value)
        value.innerHTML =
          typeof card.value === "string"
            ? card.value
            : Number(card.value).toLocaleString("es-ES");
      if (label) label.textContent = card.label;
    });

    const patientRows = [];
    for (const hospital of hospitals) {
      if (!hospital.departamentos) continue;
      for (const dep of hospital.departamentos) {
        if (!dep.unidades) continue;
        for (const unidad of dep.unidades) {
          if (!unidad.registro || !Array.isArray(unidad.registro.pacientes))
            continue;
          for (const paciente of unidad.registro.pacientes) {
            patientRows.push({
              name: paciente.nombrePac || "Paciente",
              id: paciente.numHisCli || "--",
              initials:
                (paciente.nombrePac || "P")
                  .split(" ")
                  .slice(0, 2)
                  .map((p) => p[0])
                  .join("")
                  .toUpperCase() || "P",
              cls: ["peach", "lavender", "mint", "sky", "sand"][
                patientRows.length % 5
              ],
              visit: "Hoy",
              specialty: dep.nombreDep || "General",
              doctor: "Equipo médico",
              status: "En consulta",
              statusClass: "confirmed",
            });
          }
        }
      }
    }

    if (patientRows.length) {
      patients.splice(0, patients.length, ...patientRows.slice(0, 5));
      renderPatients();
    }
  } catch (error) {
    console.warn(
      "No se pudo cargar la data del backend, se conserva el dashboard demo:",
      error,
    );
  }
}

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

document.addEventListener("DOMContentLoaded", async () => {
  refreshIcons();
  renderTimeline();
  renderQueue();
  renderPatients();
  await loadDashboardData();
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
    if (fe) {
      fe.textContent = obtenerFecha();
      fe.setAttribute("datetime", new Date().toISOString().slice(0, 10));
    }
  });
}
renderFecha();
