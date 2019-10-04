package io.statusmachina.core.api;

import java.util.Map;

public class MachineSnapshot {
    final String type;
    final String id;
    String crtState;
    final Map<String,String> context;
    final String error;

    public MachineSnapshot(String type, String id, String crtState, Map<String, String> context, String error) {
        this.type = type;
        this.id = id;
        this.crtState = crtState;
        this.context = context;
        this.error = error;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public String getError() {
        return error;
    }
}
