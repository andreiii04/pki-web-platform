import { Link, NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Icon from "./Icon";
import Button from "./Button";

export default function Navbar({ onOpenAuth }) {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();

  const NavItem = ({ to, children }) => {
    const active = pathname === to;
    return (
      <NavLink
        to={to}
        className={`text-sm transition-colors duration-150 ${
          active ? "text-fg-1" : "text-fg-3 hover:text-fg-1"
        }`}
      >
        {children}
      </NavLink>
    );
  };

  return (
    <header className="sticky top-0 z-30 h-16 border-b border-border-1 bg-surface-bg">
      <div className="max-w-[1200px] mx-auto h-full px-6 flex items-center gap-8">
        <Link to="/" className="flex items-center gap-2">
          <Icon name="logo" size={22} className="text-accent-500" strokeWidth={1.5} />
          <span className="text-[16px] font-semibold tracking-[-0.01em] text-fg-1">
            PKI Sign
          </span>
        </Link>
        <nav className="hidden md:flex items-center gap-6">
          <NavItem to="/verify">Verify</NavItem>
          <NavItem to="/generate">Generate</NavItem>
          <NavItem to="/sign">Sign</NavItem>
        </nav>
        <div className="ml-auto flex items-center gap-3">
          {user ? (
            <>
              <div className="text-sm text-fg-3 hidden sm:block">
                Signed in as{" "}
                <span className="text-fg-1 font-medium">{user.username}</span>
              </div>
              <Button variant="secondary" size="sm" onClick={logout}>
                Log out
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onOpenAuth?.("login")}
              >
                Log in
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => onOpenAuth?.("register")}
              >
                Get started
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
