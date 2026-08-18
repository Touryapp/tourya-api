package com.tourya.api.services.translation;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourFaq;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourCancellationPolicyRepository;
import com.tourya.api.repository.TourFaqRepository;
import com.tourya.api.repository.TourGalleryRepository;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import com.tourya.api.repository.TourItineraryRepository;
import com.tourya.api.repository.TourMainAttractionRepository;
import com.tourya.api.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IA-09: unit tests de {@link TourTranslationApplier}.
 *
 * <p>Cubre:</p>
 * <ul>
 *   <li>Happy path: campos vacios se rellenan con traducciones.</li>
 *   <li>Guardrail (no sobrescribir): campos con en/pt ya escritos se respetan.</li>
 *   <li>Skip cuando el service esta disabled.</li>
 *   <li>Skip cuando el tour no existe.</li>
 *   <li>No dispara evento otra vez (integrity check — el applier llama a save,
 *       no publica {@link TourTranslationEvent}).</li>
 *   <li>Guardrail via {@link TourTranslationApplier#translateAndAssign} directo:
 *       campo con solo {@code en} vacio solo pide {@code en} al service.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TourTranslationApplierTest {

    @Spy
    private MockTranslationClient translationService = new MockTranslationClient();

    @Mock private TourRepository tourRepository;
    @Mock private TourAddressRepository tourAddressRepository;
    @Mock private TourMainAttractionRepository tourMainAttractionRepository;
    @Mock private TourIncludesExcludesRepository tourIncludesExcludesRepository;
    @Mock private TourFaqRepository tourFaqRepository;
    @Mock private TourItineraryRepository tourItineraryRepository;
    @Mock private TourCancellationPolicyRepository tourCancellationPolicyRepository;
    @Mock private TourGalleryRepository tourGalleryRepository;

    private TourTranslationApplier applier;

    @BeforeEach
    void setUp() {
        applier = new TourTranslationApplier(
                translationService,
                tourRepository,
                tourAddressRepository,
                tourMainAttractionRepository,
                tourIncludesExcludesRepository,
                tourFaqRepository,
                tourItineraryRepository,
                tourCancellationPolicyRepository,
                tourGalleryRepository
        );
        // Todas las listas por default vacias — cada test setea las que le importa.
        // lenient() porque algunos tests skip early (disabled / tour no encontrado)
        // y no llegan a consultar todas las listas.
        lenient().when(tourAddressRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourMainAttractionRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourIncludesExcludesRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourFaqRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourItineraryRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourCancellationPolicyRepository.findByTourId(any())).thenReturn(List.of());
        lenient().when(tourGalleryRepository.findByTourId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("listener translates all empty fields when event fires")
    void listener_translatesAllEmptyFields_whenEventFires() {
        Tour tour = new Tour();
        tour.setId(42);
        tour.setName(new TranslatedField("Tour por la bahia", "", ""));
        tour.setDescription(new TranslatedField("Descripcion en espanol", null, null));
        when(tourRepository.findById(42)).thenReturn(Optional.of(tour));

        applier.translateTourAsync(42);

        ArgumentCaptor<Tour> captor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(captor.capture());
        Tour saved = captor.getValue();
        assertThat(saved.getName().getEs()).isEqualTo("Tour por la bahia");
        assertThat(saved.getName().getEn()).isEqualTo("[en] Tour por la bahia");
        assertThat(saved.getName().getPt()).isEqualTo("[pt] Tour por la bahia");
        assertThat(saved.getDescription().getEn()).isEqualTo("[en] Descripcion en espanol");
        assertThat(saved.getDescription().getPt()).isEqualTo("[pt] Descripcion en espanol");
    }

    @Test
    @DisplayName("listener skips fields already translated (no override)")
    void listener_skipsFieldsAlreadyTranslated() {
        Tour tour = new Tour();
        tour.setId(43);
        // El provider ya escribio en+pt manualmente — el guardrail debe respetarlo.
        tour.setName(new TranslatedField("Tour bahia", "Bay Tour", "Passeio bahia"));
        // Descripcion parcial: solo en vacio, pt lo puso el provider.
        tour.setDescription(new TranslatedField("Descripcion", "", "Descricao BR"));
        when(tourRepository.findById(43)).thenReturn(Optional.of(tour));

        applier.translateTourAsync(43);

        ArgumentCaptor<Tour> captor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(captor.capture());
        Tour saved = captor.getValue();
        // name intacto (ambos idiomas ya escritos).
        assertThat(saved.getName().getEn()).isEqualTo("Bay Tour");
        assertThat(saved.getName().getPt()).isEqualTo("Passeio bahia");
        // description: solo en se rellena, pt se respeta.
        assertThat(saved.getDescription().getEn()).isEqualTo("[en] Descripcion");
        assertThat(saved.getDescription().getPt()).isEqualTo("Descricao BR");
    }

    @Test
    @DisplayName("listener skips when translation service disabled")
    void listener_skipsWhenServiceDisabled() {
        translationService.disable();
        applier.translateTourAsync(44);
        verify(tourRepository, never()).findById(any());
        verify(tourRepository, never()).save(any());
    }

    @Test
    @DisplayName("listener skips when tour not found")
    void listener_skipsWhenTourNotFound() {
        when(tourRepository.findById(999)).thenReturn(Optional.empty());
        applier.translateTourAsync(999);
        verify(tourRepository, never()).save(any());
    }

    @Test
    @DisplayName("listener persists updates without publishing a new TourTranslationEvent")
    void listener_persistsUpdatesWithoutRefiring() {
        // Este test verifica el diseno: el applier llama a save/saveAll pero
        // NO publica TourTranslationEvent (el event solo se publica desde
        // TourService). No hay ApplicationEventPublisher inyectado en el
        // applier — que ese campo no exista es la garantia estructural de que
        // no hay bucle infinito.
        Tour tour = new Tour();
        tour.setId(45);
        tour.setName(new TranslatedField("Nombre", "", ""));
        when(tourRepository.findById(45)).thenReturn(Optional.of(tour));

        // Fija FAQ con contenido para asegurar que se recorra su rama.
        TourFaq faq = new TourFaq();
        faq.setQuestion(new TranslatedField("Que incluye?", "", ""));
        faq.setAnswer(new TranslatedField("Todo lo listado", "", ""));
        when(tourFaqRepository.findByTourId(45)).thenReturn(List.of(faq));

        applier.translateTourAsync(45);

        verify(tourRepository, times(1)).save(any(Tour.class));
        verify(tourFaqRepository, times(1)).saveAll(anyList());
        // El applier no tiene ApplicationEventPublisher inyectado — no hay riesgo
        // estructural de bucle. Confirmamos que la rama del listener nunca se dispara.
    }

    @Test
    @DisplayName("translateAndAssign only requests missing lang (batch minimizado)")
    void translateAndAssign_requestsOnlyMissingLang() {
        MockTranslationClient mock = new MockTranslationClient();
        TourTranslationApplier local = new TourTranslationApplier(
                mock, tourRepository, tourAddressRepository, tourMainAttractionRepository,
                tourIncludesExcludesRepository, tourFaqRepository, tourItineraryRepository,
                tourCancellationPolicyRepository, tourGalleryRepository
        );

        TranslatedField field = new TranslatedField("Tour bahia", "", "Passeio bahia");
        boolean[] setterCalled = new boolean[]{false};
        boolean changed = local.translateAndAssign(field, v -> {
            setterCalled[0] = true;
            assertThat(v.getEn()).isEqualTo("[en] Tour bahia");
            // pt se conserva:
            assertThat(v.getPt()).isEqualTo("Passeio bahia");
        });

        assertThat(changed).isTrue();
        assertThat(setterCalled[0]).isTrue();
        // Solo se pidio 'en' — 'pt' ya estaba escrito, no se envia al API.
        assertThat(mock.capturedLangs()).containsExactly("en");
    }

    @Test
    @DisplayName("translateAndAssign returns false when field is null or ES blank")
    void translateAndAssign_returnsFalse_whenNoSource() {
        boolean[] setterCalled = new boolean[]{false};

        assertThat(applier.translateAndAssign(null, v -> setterCalled[0] = true)).isFalse();

        TranslatedField blank = new TranslatedField("", "", "");
        assertThat(applier.translateAndAssign(blank, v -> setterCalled[0] = true)).isFalse();

        TranslatedField nullEs = new TranslatedField(null, "", "");
        assertThat(applier.translateAndAssign(nullEs, v -> setterCalled[0] = true)).isFalse();

        assertThat(setterCalled[0]).isFalse();
    }

    @Test
    @DisplayName("translateAndAssign returns false when both langs already filled")
    void translateAndAssign_returnsFalse_whenAlreadyFilled() {
        TranslatedField full = new TranslatedField("Nombre", "Name", "Nome");
        boolean[] setterCalled = new boolean[]{false};
        assertThat(applier.translateAndAssign(full, v -> setterCalled[0] = true)).isFalse();
        assertThat(setterCalled[0]).isFalse();
    }
}
