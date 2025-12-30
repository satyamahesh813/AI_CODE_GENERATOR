package com.microgen.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityModel {
    private String name;
    private List<FieldModel> fields;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldModel {
        private String name;
        private String type;
    }
}
