package com.tourya.api.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.exceptions.JsonSchemaValidationException;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.request.TourFullDataRequest;
import com.tourya.api.services.TourService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

// Importar anotaciones de Swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Importar este RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("tour")
@RequiredArgsConstructor
@Tag(name = "Tour")
public class TourController {
    private final TourService tourService;
    private final ObjectMapper objectMapper;
    private final JsonSchema tourFullDataSchema; // Inyecta el esquema pre-cargado

    //SE COMENTO POR AHORA PARA NO CONFUSION DEL FRONT
    /*@PostMapping("/user/save")
    public ResponseEntity<TourResponse> save(
            @Valid @RequestBody TourRequest tourRequest,
            Authentication connectedUser
            ){
        return ResponseEntity.ok(tourService.save(tourRequest, connectedUser));
    }*/
    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<TourResponse>> findAll (
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) TourStatusEnum status,
            Authentication connectedUser){
        return ResponseEntity.ok(tourService.findAll(page, size, status, connectedUser));
    }
    @GetMapping("/admin/consultDataTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> consultDataTourByIdToAdmin (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.consultDataTourByIdToAdmin(tourId, connectedUser));
    }
    @PutMapping("/admin/acceptTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> acceptTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.acceptTourByIdToAdmin(tourId, connectedUser));
    }
    @PutMapping("/admin/cancelTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> cancelTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.cancelTourByIdToAdmin(tourId, connectedUser));
    }
    @GetMapping("/user/findAllByUser")
    public ResponseEntity<PageResponse<TourResponse>> findAllByUser (
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser){
        return ResponseEntity.ok(tourService.findAllByUser(page, size, connectedUser));
    }
    /*
    @PostMapping("/user/saveCreateBasicData")
    public ResponseEntity<TourDetailsResponse> saveCreateBasicData(
            @Valid @RequestBody TourCreateRequest tourCreateRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreateBasicData(tourCreateRequest, connectedUser));
    }*/
 /*   @PostMapping("/user/saveAll")
    public ResponseEntity<TourFullDataResponse> saveCreateFullData(
            @Valid @RequestBody TourFullDataRequest tourFullDataRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreateOrUpdateFullData(tourFullDataRequest, connectedUser));
    }*/
    @GetMapping("/user/consultDataTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> consultDataTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.consultDataTourById(tourId, connectedUser));
    }

    @Operation(
            summary = "Crear o actualizar un tour con todos sus datos y archivos asociados",
            description = "Permite la creación de un nuevo tour o la actualización de uno existente, incluyendo información detallada, ubicaciones, atracciones, políticas y galería de imágenes.",
            // **ELIMINAMOS EL BLOQUE 'encoding' DEL requestBody principal**
            // SpringDoc inferirá las partes multipart de las anotaciones @Parameter en los argumentos del método
            requestBody = @RequestBody(
                    description = "Datos completos del tour (JSON) y archivos multimedia (binarios).",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
                    // No se necesita el schema = @Schema(type = "object") ni el encoding aquí
                    // porque las partes serán descritas por @Parameter
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tour creado/actualizado exitosamente",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TourFullDataResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (problemas de validación de JSON Schema o Bean Validation)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.tourya.api.handler.ExceptionResponse.class))),
                    @ApiResponse(responseCode = "401", description = "No autorizado",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.tourya.api.handler.ExceptionResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.tourya.api.handler.ExceptionResponse.class)))
            }
    )
    @PostMapping(value = "/user/saveAll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TourFullDataResponse> saveCreateFullData(
            @Parameter(description = "Lista de archivos (imágenes/videos) a adjuntar al tour.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))) // Ya que es List<MultipartFile>, Swagger UI lo interpreta como múltiples archivos.
            @RequestPart("files") List<MultipartFile> files,
            // **Aquí es donde se describe la parte 'metadata' para Swagger UI**
            @Parameter(name = "metadata",
                    description = """
                        Objeto JSON con los datos completos del tour. Este JSON debe cumplir con el esquema de TourFullDataRequest.

                        **Ejemplo de formato JSON para metadata:**
                        ```json
                        {
                          "name": "Tour de Aventura por Canaima",
                          "description": "Explora los tepuyes y cascadas de Canaima.",
                          "tourCategoryId": 1,
                          "duration": "5 días / 4 noches",
                          "maxPeople": 15,
                          "highlight": 1,
                          "price": 750.00,
                          "minAge": 12,
                          "rating": 4.8,
                          "locations": [
                            {
                              "countryId": 234, "stateId": 12, "cityId": 34,
                              "latitude": 6.000, "longitude": -62.000,
                              "address": "Aeropuerto de Canaima",
                              "location": "Canaima National Park",
                              "addressType": "Meeting Point"
                            }
                          ],
                          "mainAttractions": [
                            {"description": "Salto Ángel"},
                            {"description": "Laguna de Canaima"}
                          ],
                          "includes": [
                            {"description": "Vuelos internos", "type": "Include"}
                          ],
                          "excludes": [
                            {"description": "Comidas no especificadas", "type": "Exclude"}
                          ],
                          "faq": [
                            {"question": "¿Necesito visa?", "answer": "Depende de tu nacionalidad."}
                          ],
                          "itineraries": [
                            {"title": "Llegada a Canaima", "day": 1, "time": "10:00", "description": "Vuelo a Canaima."}
                          ],
                          "cancellationPolicies": [
                            {"cancellationPolicyType": "Flexible", "observations": "Cancelación gratuita hasta 7 días antes.", "allowsRainRefund": true, "allowsRescheduling": true}
                          ],
                          "galleries": [
                            {"imageUrl": "[https://example.com/image1.jpg](https://example.com/image1.jpg)", "description": "Vista del Salto Ángel", "orderIndex": 0}
                          ]
                        }
                        ```
                        """, // <-- ¡Fíjate en las """ aquí y al inicio del String!
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TourFullDataRequest.class)))
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        // 1. Convertir el String metadataJson a un JsonNode para la validación del esquema
        JsonNode metadataNode = objectMapper.readTree(metadataJson);

        // 2. Realizar la validación con JSON Schema
        Set<ValidationMessage> validationMessages = tourFullDataSchema.validate(metadataNode);

        // Paso 3: Si la validación de JSON Schema falla, lanza tu excepción personalizada.
        // El GlobalExceptionHandler la capturará y formateará la respuesta de error.
        if (!validationMessages.isEmpty()) {
            throw new JsonSchemaValidationException(
                    "La validación del JSON de metadatos del tour ha fallado. Por favor, revise el formato y los datos.",
                    validationMessages
            );
        }

        // 3. Si la validación de JSON Schema pasa, procede a deserializar el JSON en tu DTO
        // Esto asegura que solo datos válidos lleguen a tu DTO.
        TourFullDataRequest tourFullDataRequest = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(tourService.saveCreateOrUpdateFullData(files,
                tourFullDataRequest, connectedUser));
    }
}
