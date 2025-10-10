package io.github.codesapienbe.springvision.facebytes.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public final class Logs {

    private static final Logger logger = LoggerFactory.getLogger("application");

    private Logs() {
    }

    public static void info(String component, String message, Map<String, ?> context) {
        logger.info(format("INFO", component, message, null, context));
    }

    public static void debug(String component, String message, Map<String, ?> context) {
        logger.debug(format("DEBUG", component, message, null, context));
    }

    public static void warn(String component, String message, Map<String, ?> context) {
        logger.warn(format("WARN", component, message, null, context));
    }

    public static void error(String component, String message, Throwable t, Map<String, ?> context) {
        logger.error(format("ERROR", component, message, t, context));
    }

    private static String format(String level, String component, String message, Throwable t, Map<String, ?> context) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        field(sb, "timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        sb.append(',');
        field(sb, "level", level);
        sb.append(',');
        field(sb, "component", component);
        sb.append(',');
        field(sb, "message", message);
        sb.append(',');
        // Correlation fields from MDC if present
        field(sb, "correlation_id", nullToEmpty(MDC.get("correlation_id")));
        sb.append(',');
        field(sb, "user_id", nullToEmpty(MDC.get("user_id")));
        sb.append(',');
        field(sb, "request_id", nullToEmpty(MDC.get("request_id")));
        // Context map
        if (context != null && !context.isEmpty()) {
            sb.append(',');
            sb.append("\"context\":{");
            boolean first = true;
            for (Map.Entry<String, ?> e : context.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(e.getKey())).append('"').append(':');
                value(sb, e.getValue());
            }
            sb.append('}');
        }
        // Error info (type + message only, avoid stack to reduce sensitive leakage)
        if (t != null) {
            sb.append(',');
            sb.append("\"error\":{");
            field(sb, "type", t.getClass().getName());
            sb.append(',');
            field(sb, "message", safeErrorMessage(t));
            sb.append('}');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static void field(StringBuilder sb, String key, String value) {
        sb.append('"').append(escape(key)).append('"').append(':');
        sb.append('"').append(escape(value)).append('"');
    }

    private static void value(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
            return;
        }
        if (v instanceof Number || v instanceof Boolean) {
            sb.append(Objects.toString(v));
        } else {
            sb.append('"').append(escape(Objects.toString(v))).append('"');
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String safeErrorMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) return "";
        // Basic sanitization: trim and limit length
        msg = msg.replaceAll("[\n\r\t]", " ");
        if (msg.length() > 500) msg = msg.substring(0, 500) + "…";
        return msg;
    }
}
