# Quick Start: Running the Database Migration

## Prerequisites ✅

Before running the migration, ensure:
- [ ] PostgreSQL database is running
- [ ] You have database admin credentials
- [ ] Application is stopped
- [ ] Database backup is created

---

## Step-by-Step Instructions

### 1. Create Database Backup

```bash
# Windows PowerShell
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
pg_dump -U your_username -d tourya_db > "backup_$timestamp.sql"
```

### 2. Connect to Database

```bash
psql -U your_username -d tourya_db
```

### 3. Run Migration Script

**Option A: From psql prompt**
```sql
\i 'c:/Users/MI PC/Documents/dev/TourYa/tourya-api/database/migrations/01_alter_columns_to_jsonb.sql'
```

**Option B: From command line**
```bash
psql -U your_username -d tourya_db -f "c:/Users/MI PC/Documents/dev/TourYa/tourya-api/database/migrations/01_alter_columns_to_jsonb.sql"
```

### 4. Verify Migration

```sql
-- Check column types (should all be jsonb)
SELECT 
    table_name, 
    column_name, 
    data_type 
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
AND column_name IN ('location', 'description', 'question', 'answer', 'title', 'observations')
ORDER BY table_name, column_name;
```

**Expected output**: All columns should show `data_type = jsonb`

### 5. Check Sample Data

```sql
-- Check tour_address
SELECT id, location FROM tour_address LIMIT 3;

-- Check tour_main_attractions
SELECT id, description FROM tour_main_attractions LIMIT 3;

-- Check tour_faq
SELECT id, question, answer FROM tour_faq LIMIT 3;
```

**Expected format**:
```json
{"es": "Original text", "en": "", "pt": ""}
```

### 6. Verify Constraints

```sql
-- Check constraints were created
SELECT 
    conname AS constraint_name,
    conrelid::regclass AS table_name
FROM pg_constraint
WHERE conname LIKE '%_es_required';
```

**Expected**: Should see 9 constraints (one for each JSONB field)

### 7. Verify Indexes

```sql
-- Check indexes were created
SELECT 
    indexname,
    tablename
FROM pg_indexes
WHERE indexname LIKE 'idx_%_gin' OR indexname LIKE 'idx_%_es';
```

**Expected**: Should see 12 indexes (GIN + B-tree)

### 8. Start Application

```bash
# Navigate to project directory
cd "c:/Users/MI PC/Documents/dev/TourYa/tourya-api"

# Start Spring Boot application
./mvnw spring-boot:run
```

### 9. Test API Endpoints

**Test 1: Get tour details**
```bash
curl http://localhost:8080/tour/details/1
```

**Expected response**:
```json
{
  "location": {
    "es": "Cartagena, Colombia",
    "en": "",
    "pt": ""
  }
}
```

**Test 2: Create tour with translations**
```bash
curl -X POST http://localhost:8080/tour/user/saveAll \
  -H "Content-Type: application/json" \
  -d '{
    "locations": [{
      "location": {
        "es": "Cartagena, Colombia",
        "en": "Cartagena, Colombia",
        "pt": "Cartagena, Colômbia"
      }
    }]
  }'
```

---

## Rollback (If Needed)

⚠️ **WARNING**: This will LOSE English and Portuguese translations!

```bash
psql -U your_username -d tourya_db -f "c:/Users/MI PC/Documents/dev/TourYa/tourya-api/database/migrations/02_rollback_jsonb_to_text.sql"
```

---

## Troubleshooting

### Error: "permission denied for table"
**Solution**: Run as database owner
```bash
psql -U postgres -d tourya_db -f migration_script.sql
```

### Error: "column is of type text but expression is of type jsonb"
**Solution**: The script handles this automatically. Ensure you're running the complete script, not partial queries.

### Error: "constraint already exists"
**Solution**: Constraints were already created. Safe to ignore or drop and recreate:
```sql
ALTER TABLE tour_address DROP CONSTRAINT IF EXISTS tour_address_location_es_required;
```

### Application won't start
**Solution**: Rebuild the project
```bash
./mvnw clean install
```

---

## Success Checklist ✅

After migration, verify:
- [x] All 11 columns are type `jsonb`
- [x] Existing data migrated (Spanish field populated)
- [x] 9 validation constraints created
- [x] 12 indexes created (9 GIN + 3 B-tree)
- [x] Application starts without errors
- [x] API endpoints return JSONB structure

---

## Next Steps

1. ⏳ Update DTOs to use `TranslatedField`
2. ⏳ Update frontend to handle new structure
3. ⏳ Add English and Portuguese translations to existing data
4. ⏳ Test all tour-related endpoints

---

## Files Reference

- Migration script: `database/migrations/01_alter_columns_to_jsonb.sql`
- Rollback script: `database/migrations/02_rollback_jsonb_to_text.sql`
- Full documentation: `database/migrations/README.md`
- Developer guide: `docs/TRANSLATED_FIELD_GUIDE.md`
- Implementation summary: `I18N_IMPLEMENTATION_SUMMARY.md`

---

**Estimated Time**: 5-10 minutes (depending on database size)
