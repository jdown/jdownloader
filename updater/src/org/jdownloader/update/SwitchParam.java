package org.jdownloader.update;

import org.appwork.utils.logging.Log;

public class SwitchParam {
    private final String param;
    private final String description;

    public SwitchParam(final String sw, final String description) {
        param = sw;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getParam() {
        return param;
    }

    public boolean matches(String p) {
        while (p.startsWith("-")) {
            p = p.substring(1);
        }
        return p.equalsIgnoreCase(param);
    }

    public void print() {
        Log.L.info("-" + param + "description");
    }

    @Override
    public String toString() {
        return description;
    }
}
