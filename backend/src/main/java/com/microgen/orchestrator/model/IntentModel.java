package com.microgen.orchestrator.model;

import lombok.Data;

@Data
public class IntentModel {
    private String serviceName;
    private String packageName;
    private String language;
    private String framework;
    private String architecture;
    private String serviceType;
    private String auth;
    private String database;
    private String persistence;
    private Integer port;
    private java.util.List<EntityModel> entities;

    public IntentModel() {
        this.serviceName = "generated-service";
        this.packageName = "com.microgen.generated";
        this.language = "JAVA";
        this.framework = "SPRING_BOOT";
        this.architecture = "LAYERED";
        this.serviceType = "GENERAL";
        this.auth = "NONE";
        this.database = "H2";
        this.persistence = "JPA";
        this.port = 8082;
    }
}
