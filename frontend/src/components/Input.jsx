export default function Input({ label, required, helper, error, ...rest }) {
  return (
    <label className="flex flex-col gap-1.5">
      {label && (
        <span className="text-xs font-semibold text-fg-3 tracking-wide">
          {label}
          {required && <span className="text-accent-500 ml-0.5">*</span>}
        </span>
      )}
      <input
        className={`w-full bg-surface-3 border rounded-md px-3 py-2.5 text-sm text-fg-1 placeholder:text-fg-4 outline-none transition-colors duration-150 ${
          error
            ? "border-bad-500 focus:ring-2 focus:ring-bad-500/20"
            : "border-border-1 focus:border-accent-500 focus:ring-2 focus:ring-accent-500/20"
        }`}
        {...rest}
      />
      {(helper || error) && (
        <span className={`text-xs ${error ? "text-bad-500" : "text-fg-4"}`}>
          {error || helper}
        </span>
      )}
    </label>
  );
}
