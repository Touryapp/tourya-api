-- Migration 092 — IA-02
-- Agrega columna metadata JSONB al agent_audit_log para que el Travel
-- Concierge (y proximos agentes) persistan info libre de la sesion:
--   * session_id: identifica la conversacion multi-turno del turista
--   * fraud_suspected: bandera puesta por el guardrail de 3+ pagos fallidos
--     (nunca se muestra al turista — solo queda en el log para operaciones)
--   * actions_executed[]: lista de function calls que el agente disparo
--     (search_tours, get_tour_detail, add_to_cart, get_cart)
--   * escalated_to_human: cuando el guardrail (secretos, incidente) fuerza
--     escalamiento en vez de responder
--
-- Consistente con el patron de resultJson (jsonb, sin esquema forzado). Cada
-- agente futuro define su propio shape en metadata sin tocar la tabla.
--
-- NO se aplica a Cloud SQL dev aca — Franklin la corre con el patron
-- docker+psql documentado. Idempotente via IF NOT EXISTS.

ALTER TABLE agent_audit_log
    ADD COLUMN IF NOT EXISTS metadata JSONB;

COMMENT ON COLUMN agent_audit_log.metadata IS
    'IA-02: metadata libre por agente (session_id, fraud_suspected, actions_executed[], escalated_to_human, ...). Sin esquema forzado — cada agente define el shape.';

-- Indice GIN sobre metadata para permitir queries como:
--   SELECT * FROM agent_audit_log WHERE metadata @> '{"fraud_suspected": true}'
--   SELECT * FROM agent_audit_log WHERE metadata->>'session_id' = 'abc-123'
-- No es critico para el volumen inicial pero cuesta poco y evita retrofit.
CREATE INDEX IF NOT EXISTS idx_agent_audit_log_metadata
    ON agent_audit_log USING GIN (metadata);
