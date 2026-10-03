import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import Badge from "../components/Badge";
import Button from "../components/Button";
import Dropzone from "../components/Dropzone";
import Icon from "../components/Icon";
import { useAuth } from "../context/AuthContext";
import { extractErrorMessage, extractFromDn, formatDateOnly } from "../utils/format";

const MAX_PDF_SIZE = 10 * 1024 * 1024;

export default function SignPage({ onRequireAuth }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [certs, setCerts] = useState([]);
  const [certsLoading, setCertsLoading] = useState(false);
  const [certsError, setCertsError] = useState(null);

  const [selectedId, setSelectedId] = useState(null);
  const [file, setFile] = useState(null);
  const [signing, setSigning] = useState(false);
  const [signError, setSignError] = useState(null);
  const [downloadInfo, setDownloadInfo] = useState(null);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    setCertsLoading(true);
    setCertsError(null);
    api
      .get("/api/certificates/my")
      .then(({ data }) => {
        if (cancelled) return;
        const sorted = [...data].sort((a, b) => {
          if (a.status === "ACTIVE" && b.status !== "ACTIVE") return -1;
          if (a.status !== "ACTIVE" && b.status === "ACTIVE") return 1;
          return new Date(b.issuedAt) - new Date(a.issuedAt);
        });
        setCerts(sorted);
        const firstActive = sorted.find((c) => c.status === "ACTIVE");
        if (firstActive) setSelectedId(firstActive.id);
      })
      .catch((err) => {
        if (cancelled) return;
        setCertsError(extractErrorMessage(err, "Could not load certificates"));
      })
      .finally(() => {
        if (!cancelled) setCertsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [user]);

  const handleFile = (f) => {
    setFile(f);
    setSignError(null);
    setDownloadInfo(null);
    if (f && f.size > MAX_PDF_SIZE) {
      setSignError(
        `File too large (${(f.size / 1024 / 1024).toFixed(2)} MB); limit is 10 MB.`,
      );
    }
  };

  const sign = async () => {
    if (!file || !selectedId) return;
    setSigning(true);
    setSignError(null);
    setDownloadInfo(null);
    try {
      const form = new FormData();
      form.append("file", file);
      form.append("certificateId", String(selectedId));
      const response = await api.post("/api/documents/sign", form, {
        responseType: "blob",
      });
      const downloadName = parseFilename(
        response.headers["content-disposition"],
        file.name,
      );
      triggerDownload(response.data, downloadName);
      setDownloadInfo({ name: downloadName, size: response.data.size });
    } catch (err) {
      setSignError(await readBlobError(err));
    } finally {
      setSigning(false);
    }
  };

  if (!user) {
    return (
      <main className="max-w-[480px] mx-auto px-6 py-24 text-center">
        <div className="w-14 h-14 rounded-full bg-surface-2 border border-border-1 flex items-center justify-center text-fg-3 mx-auto mb-6">
          <Icon name="lock" size={22} />
        </div>
        <h1 className="text-[28px] font-semibold tracking-[-0.02em] text-fg-1">
          Authentication required
        </h1>
        <p className="mt-3 text-[14px] text-fg-3 leading-[1.6]">
          Signing a document requires access to your encrypted private key. Log
          in to continue.
        </p>
        <Button
          variant="primary"
          size="lg"
          className="mt-7"
          onClick={() => onRequireAuth?.()}
        >
          Log in
        </Button>
      </main>
    );
  }

  return (
    <main className="max-w-[920px] mx-auto px-6 py-16">
      <h1 className="text-[36px] font-semibold tracking-[-0.02em] text-fg-1">
        Sign a document
      </h1>
      <p className="mt-3 text-[15px] text-fg-3 max-w-[560px] leading-[1.6]">
        Pick a certificate, upload a PDF, and download a PAdES-signed version.
      </p>

      <div className="mt-10 grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Cert picker */}
        <div>
          <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-fg-3 mb-3">
            1 · Choose a certificate
          </div>
          {certsLoading && (
            <div className="text-[13px] text-fg-3 flex items-center gap-2">
              <span className="w-3 h-3 rounded-full border-2 border-accent-500 border-t-transparent animate-spin" />
              Loading certificates…
            </div>
          )}
          {certsError && (
            <div className="px-3 py-2 rounded-md bg-bad-500/14 text-bad-500 text-[13px]">
              {certsError}
            </div>
          )}
          {!certsLoading && !certsError && certs.length === 0 && (
            <div className="bg-surface-2 border border-border-1 rounded-[10px] p-5 text-center">
              <div className="text-[14px] text-fg-2 mb-3">
                You don&apos;t have any certificates yet.
              </div>
              <Button
                variant="primary"
                size="sm"
                iconRight="arrowRight"
                onClick={() => navigate("/generate")}
              >
                Generate one
              </Button>
            </div>
          )}
          {!certsLoading && certs.length > 0 && (
            <div className="space-y-2">
              {certs.map((c) => (
                <CertRadio
                  key={c.id}
                  cert={c}
                  selected={selectedId === c.id}
                  onSelect={() => c.status === "ACTIVE" && setSelectedId(c.id)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Upload */}
        <div className="flex flex-col">
          <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-fg-3 mb-3">
            2 · Upload PDF
          </div>
          <div className="flex-1 [&>div]:h-full [&>div]:flex [&>div]:flex-col [&>div]:justify-center">
            <Dropzone
              file={file}
              onFile={handleFile}
              hint="Drop a PDF to sign"
              sub="up to 10 MB · application/pdf"
            />
          </div>
        </div>
      </div>

      {signError && (
        <div className="mt-6 px-4 py-3 rounded-md bg-bad-500/14 text-bad-500 text-[14px] flex items-start gap-2">
          <Icon name="alert" size={18} className="mt-0.5 shrink-0" />
          <span>{signError}</span>
        </div>
      )}

      <div className="mt-10 pt-6 border-t border-border-1 flex items-center justify-between gap-4 flex-wrap">
        <div className="text-[12px] text-fg-4 flex items-center gap-2">
          <Icon name="lock" size={14} />
          <span>Your private key never leaves the server.</span>
        </div>
        {downloadInfo ? (
          <div className="flex items-center gap-3">
            <Badge tone="ok" dot>
              Signed
            </Badge>
            <div className="text-[12px] text-fg-3 font-mono">
              {downloadInfo.name}
            </div>
          </div>
        ) : (
          <Button
            variant="primary"
            onClick={sign}
            disabled={!file || !selectedId || signing}
            iconRight={!signing ? "arrowRight" : null}
          >
            {signing ? "Signing…" : "Sign document"}
          </Button>
        )}
      </div>
    </main>
  );
}

function CertRadio({ cert, selected, onSelect }) {
  const usable = cert.status === "ACTIVE";
  const tone =
    cert.status === "ACTIVE"
      ? "ok"
      : cert.status === "EXPIRED"
        ? "warn"
        : "bad";
  const label =
    cert.status === "ACTIVE"
      ? "Active"
      : cert.status === "EXPIRED"
        ? "Expired"
        : "Revoked";
  const cn = extractFromDn(cert.subjectDn, "CN") ?? cert.subjectDn;

  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={!usable}
      className={`w-full text-left p-4 rounded-[10px] border transition-colors duration-150 ${
        selected
          ? "bg-accent-500/[0.08] border-accent-500"
          : usable
            ? "bg-surface-2 border-border-1 hover:border-border-2"
            : "bg-surface-2 border-border-1 opacity-50 cursor-not-allowed"
      }`}
    >
      <div className="flex items-center gap-2 mb-2">
        <div
          className={`w-3.5 h-3.5 rounded-full border-2 flex items-center justify-center ${
            selected ? "border-accent-500" : "border-border-2"
          }`}
        >
          {selected && <div className="w-1.5 h-1.5 rounded-full bg-accent-500" />}
        </div>
        <span className="text-[14px] font-semibold text-fg-1">
          Certificate #{cert.id}
        </span>
        <span className="ml-auto">
          <Badge tone={tone} dot>
            {label}
          </Badge>
        </span>
      </div>
      <div className="text-[12px] text-fg-2 font-medium">{cn}</div>
      <div className="text-[12px] text-fg-3 font-mono break-all mt-1">
        {cert.subjectDn}
      </div>
      <div className="text-[11px] text-fg-4 font-mono mt-1.5">
        {truncateSerial(cert.serialNumber)} · expires{" "}
        {formatDateOnly(cert.expiresAt)}
      </div>
    </button>
  );
}

function truncateSerial(serial) {
  if (!serial) return "";
  if (serial.length <= 20) return serial;
  return `${serial.slice(0, 17)}…`;
}

function parseFilename(contentDisposition, fallbackOriginalName) {
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/i);
    if (match) return match[1];
  }
  if (fallbackOriginalName) {
    const dot = fallbackOriginalName.lastIndexOf(".");
    const base =
      dot > 0 ? fallbackOriginalName.slice(0, dot) : fallbackOriginalName;
    return `${base}-signed.pdf`;
  }
  return "signed.pdf";
}

function triggerDownload(blob, name) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function readBlobError(err) {
  const data = err?.response?.data;
  if (data instanceof Blob) {
    try {
      const text = await data.text();
      const parsed = JSON.parse(text);
      if (parsed.message) return parsed.message;
    } catch {
      // not JSON; fall through
    }
  }
  return extractErrorMessage(err, "Signing failed");
}
