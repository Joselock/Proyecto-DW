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
  // REGISTER
  // ---------------------------------------------------------
  const registerForm = document.getElementById("registerForm");
  const registerError = document.getElementById("registerError");
  const registerSubmit = document.getElementById("registerSubmit");

  registerForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    registerError.textContent = "";

    const data = {
      username: document.getElementById("registerUsername").value.trim(),
      email: document.getElementById("registerEmail").value.trim(),
      password: document.getElementById("registerPass").value,
      nombre: document.getElementById("registerNombre").value.trim(),
      especialidad: document.getElementById("registerEspecialidad").value.trim(),
      direccion: document.getElementById("registerDireccion").value.trim(),
      telefono: document.getElementById("registerTelefono").value.trim(),
    };

    if (!data.username || !data.email || !data.password) {
      registerError.textContent = "Completa los campos obligatorios.";
      return;
    }
    if (data.password !== document.getElementById("registerPassConfirm").value) {
      registerError.textContent = "Las contraseñas no coinciden.";
      return;
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
      setState("login");
      document.getElementById("loginEmail").value = data.email;
    } catch (error) {
      registerError.textContent = "No se pudo completar el registro. Inténtalo de nuevo.";
      console.error("Error al registrar usuario:", error);
    } finally {
      registerSubmit.disabled = false;
      registerSubmit.textContent = "Registrarse";
    }
  });
});
