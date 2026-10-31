document.addEventListener("DOMContentLoaded", () => {
  const container = document.getElementById("container");
  const btnLogin = document.getElementById("btnLogin");
  const btnRegister = document.getElementById("btnRegister");
  const backFromLogin = document.getElementById("backFromLogin");
  const backFromRegister = document.getElementById("backFromRegister");

  if (!container) {
    console.warn("No se encontró #container en la página");
    return;
  }

  // Mostrar formulario de LOGIN (desliza desde la izquierda)
  btnLogin?.addEventListener("click", () => {
    container.classList.remove("mostrar-register");
    container.classList.add("mostrar-login");
  });

  // Mostrar formulario de REGISTRO (desliza desde la derecha)
  btnRegister?.addEventListener("click", () => {
    container.classList.remove("mostrar-login");
    container.classList.add("mostrar-register");
  });

  // Volver al panel principal
  const volver = () => {
    container.classList.remove("mostrar-login", "mostrar-register");
  };

  backFromLogin?.addEventListener("click", volver);
  backFromRegister?.addEventListener("click", volver);
});
