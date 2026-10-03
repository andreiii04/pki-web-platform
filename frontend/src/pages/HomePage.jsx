import { useNavigate } from "react-router-dom";
import Card from "../components/Card";
import Eyebrow from "../components/Eyebrow";
import Icon from "../components/Icon";

const FEATURES = [
  {
    icon: "shield",
    route: "/verify",
    title: "Verify a signature",
    body: "Upload any PDF. Every embedded signature is checked against the platform CA — integrity, trust, certificate status.",
    cta: "Verify a PDF",
  },
  {
    icon: "key",
    route: "/generate",
    title: "Generate a certificate",
    body: "Issue an X.509 certificate bound to your verified identity. Stored encrypted, ready to sign.",
    cta: "Generate one",
  },
  {
    icon: "pen",
    route: "/sign",
    title: "Sign a document",
    body: "Authenticate, choose a certificate, drop a PDF. Get back a PAdES-signed document in seconds.",
    cta: "Sign a PDF",
  },
];

const STEPS = [
  {
    n: "01",
    t: "Identity",
    d: "Register an account. The platform binds it to a Common Name and an Organization.",
  },
  {
    n: "02",
    t: "Certificate",
    d: "The platform's private CA issues an X.509 certificate. Keys are stored encrypted server-side.",
  },
  { n: "03", t: "Signature", d: "Sign a PDF with PAdES." },
];

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <main>
      <section className="relative overflow-hidden">
        <div
          className="absolute inset-0 pointer-events-none opacity-[0.35]"
          style={{
            backgroundImage:
              "linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px)",
            backgroundSize: "64px 64px",
            maskImage:
              "radial-gradient(ellipse at 50% 30%, black 30%, transparent 75%)",
            WebkitMaskImage:
              "radial-gradient(ellipse at 50% 30%, black 30%, transparent 75%)",
          }}
        />
        <div className="relative max-w-[1200px] mx-auto px-6 pt-24 pb-32 text-center">
          <h1 className="text-[56px] font-semibold tracking-[-0.02em] leading-[1.05] text-fg-1 max-w-[820px] mx-auto">
            Digital signatures,
            <br />
            <span className="text-fg-3">verifiable by anyone.</span>
          </h1>
          <p className="mt-6 text-[17px] text-fg-3 max-w-[600px] mx-auto leading-[1.55]">
            A web platform for issuing X.509 certificates and applying PAdES
            signatures to PDF documents — backed by a private Certificate
            Authority.
          </p>
        </div>
      </section>

      <section className="max-w-[1200px] mx-auto px-6 pb-24">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {FEATURES.map((f) => (
            <Card
              key={f.icon}
              hoverable
              onClick={() => navigate(f.route)}
              className="p-7 group flex flex-col"
            >
              <div className="w-11 h-11 rounded-lg bg-surface-3 border border-border-1 flex items-center justify-center text-accent-300 mb-6">
                <Icon name={f.icon} size={22} />
              </div>
              <h3 className="text-[17px] font-semibold text-fg-1 mb-2">
                {f.title}
              </h3>
              <p className="text-[14px] text-fg-3 leading-[1.6] flex-1">{f.body}</p>
              <div className="mt-5 inline-flex items-center gap-1.5 text-[13px] font-medium text-accent-300 group-hover:text-accent-500 transition-colors">
                {f.cta}
                <Icon name="chevronRight" size={14} />
              </div>
            </Card>
          ))}
        </div>
      </section>

      <section className="border-t border-border-1">
        <div className="max-w-[1200px] mx-auto px-6 py-20">
          <Eyebrow>How it works</Eyebrow>
          <h2 className="mt-3 text-[32px] font-semibold tracking-[-0.02em] text-fg-1 max-w-[640px]">
            Three primitives. One trust anchor.
          </h2>
          <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-x-12 gap-y-8">
            {STEPS.map((s) => (
              <div key={s.n}>
                <div className="font-mono text-[13px] text-accent-300 mb-3">
                  {s.n}
                </div>
                <div className="text-[16px] font-semibold text-fg-1 mb-1.5">
                  {s.t}
                </div>
                <div className="text-[14px] text-fg-3 leading-[1.6]">{s.d}</div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
