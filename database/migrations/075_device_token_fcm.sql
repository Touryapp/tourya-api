-- Migration 075 — MO-40 Fase A
-- Tabla para almacenar FCM/APNs tokens de dispositivos por usuario.
-- Un usuario puede tener multiples tokens (multiples dispositivos activos).
-- El mismo token no debe duplicarse (unique). Al hacer logout, la app lo borra.
-- Google puede rotar tokens sin aviso, por eso updated_at es clave para limpiar
-- los stale (job de mantenimiento futuro).

CREATE TABLE IF NOT EXISTS device_token (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL, -- 'ANDROID' | 'IOS' | 'WEB'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_device_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_device_token_user ON device_token (user_id);

COMMENT ON TABLE device_token IS 'MO-40: FCM/APNs push tokens registrados por los clientes mobile/web';
COMMENT ON COLUMN device_token.platform IS 'ANDROID | IOS | WEB — determina que canal de push usar';
COMMENT ON COLUMN device_token.updated_at IS 'Bumpea en cada login del mismo device — util para limpiar tokens viejos';
