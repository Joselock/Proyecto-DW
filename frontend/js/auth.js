import { api } from "./api.js";

document.addEventListener("DOMContentLoaded", () => {
  const app = document.getElementById("app");
  if (!app) return;

  // ---------------------------------------------------------
  // Cambio de estado global (define una sola vez, arriba)
  // ---------------------------------------------------------
  const setState = (state) => {
    app.dataset.state = state;
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
  // REGISTER (2 pasos animados · validación completa)
  // Paso 1 = datos personales · Paso 2 = credenciales
  // ---------------------------------------------------------
  const registerForm = document.getElementById("registerForm");
  const registerError = document.getElementById("registerError");
  const registerSubmit = document.getElementById("registerSubmit");
  const registerNext = document.getElementById("registerNext");
  const registerBack = document.getElementById("registerBack");

  const step1 = registerForm?.querySelector('.step[data-step="1"]');
  const step2 = registerForm?.querySelector('.step[data-step="2"]');

  // Estado del paso actual
  let currentStep = 1;

  // --- Inicialización: quitar hidden y activar paso 1 ---
  if (step1 && step2) {
    step1.hidden = false;
    step2.hidden = false;
    step1.classList.add("is-active");
    step2.classList.remove("is-active");
  }

  // --- Animación entre pasos ---
  const animateToStep = (target, direction) => {
    if (!step1 || !step2 || target === currentStep) return;

    const from = currentStep === 1 ? step1 : step2;
    const to = target === 1 ? step1 : step2;

    // Saliente
    from.classList.remove("is-active");
    from.classList.add(
      direction === "forward" ? "is-leaving-left" : "is-leaving-right",
    );

    // Entrante: preparar sin transición
    to.style.transition = "none";
    to.classList.remove("is-leaving-left", "is-leaving-right");
    to.style.transform =
      direction === "forward" ? "translateX(24px)" : "translateX(-24px)";
    to.style.opacity = "0";
    to.style.visibility = "visible";
    to.style.pointerEvents = "none";

    void to.offsetWidth; // forzar reflow

    // Animar entrada
    to.style.transition = "";
    to.style.transform = "";
    to.style.opacity = "";
    to.classList.add("is-active");

    // Limpiar clase de salida
    setTimeout(() => {
      from.classList.remove("is-leaving-left", "is-leaving-right");
    }, 350);

    currentStep = target;
    registerError.textContent = "";
  };

  const goToStep = (step) => {
    animateToStep(step, step > currentStep ? "forward" : "backward");
  };

  // --- Utilidades de validación ---
  const val = (id) => (document.getElementById(id)?.value ?? "").trim();

  const markInvalid = (id) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.add("is-invalid");
    el.addEventListener("input", () => el.classList.remove("is-invalid"), {
      once: true,
    });
  };

  const validateStep1 = () => {
    const missing = [];
    if (!val("registerNombre")) {
      missing.push("Nombre completo");
      markInvalid("registerNombre");
    }
    if (!val("registerEspecialidad")) {
      missing.push("Especialidad");
      markInvalid("registerEspecialidad");
    }
    if (!val("registerDireccion")) {
      missing.push("Dirección");
      markInvalid("registerDireccion");
    }
    if (!val("registerTelefono")) {
      missing.push("Teléfono");
      markInvalid("registerTelefono");
    }

    if (missing.length) {
      registerError.textContent = `Completa: ${missing.join(", ")}.`;
      return false;
    }
    return true;
  };

  const validateStep2 = () => {
    const missing = [];
    if (!val("registerUsername")) {
      missing.push("Username");
      markInvalid("registerUsername");
    }
    if (!val("registerEmail")) {
      missing.push("Correo electrónico");
      markInvalid("registerEmail");
    }
    if (!val("registerPass")) {
      missing.push("Password");
      markInvalid("registerPass");
    }
    if (!val("registerPassConfirm")) {
      missing.push("Confirm password");
      markInvalid("registerPassConfirm");
    }

    if (missing.length) {
      registerError.textContent = `Completa: ${missing.join(", ")}.`;
      return false;
    }

    const email = val("registerEmail");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      registerError.textContent = "El correo no tiene un formato válido.";
      markInvalid("registerEmail");
      return false;
    }

    if (val("registerPass").length < 6) {
      registerError.textContent =
        "La contraseña debe tener al menos 6 caracteres.";
      markInvalid("registerPass");
      return false;
    }

    if (val("registerPass") !== val("registerPassConfirm")) {
      registerError.textContent = "Las contraseñas no coinciden.";
      markInvalid("registerPassConfirm");
      return false;
    }

    return true;
  };

  // --- PASO 1 → validar y avanzar ---
  registerNext?.addEventListener("click", () => {
    registerError.textContent = "";
    if (!validateStep1()) return;
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
    if (!validateStep1() || !validateStep2()) return;

    const data = {
      username: val("registerUsername"),
      email: val("registerEmail"),
      password: document.getElementById("registerPass").value,
      nombre: val("registerNombre"),
      especialidad: val("registerEspecialidad"),
      direccion: val("registerDireccion"),
      telefono: val("registerTelefono"),
    };

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
      setState("login");
      document.getElementById("loginEmail").value = data.email;
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
