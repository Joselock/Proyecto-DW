import { api } from "./api.js";

document.addEventListener("DOMContentLoaded", () => {
  const app = document.getElementById("app");
  if (!app) return;

  // ---------------------------------------------------------
  // Cambio de estado global
  // ---------------------------------------------------------
  /*const setState = (state) => {
    app.dataset.state = state;
  };*/
  const setState = (state) => {
    const prev = app.dataset.state;
    app.dataset.state = state;

    if (prev === "home" && (state === "login" || state === "register")) {
      const auth = document.getElementById("auth");
      if (!auth) return;
      auth.classList.remove("auth-entering");
      void auth.offsetWidth;
      auth.classList.add("auth-entering");
      clearTimeout(auth._enterTimer);
      auth._enterTimer = setTimeout(
        () => auth.classList.remove("auth-entering"),
        800,
      );
    }
  };

  // Botones desde HOME
  document
    .getElementById("btnGoLogin")
    ?.addEventListener("click", () => setState("login"));
  document
    .getElementById("btnGoRegister")
    ?.addEventListener("click", () => setState("register"));

  // Botones de cambio login <-> register
  document
    .getElementById("btnSwitchToRegister")
    ?.addEventListener("click", () => setState("register"));
  document
    .getElementById("btnSwitchToLogin")
    ?.addEventListener("click", () => setState("login"));

  // ---------------------------------------------------------
  // Toggle mostrar / ocultar contraseña
  // ---------------------------------------------------------
  document.querySelectorAll(".toggle-pass").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = btn.parentElement.querySelector("input");
      const icon = btn.querySelector("i");
      if (!input || !icon) return;

      const show = input.type === "password";
      input.type = show ? "text" : "password";

      icon.classList.toggle("bi-eye", !show);
      icon.classList.toggle("bi-eye-slash", show);

      btn.setAttribute("aria-pressed", show);
      btn.setAttribute(
        "aria-label",
        show ? "Ocultar contraseña" : "Mostrar contraseña",
      );
    });
  });

  // ---------------------------------------------------------
  // LOGIN
  // ---------------------------------------------------------
  const loginForm = document.getElementById("loginForm");
  const loginError = document.getElementById("loginError");
  const loginSubmit = document.getElementById("loginSubmit");

  loginForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    loginError.textContent = "";

    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPass").value;

    if (!email || !password) {
      loginError.textContent = "Completa todos los campos.";
      return;
    }

    loginSubmit.disabled = true;
    loginSubmit.textContent = "Entrando...";

    try {
      const res = await api.login(email, password);
      if (!res.ok) {
        loginError.textContent = res.error || "Credenciales incorrectas.";
        return;
      }
      window.location.href = "./dashboard.html";
    } catch (error) {
      loginError.textContent = "No se pudo iniciar sesión. Inténtalo de nuevo.";
      console.error("Error al iniciar sesión:", error);
    } finally {
      loginSubmit.disabled = false;
      loginSubmit.textContent = "Entrar";
    }
  });

  // ---------------------------------------------------------
  // REGISTER
  // ---------------------------------------------------------
  const registerForm = document.getElementById("registerForm");
  const registerError = document.getElementById("registerError");
  const registerSubmit = document.getElementById("registerSubmit");
  const registerNext = document.getElementById("registerNext");
  const registerBack = document.getElementById("registerBack");
  const hospitalSelect = document.getElementById("registerHospital");
  const unidadSelect = document.getElementById("registerUnidad");
  const medicoSelect = document.getElementById("registerMedico");
  const adminNotice = document.getElementById("registerAdminNotice");
  const medicoSelector = document.getElementById("registerMedicoSelector");

  const step1 = registerForm?.querySelector('.step[data-step="1"]');
  const step2 = registerForm?.querySelector('.step[data-step="2"]');

  let currentStep = 1;
  let opcionesRegistro = null;

  // Inicialización: quitar hidden y activar paso 1
  if (step1 && step2) {
    step1.hidden = false;
    step2.hidden = false;
    step1.classList.add("is-active");
    step2.classList.remove("is-active");
  }

  // --- Selector hospital -> unidad -> médico ---
  const llenarSelect = (select, items, valueKey, labelFn, placeholder) => {
    if (!select) return;
    select.innerHTML = "";
    const first = document.createElement("option");
    first.value = "";
    first.textContent = placeholder;
    select.appendChild(first);

    for (const item of items) {
      const option = document.createElement("option");
      option.value = item[valueKey];
      option.textContent = labelFn(item);
      select.appendChild(option);
    }
    select.value = "";
  };

  const limpiarMedicos = () => {
    if (!medicoSelect) return;
    llenarSelect(medicoSelect, [], "codMed", () => "", "Selecciona un médico");
    medicoSelect.disabled = true;
  };

  const cargarOpcionesRegistro = async () => {
    try {
      const res = await api.registroOpciones();
      if (!res.ok) {
        registerError.textContent =
          res.error || "No se pudieron cargar las opciones de registro.";
        return;
      }

      opcionesRegistro = res.data || {};
      const esPrimerUsuario = Boolean(opcionesRegistro.primerUsuario);

      if (adminNotice) adminNotice.hidden = !esPrimerUsuario;
      if (medicoSelector) medicoSelector.hidden = esPrimerUsuario;

      [hospitalSelect, unidadSelect, medicoSelect].forEach((select) => {
        if (!select) return;
        select.required = !esPrimerUsuario;
        select.disabled = esPrimerUsuario;
      });

      if (esPrimerUsuario) return;

      llenarSelect(
        hospitalSelect,
        opcionesRegistro.hospitales || [],
        "codHos",
        (hospital) => hospital.nomHos,
        "Selecciona un hospital",
      );
      llenarSelect(
        unidadSelect,
        [],
        "codUni",
        (unidad) => unidad.nomUni,
        "Selecciona una unidad",
      );
      unidadSelect.disabled = true;
      limpiarMedicos();
    } catch (error) {
      registerError.textContent =
        "No se pudieron cargar las opciones de registro.";
      console.error("Error al cargar las opciones de registro:", error);
    }
  };

  hospitalSelect?.addEventListener("change", () => {
    const codHos = hospitalSelect.value;
    const unidades = (opcionesRegistro?.unidades || []).filter(
      (unidad) => unidad.codHos === codHos,
    );

    llenarSelect(
      unidadSelect,
      unidades,
      "codUni",
      (unidad) => unidad.nomUni,
      "Selecciona una unidad",
    );
    unidadSelect.disabled = !codHos;
    limpiarMedicos();
  });

  unidadSelect?.addEventListener("change", () => {
    const codUni = unidadSelect.value;
    const medicos = (opcionesRegistro?.medicos || []).filter(
      (medico) => medico.codUni === codUni,
    );

    llenarSelect(
      medicoSelect,
      medicos,
      "codMed",
      (medico) => `${medico.nomMed} — ${medico.espMed}`,
      "Selecciona un médico",
    );
    medicoSelect.disabled = !codUni;
  });

  registerForm?.addEventListener("change", () => {
    registerError.textContent = "";
  });

  if (registerForm) {
    cargarOpcionesRegistro();
  }

  // --- Animación entre pasos ---
  const animateToStep = (target, direction) => {
    if (!step1 || !step2 || target === currentStep) return;

    const from = currentStep === 1 ? step1 : step2;
    const to = target === 1 ? step1 : step2;

    from.classList.remove("is-active");
    from.classList.add(
      direction === "forward" ? "is-leaving-left" : "is-leaving-right",
    );

    to.style.transition = "none";
    to.classList.remove("is-leaving-left", "is-leaving-right");
    to.style.transform =
      direction === "forward" ? "translateX(24px)" : "translateX(-24px)";
    to.style.opacity = "0";
    to.style.visibility = "visible";
    to.style.pointerEvents = "none";

    void to.offsetWidth;

    to.style.transition = "";
    to.style.transform = "";
    to.style.opacity = "";
    to.classList.add("is-active");

    setTimeout(() => {
      from.classList.remove("is-leaving-left", "is-leaving-right");
    }, 350);

    currentStep = target;
    registerError.textContent = "";
  };

  const goToStep = (step) => {
    animateToStep(step, step > currentStep ? "forward" : "backward");
  };

  // --- Helper: valida los inputs del paso visible con el tooltip nativo ---
  const reportCurrentStep = () => {
    const inputs = registerForm.querySelectorAll(
      ".step.is-active input, .step.is-active select, .step.is-active textarea",
    );
    for (const input of inputs) {
      if (!input.checkValidity()) {
        input.reportValidity(); // ⬅️ muestra "Completa este campo" nativo
        return false;
      }
    }
    return true;
  };

  // --- PASO 1 → validar nativo y avanzar ---
  registerNext?.addEventListener("click", () => {
    registerError.textContent = "";
    if (!reportCurrentStep()) return;
    goToStep(2);
  });

  // --- PASO 2 → volver al paso 1 ---
  registerBack?.addEventListener("click", () => {
    goToStep(1);
  });

  // --- PASO 2 → submit final ---
  registerForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (currentStep !== 2) return;

    registerError.textContent = "";

    // 1) Validación nativa (tooltip "Completa este campo" + required)
    if (!reportCurrentStep()) return;

    // 2) Validaciones extra (formato / coincidencia / longitud)
    const email = document.getElementById("registerEmail").value.trim();
    const password = document.getElementById("registerPass").value;
    const confirm = document.getElementById("registerPassConfirm").value;

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      registerError.textContent = "El correo no tiene un formato válido.";
      return;
    }
    if (password.length < 6) {
      registerError.textContent =
        "La contraseña debe tener al menos 6 caracteres.";
      return;
    }
    if (password !== confirm) {
      registerError.textContent = "Las contraseñas no coinciden.";
      return;
    }

    const data = {
      username: document.getElementById("registerUsername").value.trim(),
      email,
      password,
    };

    if (opcionesRegistro && !opcionesRegistro.primerUsuario) {
      data.codMed = medicoSelect?.value || "";
      if (!data.codMed) {
        registerError.textContent =
          "Selecciona el médico al que pertenece la cuenta.";
        return;
      }
    }

    registerSubmit.disabled = true;
    registerSubmit.textContent = "Registrando...";

    try {
      const res = await api.register(data);
      if (!res.ok) {
        registerError.textContent = res.error || "No se pudo registrar.";
        return;
      }
      registerForm.reset();
      goToStep(1);
      unidadSelect && (unidadSelect.disabled = true);
      limpiarMedicos();
      setState("login");
      document.getElementById("loginEmail").value = data.email;
      cargarOpcionesRegistro();
    } catch (error) {
      registerError.textContent =
        "No se pudo completar el registro. Inténtalo de nuevo.";
      console.error("Error al registrar usuario:", error);
    } finally {
      registerSubmit.disabled = false;
      registerSubmit.textContent = "Registrarse";
    }
  });
});
