document.addEventListener("DOMContentLoaded", () => {
  const app = document.getElementById("app");
  if (!app) return;

  // Cambia el estado global del "escenario"
  const setState = (state) => {
    app.dataset.state = state;
  };

  // Desde la home
  document
    .getElementById("btnGoLogin")
    ?.addEventListener("click", () => setState("login"));
  document
    .getElementById("btnGoRegister")
    ?.addEventListener("click", () => setState("register"));

  // Botones para volver a la home (uno por cada panel de info + móvil)
  [
    "backFromLoginLeft",
    "backFromRegisterLeft",
    "backMobileLogin",
    "backMobileRegister",
  ].forEach((id) => {
    document
      .getElementById(id)
      ?.addEventListener("click", () => setState("home"));
  });

  // Cambios entre login y registro (esta es la animación de deslizamiento)
  document
    .getElementById("btnSwitchToRegister")
    ?.addEventListener("click", () => setState("register"));
  document
    .getElementById("btnSwitchToLogin")
    ?.addEventListener("click", () => setState("login"));
});

// ---------------------------------------------------------
// Toggle mostrar / ocultar contraseña (Bootstrap Icons)
// ---------------------------------------------------------
document.querySelectorAll(".toggle-pass").forEach((btn) => {
  btn.addEventListener("click", () => {
    const input = btn.parentElement.querySelector("input");
    const icon = btn.querySelector("i");
    if (!input || !icon) return;

    const show = input.type === "password";
    input.type = show ? "text" : "password";

    // Intercambiar clases del icono
    icon.classList.toggle("bi-eye", !show);
    icon.classList.toggle("bi-eye-slash", show);

    // Accesibilidad
    btn.setAttribute("aria-pressed", show);
    btn.setAttribute(
      "aria-label",
      show ? "Ocultar contraseña" : "Mostrar contraseña",
    );
  });
});
