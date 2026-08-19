-- Migration 093 — IA-10 Review Moderation
--
-- Agrega columnas de moderacion a la tabla `review` para persistir el resultado
-- del agente 6 (moderacion de resenas). RN-050: hoy las resenas se publican con
-- status=PUBLISHED sin bloquear (autoriza el turista, no el marketplace); el
-- agente NO cambia el flow — solo *flaggea* para que backoffice pueda filtrar.
--
-- Columnas nuevas:
--   * moderation_status   VARCHAR(20)  -> APPROVED | PENDING | REJECTED (NULL = no
--                                        moderado aun; reseñas legacy antes de IA-10)
--   * moderation_flags    JSONB        -> array libre de strings, dominio:
--                                        SPAM | OFFENSIVE | OFF_TOPIC |
--                                        POTENTIAL_FRAUD | INAPPROPRIATE_MEDIA
--   * moderation_reasoning TEXT        -> explicacion corta que el LLM devolvio
--                                        (max 500 chars por el prompt; sin CHECK
--                                        para no forzar refactor si se relaja)
--   * moderated_at        TIMESTAMP    -> cuando corrio el agente (independiente de
--                                        created_at/last_modified_date del review)
--
-- Sin CHECK constraint sobre moderation_status (el proyecto mapea otros enums
-- con VARCHAR + @Enumerated(EnumType.STRING) y no usa CHECK — coherente).
--
-- Indice parcial sobre moderation_status (WHERE IS NOT NULL) para que el filtro
-- del backoffice (GET /admin/reviews/moderation?status=...) sea barato. GIN
-- sobre moderation_flags para queries como
--   SELECT * FROM review WHERE moderation_flags @> '["SPAM"]'
-- (uso post-MVP; cuesta poco y evita retrofit).
--
-- NO se aplica a Cloud SQL dev aca — Franklin la corre con el patron
-- docker+psql documentado. Idempotente via IF NOT EXISTS.

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS moderation_flags JSONB NULL,
    ADD COLUMN IF NOT EXISTS moderation_reasoning TEXT NULL,
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP NULL;

COMMENT ON COLUMN review.moderation_status IS
    'IA-10: APPROVED | PENDING | REJECTED — resultado del agente 6 (moderacion). NULL = no moderado (reseñas legacy o agente apagado).';
COMMENT ON COLUMN review.moderation_flags IS
    'IA-10: array JSON de flags: SPAM | OFFENSIVE | OFF_TOPIC | POTENTIAL_FRAUD | INAPPROPRIATE_MEDIA. NULL o [] = sin flags.';
COMMENT ON COLUMN review.moderation_reasoning IS
    'IA-10: explicacion corta que el LLM produjo (max 500 chars). Solo para revision humana en backoffice.';
COMMENT ON COLUMN review.moderated_at IS
    'IA-10: cuando corrio el agente 6 (independiente de created_at / last_modified_date del review).';

CREATE INDEX IF NOT EXISTS idx_review_moderation_status
    ON review (moderation_status)
    WHERE moderation_status IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_review_moderation_flags
    ON review USING GIN (moderation_flags);
