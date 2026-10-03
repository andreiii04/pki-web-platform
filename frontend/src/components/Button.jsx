import Icon from "./Icon";

const BASE =
  "inline-flex items-center justify-center gap-2 font-medium whitespace-nowrap rounded-md transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent-500 focus-visible:ring-offset-2 focus-visible:ring-offset-surface-bg disabled:cursor-not-allowed disabled:opacity-50";

const VARIANTS = {
  primary: "bg-accent-500 text-white hover:bg-accent-600 active:bg-accent-700",
  secondary:
    "bg-surface-2 text-fg-1 border border-border-1 hover:bg-surface-3 hover:border-border-2",
  ghost: "bg-transparent text-fg-2 hover:text-fg-1 hover:bg-white/5",
  danger:
    "bg-transparent text-bad-500 border border-bad-500/30 hover:bg-bad-500/10",
};

const SIZES = {
  sm: "px-3 py-1.5 text-[13px]",
  md: "px-4 py-2.5 text-sm",
  lg: "px-5 py-3 text-[15px]",
};

export default function Button({
  variant = "primary",
  size = "md",
  icon,
  iconRight,
  children,
  className = "",
  ...rest
}) {
  const iconSize = size === "sm" ? 14 : 16;
  return (
    <button
      className={`${BASE} ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
      {...rest}
    >
      {icon && <Icon name={icon} size={iconSize} />}
      {children}
      {iconRight && <Icon name={iconRight} size={iconSize} />}
    </button>
  );
}
