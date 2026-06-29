-- Usuarios
CREATE TABLE app_user (
  id           BIGINT IDENTITY PRIMARY KEY,
  username     VARCHAR(100) UNIQUE NOT NULL,
  password     VARCHAR(100) NOT NULL,        -- hash BCrypt, ej. $2a$10$...
  enabled      BIT NOT NULL DEFAULT 1,
  locked       BIT NOT NULL DEFAULT 0,
  created_at   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  updated_at   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Roles
CREATE TABLE app_role (
  id      BIGINT IDENTITY PRIMARY KEY,
  name    VARCHAR(50) UNIQUE NOT NULL        -- ej: ROLE_ADMIN, ROLE_VENDEDOR
);

-- Relación N:M
CREATE TABLE app_user_roles (
  user_id BIGINT NOT NULL REFERENCES app_user(id),
  role_id BIGINT NOT NULL REFERENCES app_role(id),
  CONSTRAINT PK_app_user_roles PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens con rotación
CREATE TABLE app_refresh_token (
  id           BIGINT IDENTITY PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES app_user(id),
  token_hash   VARCHAR(200) NOT NULL,        -- hash del refresh (no guardes el valor plano)
  expires_at   DATETIME2 NOT NULL,
  revoked      BIT NOT NULL DEFAULT 0,
  created_at   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  UNIQUE(user_id, token_hash)
);

insert into app_role values ('ROLE_ADMIN');
insert into app_role values ('ROLE_VENDEDOR');