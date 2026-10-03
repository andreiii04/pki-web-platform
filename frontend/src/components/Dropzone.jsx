import { useRef, useState } from "react";
import Icon from "./Icon";

export default function Dropzone({
  onFile,
  file,
  hint = "Drop a PDF here",
  sub = "or click to browse · up to 10 MB",
}) {
  const [over, setOver] = useState(false);
  const ref = useRef(null);

  const handleDrop = (e) => {
    e.preventDefault();
    setOver(false);
    const f = e.dataTransfer.files[0];
    if (f) onFile(f);
  };

  if (file) {
    return (
      <div className="bg-surface-2 border border-border-1 rounded-[10px] p-4 flex items-center gap-3">
        <div className="w-10 h-10 rounded-md bg-surface-3 border border-border-1 flex items-center justify-center text-accent-300">
          <Icon name="fileCheck" size={20} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium text-fg-1 truncate">{file.name}</div>
          <div className="text-xs text-fg-4 mt-0.5">
            {(file.size / 1024).toFixed(1)} KB · application/pdf
          </div>
        </div>
        <button
          type="button"
          onClick={() => onFile(null)}
          className="text-fg-3 hover:text-fg-1 p-1.5"
          aria-label="Remove file"
        >
          <Icon name="x" size={16} />
        </button>
      </div>
    );
  }

  return (
    <div
      onClick={() => ref.current?.click()}
      onDragOver={(e) => {
        e.preventDefault();
        setOver(true);
      }}
      onDragLeave={() => setOver(false)}
      onDrop={handleDrop}
      className={`cursor-pointer rounded-[10px] border-[1.5px] border-dashed p-10 flex flex-col items-center justify-center text-center gap-2 transition-colors duration-150 ${
        over
          ? "border-accent-500 bg-accent-500/[0.08]"
          : "border-border-2 bg-white/[0.015] hover:bg-white/[0.03]"
      }`}
    >
      <input
        ref={ref}
        type="file"
        accept="application/pdf"
        className="hidden"
        onChange={(e) => e.target.files[0] && onFile(e.target.files[0])}
      />
      <Icon name="upload" size={24} className={over ? "text-accent-500" : "text-fg-3"} />
      <div className="text-sm font-medium text-fg-1">{hint}</div>
      <div className="text-xs text-fg-4">{sub}</div>
    </div>
  );
}
