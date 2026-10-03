import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { extractErrorMessage } from "../utils/format";
import Button from "./Button";
import Input from "./Input";
import Modal from "./Modal";

export default function AuthModal({ open, onClose, initialMode = "login", onSuccess }) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState(initialMode);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      setMode(initialMode);
      setUsername("");
      setEmail("");
      setPassword("");
      setError(null);
      setLoading(false);
    }
  }, [open, initialMode]);

  const submit = async () => {
    setError(null);
    setLoading(true);
    try {
      if (mode === "login") {
        await login(username, password);
      } else {
        await register({ username, email, password });
      }
      onSuccess?.();
      onClose?.();
    } catch (err) {
      setError(extractErrorMessage(err, "Authentication failed"));
    } finally {
      setLoading(false);
    }
  };

  const tabClass = (active) =>
    `flex-1 px-3 py-2 text-[13px] font-medium rounded-md transition-colors duration-150 ${
      active
        ? "bg-surface-3 text-fg-1"
        : "bg-transparent text-fg-3 hover:text-fg-1"
    }`;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={mode === "login" ? "Log in" : "Create an account"}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={submit}
            disabled={
              loading ||
              !username.trim() ||
              !password ||
              (mode === "register" && !email.trim())
            }
          >
            {loading
              ? mode === "login"
                ? "Signing in…"
                : "Creating…"
              : mode === "login"
                ? "Log in"
                : "Create account"}
          </Button>
        </>
      }
    >
      <div className="flex gap-1 p-1 bg-surface-bg/40 border border-border-1 rounded-md mb-5">
        <button
          type="button"
          className={tabClass(mode === "login")}
          onClick={() => setMode("login")}
        >
          Log in
        </button>
        <button
          type="button"
          className={tabClass(mode === "register")}
          onClick={() => setMode("register")}
        >
          Create account
        </button>
      </div>

      {mode === "register" && (
        <p className="text-[13px] text-fg-3 leading-[1.5] mb-4">
          Pick a username (3–50 chars), a valid email, and a password of at least
          8 characters.
        </p>
      )}

      <div className="space-y-4">
        <Input
          label="Username"
          required
          placeholder="jane"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        {mode === "register" && (
          <Input
            label="Email"
            type="email"
            required
            placeholder="you@example.com"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        )}
        <Input
          label="Password"
          type="password"
          required
          placeholder="••••••••"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit();
          }}
        />
      </div>

      {error && (
        <div className="mt-4 px-3 py-2 rounded-md bg-bad-500/14 text-bad-500 text-[13px]">
          {error}
        </div>
      )}
    </Modal>
  );
}
