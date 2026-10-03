/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        "surface-bg": "#0f172a",
        "surface-1": "#131c30",
        "surface-2": "#1e293b",
        "surface-3": "#273449",
        "surface-4": "#334155",
        "surface-5": "#475569",
        "fg-1": "#f8fafc",
        "fg-2": "#cbd5e1",
        "fg-3": "#94a3b8",
        "fg-4": "#64748b",
        "fg-5": "#475569",
        "border-1": "rgba(255,255,255,0.08)",
        "border-2": "rgba(255,255,255,0.14)",
        "accent-300": "#93c5fd",
        "accent-500": "#3b82f6",
        "accent-600": "#2563eb",
        "accent-700": "#1d4ed8",
        "ok-500": "#10b981",
        "warn-500": "#f59e0b",
        "bad-500": "#f43f5e",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      boxShadow: {
        modal: "0 24px 64px -16px rgba(0,0,0,0.6)",
      },
    },
  },
  plugins: [],
};
