package xlingran.util;

import org.springframework.util.StringUtils;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

public final class MeterUtils {

    private MeterUtils() {
    }

    public static String counterName(Class<? extends AbstractExtension> type, String extensionName) {
        var annotation = type.getAnnotation(GVK.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Missing @GVK on " + type.getName());
        }
        return nameOf(annotation.group(), annotation.plural(), extensionName);
    }

    public static String nameOf(String group, String plural, String name) {
        if (!StringUtils.hasText(group)) {
            return plural + "/" + name;
        }
        return plural + "." + group + "/" + name;
    }
}
