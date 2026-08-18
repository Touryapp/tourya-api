package com.tourya.api.services.translation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IA-09: unit tests que ejercen el contrato {@link ITranslationService} contra
 * el mock. Los tests contra {@link GoogleCloudTranslationService} real requieren
 * ADC + red y NO se incluyen en la suite (Franklin los corre manual desde
 * Postman via {@code POST /admin/tours/{id}/retranslate}).
 *
 * <p>Cubre: happy path, disabled, API fallo (una vez), batch multi-idioma,
 * batch parcial cuando algunos fallan, y el mapeo de codigo pt -> pt-BR.</p>
 */
class TranslationServiceTest {

    @Test
    @DisplayName("translate returns translated text when service enabled")
    void translate_returnsTranslatedText_whenAvailable() {
        MockTranslationClient client = new MockTranslationClient();
        String out = client.translate("Tour por la bahia", "en");
        assertThat(out).isEqualTo("[en] Tour por la bahia");
        assertThat(client.capturedLangs()).containsExactly("en");
    }

    @Test
    @DisplayName("translate returns null when service disabled")
    void translate_returnsNull_whenDisabled() {
        MockTranslationClient client = new MockTranslationClient().disable();
        assertThat(client.translate("Texto", "en")).isNull();
        assertThat(client.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("translate returns null when API fails")
    void translate_returnsNull_whenApiFails() {
        MockTranslationClient client = new MockTranslationClient().failNext();
        assertThat(client.translate("Texto", "en")).isNull();
        // El proximo call recupera — el fail es one-shot como Google Translate transient.
        assertThat(client.translate("Texto", "en")).isEqualTo("[en] Texto");
    }

    @Test
    @DisplayName("translate returns null when text is blank")
    void translate_returnsNull_whenBlankText() {
        MockTranslationClient client = new MockTranslationClient();
        assertThat(client.translate(null, "en")).isNull();
        assertThat(client.translate("", "en")).isNull();
        assertThat(client.translate("   ", "en")).isNull();
    }

    @Test
    @DisplayName("translateBatch returns all target langs")
    void translateBatch_returnsAllTargetLangs() {
        MockTranslationClient client = new MockTranslationClient();
        Map<String, String> out = client.translateBatch("Tour por la bahia", List.of("en", "pt"));
        assertThat(out).hasSize(2);
        assertThat(out).containsEntry("en", "[en] Tour por la bahia");
        assertThat(out).containsEntry("pt", "[pt] Tour por la bahia");
    }

    @Test
    @DisplayName("translateBatch returns partial map when one lang fails")
    void translateBatch_returnsPartialWhenSomeFail() {
        MockTranslationClient client = new MockTranslationClient().failNext();
        Map<String, String> out = client.translateBatch("Tour", List.of("en", "pt"));
        // El primero (en) fallo silenciosamente — solo aparece pt en el map.
        assertThat(out).hasSize(1);
        assertThat(out).containsOnlyKeys("pt");
    }

    @Test
    @DisplayName("translateBatch returns empty map when service disabled")
    void translateBatch_returnsEmpty_whenDisabled() {
        MockTranslationClient client = new MockTranslationClient().disable();
        Map<String, String> out = client.translateBatch("Tour", List.of("en", "pt"));
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("translateBatch returns empty map when targetLangs empty")
    void translateBatch_returnsEmpty_whenEmptyLangs() {
        MockTranslationClient client = new MockTranslationClient();
        assertThat(client.translateBatch("Tour", List.of())).isEmpty();
        assertThat(client.translateBatch("Tour", null)).isEmpty();
    }

    @Test
    @DisplayName("Google lang code mapper: pt -> pt-BR (portugues de Brasil)")
    void googleLangMapper_ptToPtBR() {
        // Verificamos la conversion que GoogleCloudTranslationService.toGoogleLangCode
        // aplica antes de llamar al API. La convencion Tourya-side es 'pt' en el
        // JSONB; Luis pidio Brasil-especifico via la propuesta traduccion-automatica-tours.md.
        assertThat(GoogleCloudTranslationService.toGoogleLangCode("pt")).isEqualTo("pt-BR");
        assertThat(GoogleCloudTranslationService.toGoogleLangCode("PT")).isEqualTo("pt-BR");
        assertThat(GoogleCloudTranslationService.toGoogleLangCode("en")).isEqualTo("en");
        assertThat(GoogleCloudTranslationService.toGoogleLangCode("EN")).isEqualTo("en");
        assertThat(GoogleCloudTranslationService.toGoogleLangCode(null)).isNull();
    }
}
