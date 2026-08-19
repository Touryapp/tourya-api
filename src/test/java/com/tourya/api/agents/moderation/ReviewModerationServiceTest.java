package com.tourya.api.agents.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.agents.shared.AgentAuditEntry;
import com.tourya.api.agents.shared.AgentAuditWriter;
import com.tourya.api.agents.shared.BudgetGuard;
import com.tourya.api.agents.shared.MockLlmClient;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Review;
import com.tourya.api.models.ReviewAttachment;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.models.User;
import com.tourya.api.repository.ReviewAttachmentRepository;
import com.tourya.api.repository.ReviewRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IA-10: unit tests para {@link ReviewModerationService}.
 *
 * <p>Cubre veredictos + guardrails + contract "nunca modifica el texto":</p>
 * <ol>
 *   <li>APPROVED cuando LLM devuelve reseña limpia.</li>
 *   <li>REJECTED con flag SPAM cuando LLM detecta promocional.</li>
 *   <li>PENDING con flag OFFENSIVE cuando LLM detecta insulto.</li>
 *   <li>PENDING con flag OFF_TOPIC cuando la reseña no habla del tour.</li>
 *   <li>PENDING escalado cuando el LLM devuelve JSON malformado.</li>
 *   <li>El resultado se persiste via applier — comment nunca cambia.</li>
 *   <li>El texto original de la reseña nunca se modifica.</li>
 *   <li>Deny-list de secretos NO llama al LLM y escala.</li>
 *   <li>Budget exhausto NO llama al LLM y escala.</li>
 *   <li>Modo no-op (feature flag) NO llama al LLM y escala.</li>
 *   <li>Contradiccion APPROVED + flags fuerza PENDING.</li>
 *   <li>moderate() sync throws ResourceNotFoundException si no existe.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ReviewModerationServiceTest {

    @Mock private AgentAuditWriter auditWriter;
    @Mock private BudgetGuard budgetGuard;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewAttachmentRepository reviewAttachmentRepository;
    @Mock private TourRepository tourRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewModerationApplier applier;

    private MockLlmClient llmClient;
    private ObjectMapper objectMapper;
    private ReviewModerationService service;

    private Review sampleReview;
    private Tour sampleTour;
    private User sampleAuthor;

    @BeforeEach
    void setUp() {
        llmClient = new MockLlmClient();
        objectMapper = new ObjectMapper();
        lenient().when(budgetGuard.canRun(anyString())).thenReturn(true);
        lenient().when(reviewAttachmentRepository.findByReviewId(anyLong())).thenReturn(List.of());

        service = new ReviewModerationService(
                llmClient, auditWriter, budgetGuard,
                reviewRepository, reviewAttachmentRepository,
                tourRepository, userRepository,
                applier, objectMapper,
                /* enabled */ true,
                "gemini-2.5-flash");

        // Reseña sample: rating 4, comment sobre snorkel.
        Provider provider = new Provider();
        provider.setId(100);
        provider.setName("Cayo Bolivar Tours");

        sampleTour = new Tour();
        sampleTour.setId(42);
        sampleTour.setName(new TranslatedField("Tour Cayo Bolivar", null, null));
        sampleTour.setSubCategory(TourSubCategoryEnum.SNORKELING);
        sampleTour.setProvider(provider);

        sampleAuthor = User.builder().id(5).email("alice@gmail.com").build();

        sampleReview = new Review();
        sampleReview.setId(9L);
        sampleReview.setTourId(42);
        sampleReview.setUserId(5);
        sampleReview.setRating(new BigDecimal("4.5"));
        sampleReview.setComment(new TranslatedField(
                "Excelente tour de snorkel en Cayo Bolivar. El guia super profesional.",
                null, null));

        lenient().when(reviewRepository.findById(9L)).thenReturn(Optional.of(sampleReview));
        lenient().when(tourRepository.findById(42)).thenReturn(Optional.of(sampleTour));
        lenient().when(userRepository.findById(5)).thenReturn(Optional.of(sampleAuthor));
    }

    // ================================================================
    // 1. APPROVED — happy path
    // ================================================================

    @Test
    @DisplayName("moderate devuelve APPROVED cuando la reseña esta limpia")
    void moderate_returnsApproved_whenReviewIsClean() {
        llmClient.enqueueText("""
                {
                  "decision": "APPROVED",
                  "flags": [],
                  "reasoning": "Reseña positiva y descriptiva, sin señales de fraude."
                }
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.APPROVED, result.decision());
        assertTrue(result.flags().isEmpty());
        assertFalse(result.escalatedToHuman());
        verify(applier).applySync(eq(9L), any(ModerationResult.class), anyString());
    }

    // ================================================================
    // 2. SPAM
    // ================================================================

    @Test
    @DisplayName("moderate devuelve REJECTED con flag SPAM cuando LLM detecta promocional")
    void moderate_flagsSpam_whenLLMDetectsPromotional() {
        llmClient.enqueueText("""
                {
                  "decision": "REJECTED",
                  "flags": ["SPAM"],
                  "reasoning": "El comentario incluye un numero de whatsapp y URL a otro tour."
                }
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.REJECTED, result.decision());
        assertEquals(1, result.flags().size());
        assertEquals(ModerationResult.Flag.SPAM, result.flags().get(0));
        assertFalse(result.escalatedToHuman());
    }

    // ================================================================
    // 3. OFFENSIVE
    // ================================================================

    @Test
    @DisplayName("moderate devuelve PENDING con flag OFFENSIVE cuando LLM detecta insulto")
    void moderate_flagsOffensive_whenLLMDetectsInsult() {
        llmClient.enqueueText("""
                {
                  "decision": "PENDING",
                  "flags": ["OFFENSIVE"],
                  "reasoning": "El comentario contiene un insulto al guia; el backoffice decide."
                }
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.flags().contains(ModerationResult.Flag.OFFENSIVE));
        assertFalse(result.escalatedToHuman());
    }

    // ================================================================
    // 4. OFF_TOPIC
    // ================================================================

    @Test
    @DisplayName("moderate devuelve PENDING con flag OFF_TOPIC cuando la reseña no habla del tour")
    void moderate_flagsOffTopic_whenReviewNotAboutTour() {
        llmClient.enqueueText("""
                {
                  "decision": "PENDING",
                  "flags": ["OFF_TOPIC"],
                  "reasoning": "La reseña se queja del clima; no aporta info del tour."
                }
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.flags().contains(ModerationResult.Flag.OFF_TOPIC));
    }

    // ================================================================
    // 5. LLM incierto — malformed JSON
    // ================================================================

    @Test
    @DisplayName("moderate devuelve PENDING escalado cuando el LLM responde con JSON invalido")
    void moderate_returnsPending_whenLlmUncertain() {
        llmClient.enqueueText("no soy JSON, soy texto libre porque el LLM se equivoco");

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());
        assertNotNull(result.reasoning());
    }

    // ================================================================
    // 6. Persistencia via applier
    // ================================================================

    @Test
    @DisplayName("moderate persiste el resultado via ReviewModerationApplier con JSON de flags")
    void moderate_persistsResultToReview_viaApplier() {
        llmClient.enqueueText("""
                {
                  "decision": "REJECTED",
                  "flags": ["SPAM", "POTENTIAL_FRAUD"],
                  "reasoning": "Whatsapp + rating 5 + email descartable."
                }
                """);

        service.moderate(9L);

        ArgumentCaptor<ModerationResult> resultCap = ArgumentCaptor.forClass(ModerationResult.class);
        ArgumentCaptor<String> flagsJsonCap = ArgumentCaptor.forClass(String.class);
        verify(applier).applySync(eq(9L), resultCap.capture(), flagsJsonCap.capture());

        ModerationResult captured = resultCap.getValue();
        assertEquals(ModerationResult.Decision.REJECTED, captured.decision());
        assertEquals(2, captured.flags().size());

        // El JSON persistido debe ser un array parseable con exactamente los 2 flags.
        String json = flagsJsonCap.getValue();
        assertTrue(json.contains("SPAM"));
        assertTrue(json.contains("POTENTIAL_FRAUD"));
        assertTrue(json.startsWith("["));
    }

    // ================================================================
    // 7. El texto original de la reseña nunca cambia
    // ================================================================

    @Test
    @DisplayName("moderate NUNCA modifica el texto de la reseña (comment queda intacto)")
    void moderate_neverModifiesReviewText() {
        String originalText = sampleReview.getComment().getEs();
        llmClient.enqueueText("""
                {"decision": "APPROVED", "flags": [], "reasoning": "OK"}
                """);

        service.moderate(9L);

        // El service NO debe haber llamado a save() sobre la reseña directamente
        // — el applier es el unico canal, y esta mockeado. El comment original
        // queda intacto en la referencia sample.
        assertEquals(originalText, sampleReview.getComment().getEs());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // ================================================================
    // 8. Deny-list secretos
    // ================================================================

    @Test
    @DisplayName("guardrail: deny-list secretos escapa a humano y NO llama al LLM")
    void guardrail_denyListSecrets_escapesHumanAndDoesNotCallLlm() {
        sampleReview.setComment(new TranslatedField(
                "Mira mi WOMPI_INTEGRITY_SECRET = 12345", null, null));

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());
        assertTrue(llmClient.capturedPrompts().isEmpty(), "El LLM no debe haberse llamado");

        // Se auditea como rejected con error específico.
        ArgumentCaptor<AgentAuditEntry> auditCap = ArgumentCaptor.forClass(AgentAuditEntry.class);
        verify(auditWriter).register(auditCap.capture());
        assertEquals("rejected", auditCap.getValue().resultType());
        assertEquals("secret_keyword_in_review", auditCap.getValue().errorMessage());
    }

    // ================================================================
    // 9. Budget exhausto
    // ================================================================

    @Test
    @DisplayName("guardrail: budget exhausto escapa a humano y NO llama al LLM")
    void guardrail_budgetExhausted_escapesHumanAndDoesNotCallLlm() {
        when(budgetGuard.canRun("ReviewModerator")).thenReturn(false);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());
        assertTrue(llmClient.capturedPrompts().isEmpty());

        ArgumentCaptor<AgentAuditEntry> auditCap = ArgumentCaptor.forClass(AgentAuditEntry.class);
        verify(auditWriter).register(auditCap.capture());
        assertEquals("budget_exhausted", auditCap.getValue().errorMessage());
    }

    // ================================================================
    // 10. Modo no-op (feature flag)
    // ================================================================

    @Test
    @DisplayName("modo no-op (enabled=false) escapa a humano sin llamar al LLM ni al applier")
    void featureFlag_disabled_escapesWithoutLlmOrApplier() {
        ReviewModerationService disabledService = new ReviewModerationService(
                llmClient, auditWriter, budgetGuard,
                reviewRepository, reviewAttachmentRepository,
                tourRepository, userRepository,
                applier, objectMapper,
                /* enabled */ false, "gemini-2.5-flash");

        ModerationResult result = disabledService.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());
        assertTrue(llmClient.capturedPrompts().isEmpty());
        // applier.applySync SI se llama en el flujo sync — persiste PENDING
        // para reflejar que el agente corrio (aunque escaladamente). Es
        // consistente con dejar registro de "se intento moderar".
        verify(applier).applySync(eq(9L), any(ModerationResult.class), anyString());
    }

    // ================================================================
    // 11. Contradiccion APPROVED + flags
    // ================================================================

    @Test
    @DisplayName("contradiccion APPROVED con flags fuerza PENDING escalado")
    void moderate_forcePending_whenLlmContradictionApprovedWithFlags() {
        llmClient.enqueueText("""
                {"decision": "APPROVED", "flags": ["SPAM"], "reasoning": "contradictorio"}
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());
        assertEquals(1, result.flags().size());
        assertEquals(ModerationResult.Flag.SPAM, result.flags().get(0));
    }

    // ================================================================
    // 12. Not found
    // ================================================================

    @Test
    @DisplayName("moderate throws ResourceNotFoundException cuando la reseña no existe")
    void moderate_throwsNotFound_whenReviewMissing() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.moderate(999L));
        verify(applier, never()).applySync(anyLong(), any(), anyString());
    }

    // ================================================================
    // 13. Media urls pasan al request cuando hay attachments
    // ================================================================

    @Test
    @DisplayName("moderate incluye los URLs de attachments en la request pero no en el flag por defecto")
    void moderate_includesAttachmentUrls_butNoInappropriateMediaWithoutEvidence() {
        ReviewAttachment att = ReviewAttachment.builder()
                .reviewId(9L)
                .fileUrl("https://s3.amazonaws.com/reviews/9/photo1.jpg")
                .build();
        when(reviewAttachmentRepository.findByReviewId(9L)).thenReturn(List.of(att));

        // LLM devuelve APPROVED — el prompt le dice explicitamente que no use
        // INAPPROPRIATE_MEDIA sin evidencia textual.
        llmClient.enqueueText("""
                {"decision": "APPROVED", "flags": [], "reasoning": "OK, foto normal"}
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.APPROVED, result.decision());
        assertTrue(result.flags().isEmpty());
        // El prompt renderizado debe mencionar mediaCount = 1.
        String prompt = llmClient.lastPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("numero de fotos adjuntas: 1"));
    }

    // ================================================================
    // 14. Listener AFTER_COMMIT — verificacion estructural
    // ================================================================

    @Test
    @DisplayName("listener corre en AFTER_COMMIT (verificacion via anotacion de la clase)")
    void listener_firesOnAfterCommit_notBeforeCommit() throws NoSuchMethodException {
        // Verificacion estructural: la anotacion @TransactionalEventListener del
        // metodo on() del listener DEBE tener phase=AFTER_COMMIT — asi
        // garantizamos que la reseña esta persistida antes de disparar el agente.
        var method = ReviewModerationEventListener.class.getMethod(
                "on", ReviewModerationEvent.class);
        var annotation = method.getAnnotation(
                org.springframework.transaction.event.TransactionalEventListener.class);
        assertNotNull(annotation, "El listener DEBE llevar @TransactionalEventListener");
        assertEquals(org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT,
                annotation.phase(),
                "El listener DEBE correr AFTER_COMMIT — evita moderar reseñas de tx en rollback");
    }

    // ================================================================
    // 15. LLM error path
    // ================================================================

    @Test
    @DisplayName("moderate devuelve PENDING escalado cuando el LLM responde error")
    void moderate_returnsPendingEscalated_whenLlmErrors() {
        llmClient.enqueueError("rate_limit");

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.PENDING, result.decision());
        assertTrue(result.escalatedToHuman());

        ArgumentCaptor<AgentAuditEntry> auditCap = ArgumentCaptor.forClass(AgentAuditEntry.class);
        verify(auditWriter).register(auditCap.capture());
        assertEquals("error", auditCap.getValue().resultType());
        assertEquals("rate_limit", auditCap.getValue().errorMessage());
    }

    // ================================================================
    // 16. Sanitize flags fuera del dominio se descartan
    // ================================================================

    @Test
    @DisplayName("moderate ignora flags fuera del dominio conocido (defensivo)")
    void moderate_ignoresUnknownFlags() {
        llmClient.enqueueText("""
                {
                  "decision": "REJECTED",
                  "flags": ["SPAM", "HATE_SPEECH", "MADE_UP_FLAG"],
                  "reasoning": "SPAM real, resto inventado"
                }
                """);

        ModerationResult result = service.moderate(9L);

        assertEquals(ModerationResult.Decision.REJECTED, result.decision());
        // Solo SPAM sobrevive el sanitize.
        assertEquals(1, result.flags().size());
        assertEquals(ModerationResult.Flag.SPAM, result.flags().get(0));
    }

    // ================================================================
    // 17. flagsToJsonString empty
    // ================================================================

    @Test
    @DisplayName("flagsToJsonString serializa lista vacia como []")
    void flagsToJsonString_emptyList_returnsEmptyArray() {
        assertEquals("[]", service.flagsToJsonString(List.of()));
        assertEquals("[]", service.flagsToJsonString(null));
    }
}
