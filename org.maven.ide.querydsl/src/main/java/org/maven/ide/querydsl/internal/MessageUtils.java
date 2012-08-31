package org.maven.ide.querydsl.internal;

import org.apache.maven.model.Plugin;


public class MessageUtils {

    private MessageUtils() {
        // NOTHING
    }
    
    public static String info(Plugin p_plugin, String p_msg, Object... args) {
        String context = String.format("Plugin %s:%s is not properly configured. ", p_plugin.getGroupId(), p_plugin.getArtifactId());
        String detail = String.format(p_msg, args);
        return context + detail;
    }
}
