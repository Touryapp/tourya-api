# Postman Testing Guide - i18n with JSONB

## Overview

Esta guía muestra cómo probar los endpoints con los nuevos campos `TranslatedField` usando Postman.

---

## Formato de TranslatedField

Todos los campos traducibles ahora usan este formato JSON:

```json
{
  "es": "Texto en español",
  "en": "Text in English",
  "pt": "Texto em português"
}
```

**Importante**: El campo `es` (español) es **obligatorio**. Los campos `en` y `pt` son opcionales.

---

## Ejemplos de Requests

### 1. Crear Tour Completo

**Endpoint**: `POST /tour/user/saveAll`

**Body**:
```json
{
  "name": "Tour Islas del Rosario",
  "duration": 8,
  "locations": [
    {
      "countryId": 1,
      "stateId": 1,
      "cityId": 1,
      "latitude": 10.3910,
      "longitude": -75.4794,
      "address": "Muelle La Bodeguita",
      "location": {
        "es": "Cartagena, Colombia",
        "en": "Cartagena, Colombia",
        "pt": "Cartagena, Colômbia"
      },
      "addressType": "MEETING_POINT"
    }
  ],
  "mainAttractions": [
    {
      "description": {
        "es": "Islas del Rosario - Archipiélago paradisíaco",
        "en": "Rosario Islands - Paradisiacal archipelago",
        "pt": "Ilhas do Rosário - Arquipélago paradisíaco"
      }
    },
    {
      "description": {
        "es": "Playa Blanca - Arena blanca y aguas cristalinas",
        "en": "White Beach - White sand and crystal clear waters",
        "pt": "Praia Branca - Areia branca e águas cristalinas"
      }
    }
  ],
  "includes": [
    {
      "description": {
        "es": "Transporte en lancha rápida",
        "en": "Fast boat transportation",
        "pt": "Transporte em lancha rápida"
      },
      "type": "INCLUDE"
    },
    {
      "description": {
        "es": "Almuerzo típico costeño",
        "en": "Typical coastal lunch",
        "pt": "Almoço típico costeiro"
      },
      "type": "INCLUDE"
    }
  ],
  "excludes": [
    {
      "description": {
        "es": "Bebidas alcohólicas",
        "en": "Alcoholic beverages",
        "pt": "Bebidas alcoólicas"
      },
      "type": "EXCLUDE"
    }
  ],
  "faq": [
    {
      "question": {
        "es": "¿Qué debo llevar?",
        "en": "What should I bring?",
        "pt": "O que devo levar?"
      },
      "answer": {
        "es": "Protector solar, toalla, traje de baño y efectivo",
        "en": "Sunscreen, towel, swimsuit and cash",
        "pt": "Protetor solar, toalha, roupa de banho e dinheiro"
      }
    },
    {
      "question": {
        "es": "¿El tour opera con lluvia?",
        "en": "Does the tour operate in rain?",
        "pt": "O passeio funciona com chuva?"
      },
      "answer": {
        "es": "Sí, salvo condiciones climáticas extremas",
        "en": "Yes, except in extreme weather conditions",
        "pt": "Sim, exceto em condições climáticas extremas"
      }
    }
  ],
  "itineraries": [
    {
      "title": {
        "es": "Salida desde Cartagena",
        "en": "Departure from Cartagena",
        "pt": "Saída de Cartagena"
      },
      "day": 1,
      "time": "08:00:00",
      "description": {
        "es": "Encuentro en el muelle y salida en lancha",
        "en": "Meeting at the dock and boat departure",
        "pt": "Encontro no cais e partida de barco"
      }
    },
    {
      "title": {
        "es": "Llegada a Islas del Rosario",
        "en": "Arrival at Rosario Islands",
        "pt": "Chegada às Ilhas do Rosário"
      },
      "day": 1,
      "time": "09:30:00",
      "description": {
        "es": "Tiempo libre para nadar y snorkel",
        "en": "Free time for swimming and snorkeling",
        "pt": "Tempo livre para nadar e mergulhar"
      }
    },
    {
      "title": {
        "es": "Almuerzo",
        "en": "Lunch",
        "pt": "Almoço"
      },
      "day": 1,
      "time": "12:00:00",
      "description": {
        "es": "Almuerzo típico en restaurante local",
        "en": "Typical lunch at local restaurant",
        "pt": "Almoço típico em restaurante local"
      }
    },
    {
      "title": {
        "es": "Regreso a Cartagena",
        "en": "Return to Cartagena",
        "pt": "Retorno a Cartagena"
      },
      "day": 1,
      "time": "15:00:00",
      "description": {
        "es": "Salida de regreso al muelle de Cartagena",
        "en": "Departure back to Cartagena dock",
        "pt": "Partida de volta ao cais de Cartagena"
      }
    }
  ],
  "cancellationPolicies": [
    {
      "cancellationPolicyType": "FLEXIBLE",
      "allowsRainRefund": true,
      "allowsRescheduling": true,
      "observations": {
        "es": "Cancelación gratuita hasta 24 horas antes",
        "en": "Free cancellation up to 24 hours before",
        "pt": "Cancelamento gratuito até 24 horas antes"
      }
    }
  ],
  "galleries": [
    {
      "fileKey": "tour-rosario-1.jpg",
      "orderIndex": 1,
      "description": {
        "es": "Vista aérea de las Islas del Rosario",
        "en": "Aerial view of Rosario Islands",
        "pt": "Vista aérea das Ilhas do Rosário"
      }
    },
    {
      "fileKey": "tour-rosario-2.jpg",
      "orderIndex": 2,
      "description": {
        "es": "Playa Blanca con aguas cristalinas",
        "en": "White Beach with crystal clear waters",
        "pt": "Praia Branca com águas cristalinas"
      }
    }
  ]
}
```

---

### 2. Actualizar Tour

**Endpoint**: `PUT /tour/user/submitTourById/{tourId}`

**Body** (ejemplo parcial):
```json
{
  "mainAttractions": [
    {
      "id": 1,
      "description": {
        "es": "Islas del Rosario - Actualizado",
        "en": "Rosario Islands - Updated",
        "pt": "Ilhas do Rosário - Atualizado"
      }
    }
  ]
}
```

---

### 3. Consultar Tour

**Endpoint**: `GET /tour/details/{tourId}`

**Response esperado**:
```json
{
  "id": 1,
  "name": "Tour Islas del Rosario",
  "locations": [
    {
      "id": 1,
      "location": {
        "es": "Cartagena, Colombia",
        "en": "Cartagena, Colombia",
        "pt": "Cartagena, Colômbia"
      },
      "address": "Muelle La Bodeguita",
      "addressType": "MEETING_POINT"
    }
  ],
  "mainAttractions": [
    {
      "id": 1,
      "description": {
        "es": "Islas del Rosario - Archipiélago paradisíaco",
        "en": "Rosario Islands - Paradisiacal archipelago",
        "pt": "Ilhas do Rosário - Arquipélago paradisíaco"
      }
    }
  ],
  "includes": [
    {
      "id": 1,
      "description": {
        "es": "Transporte en lancha rápida",
        "en": "Fast boat transportation",
        "pt": "Transporte em lancha rápida"
      },
      "type": "INCLUDE"
    }
  ],
  "faq": [
    {
      "id": 1,
      "question": {
        "es": "¿Qué debo llevar?",
        "en": "What should I bring?",
        "pt": "O que devo levar?"
      },
      "answer": {
        "es": "Protector solar, toalla, traje de baño y efectivo",
        "en": "Sunscreen, towel, swimsuit and cash",
        "pt": "Protetor solar, toalha, roupa de banho e dinheiro"
      }
    }
  ],
  "itineraries": [
    {
      "id": 1,
      "title": {
        "es": "Salida desde Cartagena",
        "en": "Departure from Cartagena",
        "pt": "Saída de Cartagena"
      },
      "day": 1,
      "time": "08:00",
      "description": {
        "es": "Encuentro en el muelle y salida en lancha",
        "en": "Meeting at the dock and boat departure",
        "pt": "Encontro no cais e partida de barco"
      }
    }
  ],
  "cancellationPolicies": [
    {
      "id": 1,
      "observations": {
        "es": "Cancelación gratuita hasta 24 horas antes",
        "en": "Free cancellation up to 24 hours before",
        "pt": "Cancelamento gratuito até 24 horas antes"
      },
      "allowsRainRefund": true,
      "allowsRescheduling": true,
      "cancellationPolicyType": "FLEXIBLE"
    }
  ],
  "galleries": [
    {
      "id": 1,
      "imageUrl": "https://...",
      "description": {
        "es": "Vista aérea de las Islas del Rosario",
        "en": "Aerial view of Rosario Islands",
        "pt": "Vista aérea das Ilhas do Rosário"
      },
      "orderIndex": 1
    }
  ]
}
```

---

## Validaciones

### ✅ Request Válido
```json
{
  "description": {
    "es": "Texto en español",
    "en": "Text in English",
    "pt": "Texto em português"
  }
}
```

### ✅ Request Válido (solo español)
```json
{
  "description": {
    "es": "Solo texto en español",
    "en": "",
    "pt": ""
  }
}
```

### ❌ Request Inválido (español vacío)
```json
{
  "description": {
    "es": "",
    "en": "Only English",
    "pt": ""
  }
}
```
**Error**: `"Spanish translation is required"`

### ❌ Request Inválido (sin español)
```json
{
  "description": {
    "en": "Only English",
    "pt": "Apenas português"
  }
}
```
**Error**: `"Spanish translation is required"`

---

## Casos de Uso Comunes

### Caso 1: Crear tour solo en español (para después traducir)
```json
{
  "mainAttractions": [
    {
      "description": {
        "es": "Descripción en español",
        "en": "",
        "pt": ""
      }
    }
  ]
}
```

### Caso 2: Actualizar solo la traducción en inglés
```json
{
  "mainAttractions": [
    {
      "id": 1,
      "description": {
        "es": "Texto original en español",
        "en": "Updated English translation",
        "pt": "Texto original em português"
      }
    }
  ]
}
```

### Caso 3: Agregar traducciones a tour existente
1. Primero obtén el tour: `GET /tour/details/{tourId}`
2. Copia el response
3. Actualiza los campos traducibles agregando `en` y `pt`
4. Envía el update: `PUT /tour/user/submitTourById/{tourId}`

---

## Endpoints Afectados

Todos estos endpoints ahora usan `TranslatedField`:

### Usuario
- `PUT /tour/user/submitTourById/{tourId}`
- `POST /tour/user/saveAll`
- `GET /tour/user/findAllByUser`
- `GET /tour/user/consultDataTourById/{tourId}`

### Admin
- `PUT /tour/admin/returnedTourById/{tourId}`
- `PUT /tour/admin/cancelTourById/{tourId}`
- `PUT /tour/admin/acceptTourById/{tourId}`
- `GET /tour/admin/findAll`
- `GET /tour/admin/consultDataTourById/{tourId}`

### Público
- `GET /tour/details/{tourId}`

---

## Troubleshooting

### Error: "Cannot deserialize value of type `TranslatedField`"
**Causa**: JSON mal formado  
**Solución**: Asegúrate de usar el formato correcto:
```json
{
  "field": {
    "es": "texto",
    "en": "text",
    "pt": "texto"
  }
}
```

### Error: "Spanish translation is required"
**Causa**: Campo `es` vacío o ausente  
**Solución**: Siempre incluye el campo `es` con contenido:
```json
{
  "field": {
    "es": "Texto obligatorio",
    "en": "",
    "pt": ""
  }
}
```

### Error: 500 Internal Server Error
**Causa**: Base de datos no migrada  
**Solución**: Ejecuta el script de migración:
```bash
psql -U user -d db -f database/migrations/01_alter_columns_to_jsonb.sql
```

---

## Colección de Postman

Puedes importar esta colección de ejemplo:

**Archivo**: `TourYa_i18n_Tests.postman_collection.json`

```json
{
  "info": {
    "name": "TourYa - i18n Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Tour with Translations",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"mainAttractions\": [\n    {\n      \"description\": {\n        \"es\": \"Islas del Rosario\",\n        \"en\": \"Rosario Islands\",\n        \"pt\": \"Ilhas do Rosário\"\n      }\n    }\n  ]\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{baseUrl}}/tour/user/saveAll",
          "host": ["{{baseUrl}}"],
          "path": ["tour", "user", "saveAll"]
        }
      }
    },
    {
      "name": "Get Tour Details",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/tour/details/{{tourId}}",
          "host": ["{{baseUrl}}"],
          "path": ["tour", "details", "{{tourId}}"]
        }
      }
    }
  ]
}
```

---

## Variables de Entorno

Configura estas variables en Postman:

- `baseUrl`: `http://localhost:8080` (o tu URL de API)
- `tourId`: ID del tour para pruebas
- `token`: Token de autenticación (si aplica)
