-- Obliga cambio de contraseña en el primer acceso (p. ej. operadores con clave temporal del proveedor)
ALTER TABLE public._user
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public._user.must_change_password IS
    'Si true, el usuario debe cambiar contraseña (PATCH /users) antes de usar la app con normalidad.';
