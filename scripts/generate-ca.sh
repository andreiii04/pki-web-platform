#!/usr/bin/env bash
#
# Generates the Root Certificate Authority for the PKI platform.
# Output (in secrets/): ca-root.key (private key), ca-root.crt (certificate),
# ca-keystore.p12 (PKCS#12 bundle loaded by Spring Boot).
#
# Usage: ./scripts/generate-ca.sh
#
# The keystore password is prompted for interactively, unless the
# CA_KEYSTORE_PASSWORD environment variable is set (non-interactive use, e.g. CI).
#

set -euo pipefail

# Requires OpenSSL 3 (macOS ships LibreSSL as `openssl`). Resolution order:
# $OPENSSL, Homebrew openssl@3, then `openssl` on PATH.
if [[ -z "${OPENSSL:-}" ]]; then
  if [[ -x /opt/homebrew/opt/openssl@3/bin/openssl ]]; then
    OPENSSL=/opt/homebrew/opt/openssl@3/bin/openssl
  elif [[ -x /usr/local/opt/openssl@3/bin/openssl ]]; then
    OPENSSL=/usr/local/opt/openssl@3/bin/openssl
  else
    OPENSSL="$(command -v openssl || true)"
  fi
fi

if [[ -z "$OPENSSL" ]] || ! "$OPENSSL" version | grep -q '^OpenSSL 3'; then
  echo "OpenSSL 3 not found (got: ${OPENSSL:-none})."
  echo "macOS: brew install openssl@3   |   Debian/Ubuntu: apt install openssl"
  exit 1
fi

# Resolve the secrets/ folder relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SECRETS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)/secrets"

CA_KEY="$SECRETS_DIR/ca-root.key"
CA_CRT="$SECRETS_DIR/ca-root.crt"
CA_P12="$SECRETS_DIR/ca-keystore.p12"
CA_SUBJECT="/C=RO/O=PKI Platform Educational CA/OU=Root CA/CN=PKI Platform Root CA"
VALIDITY_DAYS=3650   # 10 years

mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"

if [[ -f "$CA_KEY" ]]; then
  echo "CA already exists at $CA_KEY"
  echo "To regenerate it (WARNING: invalidates every issued certificate), delete manually:"
  echo "  rm $SECRETS_DIR/ca-root.* $SECRETS_DIR/ca-keystore.p12"
  exit 1
fi

if [[ -n "${CA_KEYSTORE_PASSWORD:-}" ]]; then
  CA_PASS="$CA_KEYSTORE_PASSWORD"
else
  # Read the password without echoing it to the terminal
  read -rsp "CA private key password (min. 8 characters): " CA_PASS
  echo
  read -rsp "Confirm password: " CA_PASS_CONFIRM
  echo
  if [[ "$CA_PASS" != "$CA_PASS_CONFIRM" ]]; then
    echo "Passwords do not match."
    exit 1
  fi
fi

if [[ ${#CA_PASS} -lt 8 ]]; then
  echo "Password is too short. Minimum 8 characters."
  exit 1
fi

echo
echo "Generating RSA-4096 private key (may take a few seconds)..."
"$OPENSSL" genrsa -aes256 -passout pass:"$CA_PASS" -out "$CA_KEY" 4096

echo "Generating self-signed X.509 certificate..."
"$OPENSSL" req -x509 -new -key "$CA_KEY" -passin pass:"$CA_PASS" \
  -sha256 -days $VALIDITY_DAYS \
  -subj "$CA_SUBJECT" \
  -out "$CA_CRT" \
  -extensions v3_ca \
  -config <(cat <<'CFG'
[req]
distinguished_name = req_dn
[req_dn]
[v3_ca]
basicConstraints = critical, CA:TRUE
keyUsage = critical, keyCertSign, cRLSign
subjectKeyIdentifier = hash
CFG
)

echo "Creating PKCS#12 bundle for Spring Boot..."
"$OPENSSL" pkcs12 -export \
  -inkey "$CA_KEY" -in "$CA_CRT" \
  -name "pki-root-ca" \
  -passin pass:"$CA_PASS" \
  -passout pass:"$CA_PASS" \
  -out "$CA_P12"

# Restrict permissions (owner-only read)
chmod 600 "$CA_KEY" "$CA_P12"
chmod 644 "$CA_CRT"

echo
echo "CA generated successfully:"
echo "  $CA_KEY     (private key - keep secret)"
echo "  $CA_CRT     (public certificate - safe to distribute)"
echo "  $CA_P12     (bundle for Spring Boot)"
echo
echo "IMPORTANT: store the password in a password manager now."
echo "Set it in .env as CA_KEYSTORE_PASSWORD."
