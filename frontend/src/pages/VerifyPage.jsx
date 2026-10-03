import { useState } from "react";
import api from "../api/axios";
import Badge from "../components/Badge";
import Dropzone from "../components/Dropzone";
import Icon from "../components/Icon";
import { extractErrorMessage, formatInstant } from "../utils/format";

const MAX_PDF_SIZE = 10 * 1024 * 1024;

export default function VerifyPage() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleFile = async (f) => {
    setFile(f);
    setResult(null);
    setError(null);
    if (!f) return;

    if (f.size > MAX_PDF_SIZE) {
      setError(`File too large (${(f.size / 1024 / 1024).toFixed(2)} MB); limit is 10 MB.`);
      return;
    }

    setLoading(true);
    try {
      const form = new FormData();
      form.append("file", f);
      const { data } = await api.post("/api/documents/verify", form);
      setResult(data);
    } catch (err) {
      setError(extractErrorMessage(err, "Verification failed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="max-w-[920px] mx-auto px-6 py-16">
      <h1 className="text-[36px] font-semibold tracking-[-0.02em] text-fg-1">
        Verify a signature
      </h1>
      <p className="mt-3 text-[15px] text-fg-3 max-w-[560px] leading-[1.6]">
        Upload a PDF to inspect every embedded signature: cryptographic
        integrity, certificate status, and CA trust.
      </p>

      <div className="mt-10">
        <Dropzone file={file} onFile={handleFile} />
      </div>

      {loading && (
        <div className="mt-6 text-[14px] text-fg-3 flex items-center gap-2">
          <span className="w-3 h-3 rounded-full border-2 border-accent-500 border-t-transparent animate-spin" />
          Inspecting signatures…
        </div>
      )}

      {error && (
        <div className="mt-6 px-4 py-3 rounded-md bg-bad-500/14 text-bad-500 text-[14px] flex items-start gap-2">
          <Icon name="alert" size={18} className="mt-0.5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {result && <VerificationResult result={result} />}
    </main>
  );
}

function VerificationResult({ result }) {
  if (!result.signed) {
    return (
      <div className="mt-8">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-9 h-9 rounded-full bg-warn-500/14 text-warn-500 flex items-center justify-center">
            <Icon name="alert" size={18} />
          </div>
          <div>
            <div className="text-[16px] font-semibold text-fg-1">
              Document is not signed
            </div>
            <div className="text-[13px] text-fg-3">
              No PAdES signatures found in this PDF.
            </div>
          </div>
        </div>
        {result.messages?.length > 0 && (
          <ul className="mt-4 space-y-1 text-[13px] text-fg-3 list-disc list-inside">
            {result.messages.map((m, i) => (
              <li key={i}>{m}</li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  const tone = result.allSignaturesValid ? "ok" : "bad";
  const headlineColor =
    tone === "ok"
      ? "bg-ok-500/14 text-ok-500"
      : "bg-bad-500/14 text-bad-500";

  return (
    <div className="mt-8">
      <div className="flex items-center gap-3 mb-5">
        <div
          className={`w-9 h-9 rounded-full flex items-center justify-center ${headlineColor}`}
        >
          <Icon
            name={result.allSignaturesValid ? "check" : "x"}
            size={18}
            strokeWidth={2}
          />
        </div>
        <div>
          <div className="text-[16px] font-semibold text-fg-1">
            Document signed · {result.signaturesCount} signature
            {result.signaturesCount === 1 ? "" : "s"}
          </div>
          <div className="text-[13px] text-fg-3">
            {result.allSignaturesValid
              ? "All signatures are cryptographically valid."
              : "One or more signatures failed integrity checks."}
          </div>
        </div>
      </div>

      <div className="space-y-3">
        {result.signatures.map((s, i) => (
          <SignatureCard key={i} index={i} sig={s} />
        ))}
      </div>

      {result.messages?.length > 0 && (
        <ul className="mt-5 space-y-1 text-[13px] text-fg-3 list-disc list-inside">
          {result.messages.map((m, i) => (
            <li key={i}>{m}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

function SignatureCard({ index, sig }) {
  const integrityTone = sig.integrityValid ? "ok" : "bad";
  const certTone =
    sig.certificateStatus === "ACTIVE"
      ? "ok"
      : sig.certificateStatus === "EXPIRED"
        ? "warn"
        : sig.certificateStatus === "REVOKED"
          ? "bad"
          : "neutral";
  const certLabel =
    sig.certificateStatus === "ACTIVE"
      ? "Active"
      : sig.certificateStatus === "EXPIRED"
        ? "Expired"
        : sig.certificateStatus === "REVOKED"
          ? "Revoked"
          : "Unknown to CA";

  return (
    <div className="bg-surface-2 border border-border-1 rounded-[10px] p-5">
      <div className="flex items-center gap-3 flex-wrap mb-4">
        <div className="text-[15px] font-semibold text-fg-1">
          Signature {index + 1}
        </div>
        <Badge tone={integrityTone} dot>
          {sig.integrityValid ? "Integrity valid" : "Tampered"}
        </Badge>
        <Badge tone={certTone} dot>
          {certLabel}
        </Badge>
        {sig.trusted && <Badge tone="accent">Trusted CA</Badge>}
        <div className="ml-auto text-[12px] text-fg-4 font-mono">
          {sig.fieldName}
        </div>
      </div>
      <dl className="grid grid-cols-1 sm:grid-cols-[140px_1fr] gap-y-2.5 gap-x-4 text-[13px]">
        <Row k="Signer" v={sig.signerCommonName} />
        <Row k="Signed at" v={formatInstant(sig.signedAt)} mono />
        <Row k="Serial" v={sig.serialNumber} mono />
        <Row k="Subject" v={sig.signerSubjectDn} mono />
        <Row k="Issuer" v={sig.issuerDn} mono />
      </dl>
    </div>
  );
}

function Row({ k, v, mono }) {
  return (
    <>
      <dt className="text-fg-3 font-medium">{k}</dt>
      <dd
        className={
          mono ? "text-fg-1 font-mono text-[12px] break-all" : "text-fg-1"
        }
      >
        {v ?? "—"}
      </dd>
    </>
  );
}
