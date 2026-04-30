CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name         VARCHAR(100) NOT NULL,
                       phone_number      VARCHAR(15)  NOT NULL UNIQUE,
                       email             VARCHAR(100) UNIQUE,
                       national_id       VARCHAR(20)  UNIQUE,
                       password_hash     VARCHAR(255) NOT NULL,
                       is_phone_verified BOOLEAN      DEFAULT FALSE,
                       is_active         BOOLEAN      DEFAULT TRUE,
                       profile_photo_url VARCHAR(500),
                       created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE chamas (
                        id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                        name                  VARCHAR(150)  NOT NULL,
                        description           TEXT,
                        registration_number   VARCHAR(50),
                        paybill_number        VARCHAR(20),
                        account_number        VARCHAR(50),
                        monthly_contribution  DECIMAL(12,2) NOT NULL,
                        contribution_day      INTEGER       NOT NULL CHECK (contribution_day BETWEEN 1 AND 28),
                        meeting_frequency     VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
                        loan_interest_rate    DECIMAL(5,2)  NOT NULL DEFAULT 10.00,
                        max_loan_multiplier   DECIMAL(4,1)  NOT NULL DEFAULT 3.0,
                        is_active             BOOLEAN       DEFAULT TRUE,
                        created_by            UUID          REFERENCES users(id),
                        created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
                        updated_at            TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE chama_members (
                               id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                               chama_id    UUID        NOT NULL REFERENCES chamas(id) ON DELETE CASCADE,
                               user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               role        VARCHAR(20) NOT NULL CHECK (role IN ('TREASURER','SECRETARY','MEMBER')),
                               joined_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
                               is_active   BOOLEAN     DEFAULT TRUE,
                               UNIQUE(chama_id, user_id)
);

CREATE TABLE otp_codes (
                           id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           phone_number VARCHAR(15) NOT NULL,
                           code         VARCHAR(6)  NOT NULL,
                           purpose      VARCHAR(20) NOT NULL CHECK (purpose IN ('REGISTER','LOGIN','RESET')),
                           is_used      BOOLEAN     DEFAULT FALSE,
                           expires_at   TIMESTAMP   NOT NULL,
                           created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);