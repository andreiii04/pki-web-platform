import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import api, { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from "../api/axios";

const AuthContext = createContext(null);

function readStoredUser() {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [user, setUser] = useState(readStoredUser);

  const persist = useCallback((authResponse) => {
    const { accessToken, username, email, role } = authResponse;
    const nextUser = { username, email, role };
    localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser));
    setToken(accessToken);
    setUser(nextUser);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const login = useCallback(
    async (username, password) => {
      const { data } = await api.post("/api/auth/login", { username, password });
      persist(data);
      return data;
    },
    [persist],
  );

  const register = useCallback(
    async ({ username, email, password }) => {
      const { data } = await api.post("/api/auth/register", {
        username,
        email,
        password,
      });
      persist(data);
      return data;
    },
    [persist],
  );

  useEffect(() => {
    const onUnauthorized = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener("pki:unauthorized", onUnauthorized);
    return () => window.removeEventListener("pki:unauthorized", onUnauthorized);
  }, []);

  const value = useMemo(
    () => ({ token, user, login, register, logout }),
    [token, user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// The hook lives next to its provider on purpose; it is not a component.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
