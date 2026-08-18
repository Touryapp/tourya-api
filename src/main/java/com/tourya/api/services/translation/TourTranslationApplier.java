package com.tourya.api.services.translation;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.TourFaq;
import com.tourya.api.models.TourGallery;
import com.tourya.api.models.TourIncludesExcludes;
import com.tourya.api.models.TourItinerary;
import com.tourya.api.models.TourMainAttraction;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourCancellationPolicyRepository;
import com.tourya.api.repository.TourFaqRepository;
import com.tourya.api.repository.TourGalleryRepository;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import com.tourya.api.repository.TourItineraryRepository;
import com.tourya.api.repository.TourMainAttractionRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

/**
 * IA-09: aplica traducciones es -> en / pt-BR a todos los campos JSONB de un
 * tour. Se ejecuta en background ({@code @Async}) tras el evento
 * {@link TourTranslationEvent}, sin bloquear el HTTP response del save.
 *
 * <p><b>Por que un service aparte del listener:</b> {@code @Async} necesita un
 * proxy Spring — poner {@code @Async} en el mismo bean que el listener y luego
 * invocarlo con {@code this.method()} lo evita. Delegando al metodo publico de
 * este bean el proxy se aplica correctamente. Es el mismo motivo por el que
 * {@code CreditRefundEventListener} delega a los metodos {@code @Async} de
 * {@code EmailService}.</p>
 *
 * <p><b>Guardrail (no sobrescribir):</b> para cada campo {@link TranslatedField}
 * solo se rellena {@code en} / {@code pt} si estan {@code null} o blank.
 * Cualquier valor que el provider haya escrito manualmente (o que IA-07 haya
 * generado y el operador haya aceptado) queda intacto.</p>
 *
 * <p><b>No dispara el evento otra vez:</b> este bean llama {@code save/saveAll}
 * directamente en los repositorios — el evento {@link TourTranslationEvent}
 * solo se publica desde {@link com.tourya.api.services.TourService}, asi que
 * no hay riesgo de bucle infinito.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourTranslationApplier {

    private final ITranslationService translationService;
    private final TourRepository tourRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourFaqRepository tourFaqRepository;
    private final TourItineraryRepository tourItineraryRepository;
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final TourGalleryRepository tourGalleryRepository;

    private static final List<String> TARGET_LANGS = List.of("en", "pt");

    /**
     * Traduce todos los campos i18n del tour que aun no tengan en/pt.
     *
     * <p>{@code @Async} + {@code @Transactional} — la actualizacion corre en
     * hilo aparte con su propia tx. Si algun call a Google falla el campo se
     * deja en {@code null} y {@link TranslatedField#get(String)} hace auto-fallback
     * a espanol; el resto de los campos siguen adelante.</p>
     */
    @Async
    @Transactional
    public void translateTourAsync(Integer tourId) {
        if (tourId == null) return;
        if (!translationService.isEnabled()) {
            log.debug("IA-09 translation disabled — skipping tourId={}", tourId);
            return;
        }

        Tour tour = tourRepository.findById(tourId).orElse(null);
        if (tour == null) {
            log.warn("IA-09 tour not found tourId={} — skipping translation", tourId);
            return;
        }

        int updated = 0;
        try {
            // Campos directos del Tour (name + description).
            if (translateAndAssign(tour.getName(), tour::setName)) updated++;
            if (translateAndAssign(tour.getDescription(), tour::setDescription)) updated++;
            tourRepository.save(tour);

            // TourAddress[]: location (descripcion textual, HOTEL_PICKUP puede ser null).
            List<TourAddress> addresses = tourAddressRepository.findByTourId(tourId);
            for (TourAddress addr : addresses) {
                if (translateAndAssign(addr.getLocation(), addr::setLocation)) updated++;
            }
            if (!addresses.isEmpty()) tourAddressRepository.saveAll(addresses);

            // TourMainAttraction[]: description.
            List<TourMainAttraction> attractions = tourMainAttractionRepository.findByTourId(tourId);
            for (TourMainAttraction attr : attractions) {
                if (translateAndAssign(attr.getDescription(), attr::setDescription)) updated++;
            }
            if (!attractions.isEmpty()) tourMainAttractionRepository.saveAll(attractions);

            // TourIncludesExcludes[]: description (cubre includes y excludes).
            List<TourIncludesExcludes> includesExcludes =
                    tourIncludesExcludesRepository.findByTourId(tourId);
            for (TourIncludesExcludes item : includesExcludes) {
                if (translateAndAssign(item.getDescription(), item::setDescription)) updated++;
            }
            if (!includesExcludes.isEmpty()) tourIncludesExcludesRepository.saveAll(includesExcludes);

            // TourFaq[]: question + answer.
            List<TourFaq> faqs = tourFaqRepository.findByTourId(tourId);
            for (TourFaq faq : faqs) {
                if (translateAndAssign(faq.getQuestion(), faq::setQuestion)) updated++;
                if (translateAndAssign(faq.getAnswer(), faq::setAnswer)) updated++;
            }
            if (!faqs.isEmpty()) tourFaqRepository.saveAll(faqs);

            // TourItinerary[]: title + description.
            List<TourItinerary> itineraries = tourItineraryRepository.findByTourId(tourId);
            for (TourItinerary it : itineraries) {
                if (translateAndAssign(it.getTitle(), it::setTitle)) updated++;
                if (translateAndAssign(it.getDescription(), it::setDescription)) updated++;
            }
            if (!itineraries.isEmpty()) tourItineraryRepository.saveAll(itineraries);

            // TourCancellationPolicy[]: observations.
            List<TourCancellationPolicy> policies =
                    tourCancellationPolicyRepository.findByTourId(tourId);
            for (TourCancellationPolicy p : policies) {
                if (translateAndAssign(p.getObservations(), p::setObservations)) updated++;
            }
            if (!policies.isEmpty()) tourCancellationPolicyRepository.saveAll(policies);

            // TourGallery[]: description.
            List<TourGallery> galleries = tourGalleryRepository.findByTourId(tourId);
            for (TourGallery g : galleries) {
                if (translateAndAssign(g.getDescription(), g::setDescription)) updated++;
            }
            if (!galleries.isEmpty()) tourGalleryRepository.saveAll(galleries);

            log.info("IA-09 tour {} translated — {} field(s) updated (en/pt)", tourId, updated);
        } catch (Exception ex) {
            // Cualquier error individual se loguea; NO se relanza (async — ya no
            // hay caller esperando y el tour ya quedo persistido en ES).
            log.error("IA-09 translation failed tourId={} — partial updates persisted={}",
                    tourId, updated, ex);
        }
    }

    /**
     * Rellena {@code en} y {@code pt} en {@code field} SOLO si estan vacios; si
     * ya vienen escritos por el provider no los toca (RN-011). Devuelve
     * {@code true} si aplico al menos una traduccion — el caller usa el flag
     * para decidir si persistir la entidad.
     *
     * <p>Package-private para facilitar tests.</p>
     */
    boolean translateAndAssign(TranslatedField field, Consumer<TranslatedField> setter) {
        if (field == null) return false;
        String es = field.getEs();
        if (es == null || es.isBlank()) return false;

        boolean needsEn = isEmpty(field.getEn());
        boolean needsPt = isEmpty(field.getPt());
        if (!needsEn && !needsPt) return false;

        // Batch: minimiza calls cuando ambos idiomas hacen falta.
        List<String> langsToTranslate = needsEn && needsPt
                ? TARGET_LANGS
                : needsEn ? List.of("en") : List.of("pt");
        var translations = translationService.translateBatch(es, langsToTranslate);
        if (translations == null || translations.isEmpty()) return false;

        String newEn = needsEn ? translations.getOrDefault("en", field.getEn()) : field.getEn();
        String newPt = needsPt ? translations.getOrDefault("pt", field.getPt()) : field.getPt();

        boolean changed = (needsEn && translations.containsKey("en"))
                || (needsPt && translations.containsKey("pt"));
        if (!changed) return false;

        // Copia defensiva — mismo patron que TourService.copyTranslatedField para
        // asegurar que Hibernate detecte el cambio en la columna JSONB.
        setter.accept(new TranslatedField(es, newEn, newPt));
        return true;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
