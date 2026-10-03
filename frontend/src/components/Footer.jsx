import Icon from "./Icon";

export default function Footer() {
  return (
    <footer className="border-t border-border-1 mt-24">
      <div className="max-w-[1200px] mx-auto px-6 py-12 grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-16">
        <div className="max-w-[420px]">
          <div className="flex items-center gap-2 mb-4">
            <Icon name="logo" size={20} className="text-accent-500" />
            <span className="text-[15px] font-semibold text-fg-1">PKI Sign</span>
          </div>
          <p className="text-[13px] text-fg-3 leading-relaxed">
            Digital signatures on PDF documents, backed by a private X.509
            Certificate Authority.
          </p>
        </div>
        <div className="md:justify-self-end">
          <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-fg-3 mb-3">
            Standards
          </div>
          <ul className="space-y-2 text-[13px] text-fg-2">
            <li>X.509 v3 certificates</li>
            <li>RFC 5280 (PKIX)</li>
            <li>PAdES PDF signatures</li>
            <li>PKCS #7 / CMS</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-border-1">
        <div className="max-w-[1200px] mx-auto px-6 py-4 flex items-center justify-between text-[12px] text-fg-4">
          <span>© 2026 PKI Sign. Private CA — not for production use.</span>
          <span className="font-mono whitespace-nowrap">v0.1.0</span>
        </div>
      </div>
    </footer>
  );
}
