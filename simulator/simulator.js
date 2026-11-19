"use strict";
// Demo-only simulator. No vehicle APIs or audio access.
const light = document.getElementById("light");
const level = document.getElementById("level");
const number = document.getElementById("number");
const button = document.getElementById("demo");
const status = document.getElementById("status");
let interval = null;
function render(value) {
  const n = Math.max(0, Math.min(100, Number(value) || 0));
  number.textContent = Math.round(n) + "%";
  light.style.opacity = String(0.12 + n * 0.0088);
  light.style.boxShadow = `0 0 ${8 + n * 0.45}px ${1 + n * 0.19}px rgba(123,81,255,${0.2 + n * 0.008})`;
}
level.addEventListener("input", () => render(level.value));
button.addEventListener("click", () => {
  if (interval !== null) {
    clearInterval(interval); interval = null;
    button.textContent = "Start demo beat";
    status.textContent = "Manual mode.";
    return;
  }
  button.textContent = "Stop demo beat";
  status.textContent = "Demo mode: synthesized pulse, not actual music.";
  let tick = 0;
  interval = setInterval(() => {
    tick += 1;
    // Simple alternating synthesized pulses for visual testing.
    const pulse = tick % 8 === 0 ? 95 : tick % 4 === 0 ? 65 : Math.max(0, 30 - (tick % 4) * 11);
    level.value = String(pulse);
    render(pulse);
  }, 125);
});
render(0);
