export default function Card({
  children,
  className = "",
  hoverable = false,
  ...rest
}) {
  return (
    <div
      className={`bg-surface-2 border border-border-1 rounded-[10px] ${
        hoverable
          ? "hover:bg-surface-3 hover:border-border-2 transition-colors duration-150 cursor-pointer"
          : ""
      } ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
