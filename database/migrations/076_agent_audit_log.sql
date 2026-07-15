-- Migration 076 — IA-00
-- Tabla de auditoría obligatoria para acciones de agentes IA (RN futuro,
-- ver docs/16-agentes-ia.md Principio rector #4).
--
-- Cada llamada de un agente al LLM se registra aquí: prompt, modelo, tokens,
-- costo, entidad afectada, resultado. Sin FK cross-domain para no acoplar
-- audit con lifecycle de las entidades (si se borra una reservation, el log
-- de la interacción del agente sigue existiendo para forense).
--
-- Este es el prerequisito antes de dar autonomía a cualquier agente. IA-01
-- construye el AgentAuditWriter que escribe acá. IA-11 arma el dashboard.

CREATE TABLE IF NOT EXISTS agent_audit_log (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    prompt_version VARCHAR(20),
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    cost_usd NUMERIC(10, 6) NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    entity_type VARCHAR(30),
    entity_id BIGINT,
    user_id INTEGER,
    result_type VARCHAR(30) NOT NULL,
    result_json JSONB,
    prompt_input TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Dashboards por agente (uso, latencia, override rate) — el patrón de acceso
-- más frecuente será "últimas N corridas del agente X".
CREATE INDEX IF NOT EXISTS idx_agent_audit_log_agent_date
    ON agent_audit_log (agent_name, created_at DESC);

-- "¿Qué le pasó a la reserva 123 con el agente Support24x7?" — investigación
-- forense por entidad afectada.
CREATE INDEX IF NOT EXISTS idx_agent_audit_log_entity
    ON agent_audit_log (entity_type, entity_id)
    WHERE entity_id IS NOT NULL;

-- Historial de interacciones por usuario humano involucrado (turista/provider).
CREATE INDEX IF NOT EXISTS idx_agent_audit_log_user_date
    ON agent_audit_log (user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

COMMENT ON TABLE agent_audit_log IS 'IA-00: auditoría obligatoria de acciones de agentes IA (ver docs/16-agentes-ia.md Principio #4)';
COMMENT ON COLUMN agent_audit_log.agent_name IS 'Nombre del agente: TravelConcierge | Support24x7 | DesertCart | OperatorSupport | BackofficeSupport | ReviewModerator | ...';
COMMENT ON COLUMN agent_audit_log.model IS 'ID del modelo LLM usado: claude-sonnet-5 | claude-haiku-4-5 | claude-opus-4-8 | ...';
COMMENT ON COLUMN agent_audit_log.prompt_version IS 'Versión de la plantilla de prompt: v1 | v2 | ... — para A/B testing y rollback de prompts';
COMMENT ON COLUMN agent_audit_log.cost_usd IS 'Costo total del call en USD (input_tokens * price_in + output_tokens * price_out)';
COMMENT ON COLUMN agent_audit_log.entity_type IS 'Tipo de entidad afectada: reservation | tour | review | user | credit | ... — sin FK cross-domain intencional';
COMMENT ON COLUMN agent_audit_log.entity_id IS 'ID de la entidad afectada — BIGINT cubre long (reservation, payment) e int (tour, user)';
COMMENT ON COLUMN agent_audit_log.result_type IS 'Tipo de resultado: suggestion | autonomous_action | rejected | error';
COMMENT ON COLUMN agent_audit_log.result_json IS 'Output estructurado del LLM (jsonb — patrón consistente con wompi_webhook_event, tour.name)';
COMMENT ON COLUMN agent_audit_log.prompt_input IS 'Prompt real enviado al LLM (útil para debug + replay de casos ambiguos)';
