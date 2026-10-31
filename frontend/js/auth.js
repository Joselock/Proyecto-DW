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
