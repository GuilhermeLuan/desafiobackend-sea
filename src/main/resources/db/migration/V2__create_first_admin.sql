CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO tb_users (email, password_hash, role)
VALUES ('${admin_email}', crypt('${admin_password}', gen_salt('bf', 10)), 'ADMIN')
ON CONFLICT (email) DO NOTHING;
