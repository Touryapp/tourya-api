# Database Migrations - i18n with JSONB

## Overview

This directory contains SQL migration scripts to add internationalization support using PostgreSQL JSONB type.

## Migration Files

### 1. `01_alter_columns_to_jsonb.sql` - Main Migration
Converts text columns to JSONB format for multi-language support (Spanish, English, Portuguese).

**What it does:**
- Alters column types from TEXT/VARCHAR to JSONB
- Migrates existing data (current values → Spanish field)
- Adds validation constraints (Spanish field required)
- Creates GIN indexes for efficient JSONB queries
- Creates specific indexes for Spanish language searches

**Affected Tables:**
- `tour_address` → `location`
- `tour_main_attractions` → `description`
- `tour_includes_excludes` → `description`
- `tour_faq` → `question`, `answer`
- `tour_itinerary` → `title`, `description`
- `tour_cancellation_policy` → `observations`
- `tour_gallery` → `description`

### 2. `02_rollback_jsonb_to_text.sql` - Rollback Script
Reverts JSONB columns back to TEXT type.

**⚠️ WARNING:** This will **LOSE** English and Portuguese translations! Only use if you need to rollback.

## How to Run

### Prerequisites
1. Backup your database first!
2. Ensure you have PostgreSQL admin access
3. Stop your application to avoid conflicts

### Running the Migration

```bash
# Connect to your database
psql -U your_username -d tourya_db

# Run the migration script
\i c:/Users/MI\ PC/Documents/dev/TourYa/tourya-api/database/migrations/01_alter_columns_to_jsonb.sql

# Verify the migration
SELECT table_name, column_name, data_type 
FROM information_schema.columns 
WHERE table_name IN (
    'tour_address', 
    'tour_main_attractions', 
    'tour_includes_excludes',
    'tour_faq',
    'tour_itinerary',
    'tour_cancellation_policy',
    'tour_gallery'
)
AND column_name IN ('location', 'description', 'question', 'answer', 'title', 'observations');
```

### Rollback (if needed)

```bash
# Only if you need to revert the changes
\i c:/Users/MI\ PC/Documents/dev/TourYa/tourya-api/database/migrations/02_rollback_jsonb_to_text.sql
```

## Data Structure

### Before Migration
```sql
location: "Cartagena, Colombia"
```

### After Migration
```json
location: {
  "es": "Cartagena, Colombia",
  "en": "",
  "pt": ""
}
```

## Querying JSONB Data

### Get Spanish value
```sql
SELECT location->>'es' FROM tour_address;
```

### Search by Spanish content
```sql
SELECT * FROM tour_address 
WHERE location->>'es' ILIKE '%cartagena%';
```

### Search by English content
```sql
SELECT * FROM tour_main_attractions 
WHERE description->>'en' ILIKE '%islands%';
```

### Update a specific language
```sql
UPDATE tour_address 
SET location = jsonb_set(location, '{en}', '"Cartagena, Colombia"')
WHERE id = 1;
```

## Performance Considerations

The migration creates GIN indexes on all JSONB columns for efficient querying. These indexes support:
- Full JSONB queries
- Containment operations
- Existence checks

For specific language searches, additional B-tree indexes are created on the Spanish fields.

## Troubleshooting

### Issue: Migration fails with "column is of type text but expression is of type jsonb"
**Solution:** The script uses `USING` clause to handle type conversion automatically. Ensure you're running the complete script.

### Issue: Constraint violations after migration
**Solution:** Check that all Spanish fields have non-empty values. The migration should handle this automatically, but verify with:
```sql
SELECT * FROM tour_address WHERE location->>'es' IS NULL OR location->>'es' = '';
```

### Issue: Application errors after migration
**Solution:** Ensure your Java entities are updated to use `TranslatedField` type and the `TranslatedFieldConverter` is properly configured.

## Next Steps

After running the migration:
1. ✅ Update Java entities (already done)
2. ⏳ Update DTOs and Request/Response classes
3. ⏳ Update mappers
4. ⏳ Test API endpoints
5. ⏳ Update frontend to handle new structure
