package com.tourya.api.config;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class JsonSchemaValidationConfig {
    @Bean
    public JsonSchemaFactory jsonSchemaFactory() {
        // Usa la versión del draft de JSON Schema que especificaste en tu archivo JSON
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @Bean("tourFullDataSchema") // Nombre del bean para inyectar en el controlador
    public JsonSchema tourFullDataSchema(JsonSchemaFactory factory) throws IOException {
        try (InputStream is = new ClassPathResource("jsonschema/tour-full-data-request.json").getInputStream()) {
            return factory.getSchema(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
