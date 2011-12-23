package org.jdownloader.update;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;

public class SwitchParam {
    private final String param;
    private final String description;
    private String[] parameters;

    public SwitchParam(final String sw,  String description,final String... parameters) {
        param = sw;
        this.description = description;
        this.parameters=parameters;
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

   

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append("-");
        sb.append(param);
        for(String s:parameters){
            sb.append(" ");
            sb.append(s);
        }
        
        while(sb.length()<40){
            sb.append(" ");            
        }
        if(!StringUtils.isEmpty(description)){
            sb.append(" | ");
            sb.append(description);
        }
        return sb.toString();
    }
}
