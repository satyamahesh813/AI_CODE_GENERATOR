package com.microgen.orchestrator.model;

import lombok.Data;
import java.util.List;

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
    private String messaging;
    private String cache;
    private String buildTool;
    private String observability;
    private Integer port;
    private List<EntityModel> entities;

    public IntentModel() {
        this.serviceName    = "generated-service";
        this.packageName    = "com.microgen.generated";
        this.language       = "JAVA";
        this.framework      = "SPRING_BOOT";
        this.architecture   = "LAYERED";
        this.serviceType    = "GENERAL";
        this.auth           = "NONE";
        this.database       = "NONE";
        this.persistence    = "NONE";
        this.messaging      = "NONE";
        this.cache          = "NONE";
        this.buildTool      = "MAVEN";
        this.observability  = "NONE";
        this.port           = 8082;
    }
}
