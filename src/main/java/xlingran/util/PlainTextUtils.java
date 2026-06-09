package xlingran.util;

import org.springframework.util.StringUtils;

public final class PlainTextUtils {

    private PlainTextUtils() {
    }

    public static String stripHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        var stripped = html.replaceAll("<[^>]+>", "");
        return stripped.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    public static String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }
}
