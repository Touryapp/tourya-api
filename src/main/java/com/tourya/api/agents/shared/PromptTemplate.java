package com.tourya.api.agents.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * IA-01: plantilla de prompt versionada, cargada de {@code classpath:/prompts/{name}.txt}.
 *
 * <p>Convención de nombres: <code>{agentName}.{version}.txt</code>, ej.
 * <code>support24x7.v1.txt</code>. Al iterar prompts se crea {@code v2}
 * sin borrar {@code v1} (rollback trivial + A/B testing con feature flag).</p>
 *
 * <p>Sustituye placeholders <code>{{var}}</code> con los valores del map.
 * Si un placeholder no tiene valor, queda literal en el output (comportamiento
 * intencional: falla ruidosa mejor que silenciosa — se ve en el audit log
 * como texto sin renderizar).</p>
 */
@Slf4j
public final class PromptTemplate {

    private final String template;
    private final String name;

    private PromptTemplate(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    /**
     * Carga la plantilla del classpath. Throws {@link IllegalStateException}
     * si el recurso no existe — se resuelve en test time, no runtime.
     *
     * @param name Nombre del recurso sin extensión, ej. "support24x7.v1".
     */
    public static PromptTemplate load(String name) {
        String path = "prompts/" + name + ".txt";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt template not found: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new PromptTemplate(name, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read prompt template: " + path, ex);
        }
    }

    /**
     * Factory alternativa para tests — sin tocar classpath.
     */
    public static PromptTemplate ofInline(String name, String template) {
        return new PromptTemplate(name, template);
    }

    /**
     * Renderiza el template sustituyendo <code>{{var}}</code> por los valores.
     * Los null se materializan como string vacío. Los objetos usan
     * {@link Object#toString()}.
     */
    public String render(Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
