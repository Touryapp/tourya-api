package com.tourya.api.agents.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IA-01: unit tests para {@link PromptTemplate} — deterministas, sin LLM real
 * ni classpath (usan {@link PromptTemplate#ofInline}).
 */
class PromptTemplateTest {

    @Test
    @DisplayName("render sustituye placeholders {{var}} con valores")
    void render_replacesPlaceholders() {
        var tpl = PromptTemplate.ofInline("test", "Hola {{name}}, tienes {{count}} reservas.");
        var out = tpl.render(Map.of("name", "Franklin", "count", 3));
        assertEquals("Hola Franklin, tienes 3 reservas.", out);
    }

    @Test
    @DisplayName("render con map null devuelve el template intacto")
    void render_nullMap_returnsTemplate() {
        var tpl = PromptTemplate.ofInline("test", "Hola {{name}}");
        assertEquals("Hola {{name}}", tpl.render(null));
    }

    @Test
    @DisplayName("render con map vacio devuelve el template intacto")
    void render_emptyMap_returnsTemplate() {
        var tpl = PromptTemplate.ofInline("test", "Hola {{name}}");
        assertEquals("Hola {{name}}", tpl.render(Map.of()));
    }

    @Test
    @DisplayName("placeholder no matcheado queda literal — falla ruidosa mejor que silenciosa")
    void render_unmatchedPlaceholder_staysLiteral() {
        var tpl = PromptTemplate.ofInline("test", "Hola {{name}}, tu {{extra}} hoy.");
        var out = tpl.render(Map.of("name", "Luis"));
        assertTrue(out.contains("{{extra}}"), "placeholder no matcheado debe quedar literal");
        assertTrue(out.contains("Hola Luis"));
    }

    @Test
    @DisplayName("valor null se materializa como string vacio")
    void render_nullValue_emptyString() {
        var tpl = PromptTemplate.ofInline("test", "user={{u}}");
        var vars = new java.util.HashMap<String, Object>();
        vars.put("u", null);
        assertEquals("user=", tpl.render(vars));
    }

    @Test
    @DisplayName("valores no-string usan toString() — enums, numeros, etc.")
    void render_nonStringValues_useToString() {
        var tpl = PromptTemplate.ofInline("test", "code={{c}}, amount={{a}}");
        var out = tpl.render(Map.of("c", java.time.DayOfWeek.MONDAY, "a", 42.5));
        assertEquals("code=MONDAY, amount=42.5", out);
    }

    @Test
    @DisplayName("load de recurso inexistente falla ruidosamente")
    void load_missingResource_throws() {
        assertThrows(IllegalStateException.class,
                () -> PromptTemplate.load("nonexistent.v99"));
    }
}
