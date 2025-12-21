# TranslatedField Developer Guide

## Quick Reference

This guide shows you how to work with `TranslatedField` objects in the TourYa API.

---

## Creating TranslatedField Objects

### Option 1: Constructor
```java
TranslatedField location = new TranslatedField(
    "Cartagena, Colombia",      // Spanish (required)
    "Cartagena, Colombia",       // English
    "Cartagena, Colômbia"        // Portuguese
);
```

### Option 2: Factory Method - Spanish Only
```java
TranslatedField location = TranslatedField.ofSpanish("Cartagena, Colombia");
// Result: {es: "Cartagena, Colombia", en: "", pt: ""}
```

### Option 3: Factory Method - All Languages
```java
TranslatedField location = TranslatedField.of(
    "Cartagena, Colombia",
    "Cartagena, Colombia",
    "Cartagena, Colômbia"
);
```

### Option 4: Builder Pattern (with Lombok)
```java
TranslatedField location = TranslatedField.builder()
    .es("Cartagena, Colombia")
    .en("Cartagena, Colombia")
    .pt("Cartagena, Colômbia")
    .build();
```

---

## Getting Translations

### Get Specific Language
```java
TranslatedField description = attraction.getDescription();

String spanish = description.getEs();
String english = description.getEn();
String portuguese = description.getPt();
```

### Get with Fallback
```java
// If English is empty, falls back to Spanish
String text = description.get("en");

// Examples:
description.get("es");  // Returns Spanish
description.get("en");  // Returns English, or Spanish if English is empty
description.get("pt");  // Returns Portuguese, or Spanish if Portuguese is empty
description.get(null);  // Returns Spanish (default)
```

---

## Using in Entities

### Example: TourAddress
```java
@Entity
@Table(name = "tour_address")
public class TourAddress extends BaseEntity {
    
    @Convert(converter = TranslatedFieldConverter.class)
    @Column(name = "location", columnDefinition = "jsonb")
    private TranslatedField location;
    
    // Getters and setters
}
```

### Setting Values
```java
TourAddress address = new TourAddress();
address.setLocation(TranslatedField.of(
    "Cartagena, Colombia",
    "Cartagena, Colombia",
    "Cartagena, Colômbia"
));
```

---

## Using in DTOs/Requests

### Request DTO
```java
public class TourAddressRequest {
    @Valid
    private TranslatedField location;
    
    // Other fields...
}
```

### JSON Request Body
```json
{
  "location": {
    "es": "Cartagena, Colombia",
    "en": "Cartagena, Colombia",
    "pt": "Cartagena, Colômbia"
  }
}
```

### Response DTO
```java
public class TourAddressResponse {
    private Integer id;
    private TranslatedField location;
    
    // Other fields...
}
```

### JSON Response
```json
{
  "id": 1,
  "location": {
    "es": "Cartagena, Colombia",
    "en": "Cartagena, Colombia",
    "pt": "Cartagena, Colômbia"
  }
}
```

---

## Validation

### Spanish is Required
```java
@Valid
private TranslatedField description;
```

The `@NotBlank` annotation on the `es` field ensures Spanish is always provided:
```json
// ✅ Valid
{
  "description": {
    "es": "Tour a las Islas",
    "en": "",
    "pt": ""
  }
}

// ❌ Invalid - Spanish is empty
{
  "description": {
    "es": "",
    "en": "Islands Tour",
    "pt": ""
  }
}
```

---

## Mapping Between Entities and DTOs

### Using MapStruct (if available)
```java
@Mapper
public interface TourMapper {
    TourResponse toResponse(Tour tour);
    Tour toEntity(TourRequest request);
}
```

MapStruct will automatically map `TranslatedField` objects since they're POJOs.

### Manual Mapping
```java
public TourAddressResponse toResponse(TourAddress entity) {
    TourAddressResponse response = new TourAddressResponse();
    response.setId(entity.getId());
    response.setLocation(entity.getLocation()); // Direct assignment
    return response;
}
```

---

## Database Queries

### JPA Repository - Find by Spanish Content
```java
public interface TourMainAttractionRepository extends JpaRepository<TourMainAttraction, Integer> {
    
    @Query(value = "SELECT * FROM tour_main_attractions WHERE description->>'es' ILIKE %:keyword%", 
           nativeQuery = true)
    List<TourMainAttraction> findByDescriptionSpanishContaining(@Param("keyword") String keyword);
}
```

### Native SQL Queries
```java
@Query(value = """
    SELECT * FROM tour_address 
    WHERE location->>'es' = :location 
       OR location->>'en' = :location 
       OR location->>'pt' = :location
    """, nativeQuery = true)
List<TourAddress> findByLocationInAnyLanguage(@Param("location") String location);
```

---

## Common Patterns

### Pattern 1: Create with User Input
```java
@PostMapping("/tours")
public ResponseEntity<TourResponse> createTour(@RequestBody TourRequest request) {
    Tour tour = new Tour();
    
    // TranslatedField is already in the request
    tour.setMainAttractions(request.getMainAttractions());
    
    tourRepository.save(tour);
    return ResponseEntity.ok(toResponse(tour));
}
```

### Pattern 2: Update Specific Language
```java
public void updateEnglishTranslation(Integer attractionId, String englishText) {
    TourMainAttraction attraction = repository.findById(attractionId)
        .orElseThrow(() -> new NotFoundException("Attraction not found"));
    
    TranslatedField description = attraction.getDescription();
    description.setEn(englishText);
    
    repository.save(attraction);
}
```

### Pattern 3: Get Translation Based on User Language
```java
@GetMapping("/tours/{id}")
public ResponseEntity<TourDetailResponse> getTourDetails(
        @PathVariable Integer id,
        @RequestHeader(value = "Accept-Language", defaultValue = "es") String language) {
    
    Tour tour = tourRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Tour not found"));
    
    // Get description in user's language
    String description = tour.getDescription().get(language);
    
    return ResponseEntity.ok(buildResponse(tour, language));
}
```

### Pattern 4: Validate All Languages Present
```java
public boolean hasAllTranslations(TranslatedField field) {
    return field != null 
        && field.getEs() != null && !field.getEs().isEmpty()
        && field.getEn() != null && !field.getEn().isEmpty()
        && field.getPt() != null && !field.getPt().isEmpty();
}
```

---

## Testing

### Unit Test Example
```java
@Test
public void testTranslatedFieldCreation() {
    TranslatedField field = TranslatedField.of(
        "Español",
        "English",
        "Português"
    );
    
    assertEquals("Español", field.getEs());
    assertEquals("English", field.getEn());
    assertEquals("Português", field.getPt());
}

@Test
public void testTranslatedFieldFallback() {
    TranslatedField field = TranslatedField.ofSpanish("Solo español");
    
    assertEquals("Solo español", field.get("es"));
    assertEquals("Solo español", field.get("en")); // Falls back to Spanish
    assertEquals("Solo español", field.get("pt")); // Falls back to Spanish
}
```

### Integration Test Example
```java
@Test
public void testSaveAndRetrieveTourWithTranslations() {
    TourAddress address = new TourAddress();
    address.setLocation(TranslatedField.of(
        "Cartagena, Colombia",
        "Cartagena, Colombia",
        "Cartagena, Colômbia"
    ));
    
    TourAddress saved = repository.save(address);
    TourAddress retrieved = repository.findById(saved.getId()).orElseThrow();
    
    assertEquals("Cartagena, Colombia", retrieved.getLocation().getEs());
    assertEquals("Cartagena, Colombia", retrieved.getLocation().getEn());
    assertEquals("Cartagena, Colômbia", retrieved.getLocation().getPt());
}
```

---

## Troubleshooting

### Issue: "Cannot deserialize value of type `TranslatedField`"
**Cause**: JSON structure doesn't match expected format  
**Solution**: Ensure JSON has `es`, `en`, and `pt` fields:
```json
{
  "description": {
    "es": "Texto en español",
    "en": "Text in English",
    "pt": "Texto em português"
  }
}
```

### Issue: Validation error "Spanish translation is required"
**Cause**: Spanish field is null or empty  
**Solution**: Always provide Spanish text:
```java
TranslatedField.of("Texto español", "", ""); // ✅ Valid
TranslatedField.of("", "English text", ""); // ❌ Invalid
```

### Issue: Database error "column is of type jsonb but expression is of type text"
**Cause**: Database column not migrated to JSONB  
**Solution**: Run the migration script:
```bash
psql -U user -d db -f database/migrations/01_alter_columns_to_jsonb.sql
```

---

## Best Practices

1. **Always provide Spanish**: It's the default language and required
2. **Use factory methods**: `ofSpanish()` for quick creation
3. **Use `get()` method**: For language fallback logic
4. **Validate in DTOs**: Use `@Valid` annotation
5. **Index JSONB columns**: For better query performance
6. **Test all languages**: Ensure translations work correctly

---

## Related Files

- Model: [`TranslatedField.java`](file:///c:/Users/MI PC/Documents/dev/TourYa/tourya-api/src/main/java/com/tourya/api/models/TranslatedField.java)
- Converter: [`TranslatedFieldConverter.java`](file:///c:/Users/MI PC/Documents/dev/TourYa/tourya-api/src/main/java/com/tourya/api/config/TranslatedFieldConverter.java)
- Migration: [`01_alter_columns_to_jsonb.sql`](file:///c:/Users/MI PC/Documents/dev/TourYa/tourya-api/database/migrations/01_alter_columns_to_jsonb.sql)
