const TONES = {
  ok: "bg-ok-500/14 text-ok-500",
  warn: "bg-warn-500/14 text-warn-500",
  bad: "bg-bad-500/14 text-bad-500",
  accent: "bg-accent-500/12 text-accent-300",
  neutral: "bg-surface-3 text-fg-2 border border-border-1",
};

const DOT_COLORS = {
  ok: "bg-ok-500",
  warn: "bg-warn-500",
  bad: "bg-bad-500",
  accent: "bg-accent-500",
  neutral: "bg-fg-3",
};

export default function Badge({ tone = "neutral", dot = false, children }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[12px] font-medium leading-none ${TONES[tone]}`}
    >
      {dot && <span className={`w-1.5 h-1.5 rounded-full ${DOT_COLORS[tone]}`} />}
      {children}
    </span>
  );
}
