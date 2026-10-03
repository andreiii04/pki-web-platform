export function formatInstant(value, { withSeconds = true } = {}) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  const yyyy = d.getUTCFullYear();
  const mm = pad(d.getUTCMonth() + 1);
  const dd = pad(d.getUTCDate());
  const hh = pad(d.getUTCHours());
  const mi = pad(d.getUTCMinutes());
  const ss = pad(d.getUTCSeconds());
  return withSeconds
    ? `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss} UTC`
    : `${yyyy}-${mm}-${dd} ${hh}:${mi} UTC`;
}

export function formatDateOnly(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;
}

export function extractFromDn(dn, key) {
  if (!dn) return null;
  const match = dn.match(new RegExp(`(?:^|,\\s*)${key}=([^,]+)`, "i"));
  return match ? match[1].trim() : null;
}

export function extractErrorMessage(err, fallback = "Something went wrong") {
  if (!err) return fallback;
  const data = err.response?.data;
  if (data && typeof data === "object" && data.message) return data.message;
  if (typeof data === "string" && data.trim()) return data;
  if (err.message) return err.message;
  return fallback;
}
