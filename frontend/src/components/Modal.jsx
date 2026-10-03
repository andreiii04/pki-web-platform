import Icon from "./Icon";

export default function Modal({ open, onClose, title, children, footer }) {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-surface-bg/72 backdrop-blur-[8px]"
      onClick={onClose}
    >
      <div
        className="bg-surface-2 border border-border-2 rounded-[14px] w-full max-w-md shadow-modal"
        style={{ boxShadow: "0 24px 64px -16px rgba(0,0,0,0.6)" }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 pt-5 pb-4 flex items-center justify-between border-b border-border-1">
          <h3 className="text-[17px] font-semibold text-fg-1">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-fg-3 hover:text-fg-1 p-1"
            aria-label="Close"
          >
            <Icon name="x" size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {footer && (
          <div className="px-6 py-4 border-t border-border-1 flex justify-end gap-2">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
