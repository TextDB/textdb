package edu.uci.ics.texera.workflow.common.metadata.annotations;

import java.lang.annotation.*;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaBool;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;

import static edu.uci.ics.texera.workflow.common.metadata.annotations.EnablePresets.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@JacksonAnnotationsInside
@JsonSchemaInject(
        bools = @JsonSchemaBool(path = path, value = value))
public @interface EnablePresets {
    String path = "enable-presets";
    boolean value = true;
}
