-- ============================================================
--  PKI Web Platform — Initial Schema (V1)
--  Creates the core tables: users, certificates
--  Plus: native PostgreSQL enums, indexes, constraints
-- ============================================================

-- ------------------------------------------------------------
--  Enum: role of a user on the platform
-- ------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- ------------------------------------------------------------
--  Enum: status of an X.509 certificate
--  ACTIVE  - valid certificate, within its validity period
--  EXPIRED - the expiry date has passed
--  REVOKED - revoked before expiry (compromised or lost key)
-- ------------------------------------------------------------
CREATE TYPE certificate_status AS ENUM ('ACTIVE', 'EXPIRED', 'REVOKED');

-- ------------------------------------------------------------
--  Table: users
--  Stores user accounts (username + password hash)
-- ------------------------------------------------------------
CREATE TABLE users (
                       id              BIGSERIAL       PRIMARY KEY,
                       username        VARCHAR(50)     NOT NULL UNIQUE,
                       email           VARCHAR(255)    NOT NULL UNIQUE,
                       password_hash   VARCHAR(255)    NOT NULL,
                       role            user_role       NOT NULL DEFAULT 'USER',
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email    ON users(email);

COMMENT ON TABLE users IS 'Registered users of the PKI platform';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, never the plaintext password';

-- ------------------------------------------------------------
--  Table: certificates
--  Stores X.509 certificates issued by the internal CA
-- ------------------------------------------------------------
CREATE TABLE certificates (
                              id                      BIGSERIAL           PRIMARY KEY,
                              user_id                 BIGINT              NOT NULL,
                              serial_number           VARCHAR(64)         NOT NULL UNIQUE,
                              subject_dn              VARCHAR(512)        NOT NULL,
                              issuer_dn               VARCHAR(512)        NOT NULL,
                              public_key_pem          TEXT                NOT NULL,
                              encrypted_private_key   TEXT                NOT NULL,
                              certificate_pem         TEXT                NOT NULL,
                              status                  certificate_status  NOT NULL DEFAULT 'ACTIVE',
                              issued_at               TIMESTAMPTZ         NOT NULL,
                              expires_at              TIMESTAMPTZ         NOT NULL,
                              revoked_at              TIMESTAMPTZ,
                              revocation_reason       VARCHAR(255),
                              created_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
                              updated_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

                              CONSTRAINT fk_certificates_user
                                  FOREIGN KEY (user_id) REFERENCES users(id)
                                      ON DELETE RESTRICT,

                              CONSTRAINT chk_certificates_dates
                                  CHECK (expires_at > issued_at),

                              CONSTRAINT chk_certificates_revocation
                                  CHECK (
                                      (status = 'REVOKED' AND revoked_at IS NOT NULL) OR
                                      (status <> 'REVOKED' AND revoked_at IS NULL)
                                      )
);

CREATE INDEX idx_certificates_user_id       ON certificates(user_id);
CREATE INDEX idx_certificates_serial_number ON certificates(serial_number);
CREATE INDEX idx_certificates_status        ON certificates(status);
CREATE INDEX idx_certificates_expires_at    ON certificates(expires_at);

COMMENT ON TABLE certificates IS 'X.509 digital certificates issued by the private CA';
COMMENT ON COLUMN certificates.serial_number IS 'Unique serial number, cryptographically generated at issuance';
COMMENT ON COLUMN certificates.subject_dn IS 'Subject Distinguished Name (CN, O, C)';
COMMENT ON COLUMN certificates.encrypted_private_key IS 'RSA private key encrypted with AES-256-GCM; master key supplied via environment';
COMMENT ON COLUMN certificates.certificate_pem IS 'Full certificate in PEM format (Base64 + headers)';