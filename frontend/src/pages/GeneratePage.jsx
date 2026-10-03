import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import Badge from "../components/Badge";
import Button from "../components/Button";
import Card from "../components/Card";
import Icon from "../components/Icon";
import Input from "../components/Input";
import { useAuth } from "../context/AuthContext";
import { extractErrorMessage, formatInstant } from "../utils/format";

export default function GeneratePage({ onRequireAuth }) {
  const { user } = useAuth();
  const [commonName, setCommonName] = useState("");
  const [organization, setOrganization] = useState("");
  const [issued, setIssued] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const submit = async () => {
    setError(null);
    if (!user) {
      onRequireAuth?.();
      return;
    }
    if (!commonName.trim()) return;

    setLoading(true);
    try {
      const payload = {
        commonName: commonName.trim(),
        ...(organization.trim() ? { organization: organization.trim() } : {}),
      };
      const { data } = await api.post("/api/certificates/generate", payload);
      setIssued(data);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not issue certificate"));
    } finally {
      setLoading(false);
    }
  };

  if (issued) {
    return (
      <Success
        cert={issued}
        onReset={() => {
          setIssued(null);
          setCommonName("");
          setOrganization("");
        }}
      />
    );
  }

  return (
    <main className="max-w-[1200px] mx-auto px-6 py-12">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 items-start lg:items-center">
        <div className="max-w-[480px]">
          <h1 className="text-[36px] font-semibold tracking-[-0.02em] text-fg-1">
            Generate a certificate
          </h1>
          <p className="mt-3 text-[15px] text-fg-3 leading-[1.6]">
            The platform CA issues an X.509 v3 certificate bound to your account. Your
            private key is generated server-side and stored encrypted.
          </p>
          {!user && (
            <p className="mt-4 text-[13px] text-fg-4">
              You need to be logged in to generate a certificate.
            </p>
          )}
        </div>

        <Card className="p-7">
          <div className="space-y-5">
            <Input
              label="Full name"
              required
              placeholder="e.g. Jane Doe"
              value={commonName}
              onChange={(e) => setCommonName(e.target.value)}
              helper="Becomes the Common Name (CN) on your certificate. 1–100 chars."
              maxLength={100}
            />
            <Input
              label="Organization"
              placeholder="PKI Web Platform Demo (default)"
              value={organization}
              onChange={(e) => setOrganization(e.target.value)}
              helper="Optional. Becomes the O field. Leave blank to use the default."
              maxLength={100}
            />
          </div>

          {error && (
            <div className="mt-5 px-3 py-2 rounded-md bg-bad-500/14 text-bad-500 text-[13px]">
              {error}
            </div>
          )}

          <div className="mt-7 pt-5 border-t border-border-1 flex items-center justify-between">
            <div className="flex items-center gap-2 text-[12px] text-fg-4">
              <Icon name="lock" size={14} />
              <span>RSA-2048 · SHA-256 · 365 days validity</span>
            </div>
            <Button
              variant="primary"
              onClick={submit}
              disabled={loading || !commonName.trim()}
            >
              {loading
                ? "Issuing…"
                : user
                  ? "Generate certificate"
                  : "Log in to continue"}
            </Button>
          </div>
        </Card>
      </div>
    </main>
  );
}

function Success({ cert, onReset }) {
  const navigate = useNavigate();
  const statusTone =
    cert.status === "ACTIVE"
      ? "ok"
      : cert.status === "EXPIRED"
        ? "warn"
        : "bad";
  const statusLabel =
    cert.status === "ACTIVE"
      ? "Active"
      : cert.status === "EXPIRED"
        ? "Expired"
        : "Revoked";

  return (
    <main className="max-w-[640px] mx-auto px-6 py-16">
      <div className="flex items-center gap-3 mb-7">
        <div className="w-10 h-10 rounded-full bg-ok-500/14 text-ok-500 flex items-center justify-center">
          <Icon name="check" size={20} strokeWidth={2} />
        </div>
        <div>
          <h1 className="text-[24px] font-semibold tracking-[-0.02em] text-fg-1">
            Certificate issued
          </h1>
          <p className="text-[13px] text-fg-3">
            Stored against your account. You can use it to sign now.
          </p>
        </div>
      </div>

      <Card className="p-6">
        <div className="flex items-center justify-between mb-5">
          <div className="flex items-center gap-2">
            <Icon name="key" size={16} className="text-accent-300" />
            <span className="text-[14px] font-semibold text-fg-1">
              Certificate #{cert.id}
            </span>
          </div>
          <Badge tone={statusTone} dot>
            {statusLabel}
          </Badge>
        </div>
        <dl className="grid grid-cols-[140px_1fr] gap-y-3 gap-x-4 text-[13px]">
          <dt className="text-fg-3">Serial</dt>
          <dd className="text-fg-1 font-mono text-[12px] break-all">
            {cert.serialNumber}
          </dd>
          <dt className="text-fg-3">Subject</dt>
          <dd className="text-fg-1 font-mono text-[12px] break-all">
            {cert.subjectDn}
          </dd>
          <dt className="text-fg-3">Issuer</dt>
          <dd className="text-fg-1 font-mono text-[12px] break-all">
            {cert.issuerDn}
          </dd>
          <dt className="text-fg-3">Issued</dt>
          <dd className="text-fg-1 font-mono text-[12px]">
            {formatInstant(cert.issuedAt)}
          </dd>
          <dt className="text-fg-3">Expires</dt>
          <dd className="text-fg-1 font-mono text-[12px]">
            {formatInstant(cert.expiresAt)}
          </dd>
        </dl>
      </Card>

      <div className="mt-6 flex items-center gap-3">
        <Button
          variant="primary"
          iconRight="arrowRight"
          onClick={() => navigate("/sign")}
        >
          Sign a document
        </Button>
        <Button variant="ghost" onClick={onReset}>
          Issue another
        </Button>
      </div>
    </main>
  );
}
