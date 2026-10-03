import { useState } from "react";
import { Route, Routes } from "react-router-dom";
import AuthModal from "./components/AuthModal";
import Footer from "./components/Footer";
import Navbar from "./components/Navbar";
import GeneratePage from "./pages/GeneratePage";
import HomePage from "./pages/HomePage";
import SignPage from "./pages/SignPage";
import VerifyPage from "./pages/VerifyPage";

export default function App() {
  const [authModal, setAuthModal] = useState({ open: false, mode: "login" });

  const openAuth = (mode = "login") => setAuthModal({ open: true, mode });
  const closeAuth = () => setAuthModal((s) => ({ ...s, open: false }));

  return (
    <div className="min-h-screen flex flex-col relative">
      <div
        aria-hidden="true"
        className="fixed top-16 left-0 right-0 bottom-0 pointer-events-none -z-10"
        style={{
          background:
            "radial-gradient(ellipse at 25% 15%, #1d4ed8 0%, rgba(29,78,216,0) 55%), radial-gradient(ellipse at 80% 75%, #0e7490 0%, rgba(14,116,144,0) 50%), #0b1226",
        }}
      />
      <div
        aria-hidden="true"
        className="fixed top-16 left-0 right-0 bottom-0 pointer-events-none -z-10"
        style={{
          backgroundImage:
            "linear-gradient(rgba(148,163,184,0.07) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,0.07) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
        }}
      />
      <div
        className="fixed top-16 left-0 right-0 bottom-0 pointer-events-none -z-10 mix-blend-color"
        style={{ background: "#1e3a8a" }}
      />
      <div
        className="fixed top-16 left-0 right-0 h-24 pointer-events-none -z-10"
        style={{
          background:
            "linear-gradient(to bottom, #0f172a 0%, rgba(15,23,42,0.55) 45%, rgba(15,23,42,0) 100%)",
        }}
      />
      <div
        className="fixed top-16 left-0 right-0 bottom-0 pointer-events-none -z-10"
        style={{ background: "rgba(15,23,42,0.55)" }}
      />

      <Navbar onOpenAuth={openAuth} />

      <div className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/verify" element={<VerifyPage />} />
          <Route
            path="/generate"
            element={<GeneratePage onRequireAuth={() => openAuth("login")} />}
          />
          <Route
            path="/sign"
            element={<SignPage onRequireAuth={() => openAuth("login")} />}
          />
        </Routes>
      </div>

      <Footer />

      <AuthModal
        open={authModal.open}
        initialMode={authModal.mode}
        onClose={closeAuth}
      />
    </div>
  );
}
