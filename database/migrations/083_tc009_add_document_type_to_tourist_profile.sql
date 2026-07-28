-- TC-009 (#196): agregar document_type al tourist_profile.
--
-- Antes: document_type solo vivia en payment.payer_document_type, se re-preguntaba
-- en cada compra. Ahora se persiste en el perfil para pre-llenar futuras compras.
--
-- Sin enum: permanece como varchar libre para compatibilidad con payment (que ya
-- usa varchar). Valores esperados de la UI: 'CC', 'CE', 'PP', 'NIT', 'TI'.

ALTER TABLE tourist_profile
    ADD COLUMN IF NOT EXISTS document_type varchar(50) NULL;

COMMENT ON COLUMN tourist_profile.document_type IS
    'TC-009: tipo de documento del turista (CC/CE/PP/NIT/TI). Se persiste para pre-llenar checkout.';
