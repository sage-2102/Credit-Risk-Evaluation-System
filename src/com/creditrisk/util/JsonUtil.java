package com.creditrisk.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * High-performance, zero-dependency JSON parser and serializer for Java standard edition.
 */
public class JsonUtil {

    // ==========================================
    // SERIALIZATION
    // ==========================================

    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        toJson(obj, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void toJson(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
            return;
        }

        if (obj instanceof String) {
            sb.append('"').append(escapeJson((String) obj)).append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
        } else if (obj instanceof Enum<?>) {
            sb.append('"').append(((Enum<?>) obj).name()).append('"');
        } else if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                toJson(entry.getValue(), sb);
                first = false;
            }
            sb.append('}');
        } else if (obj instanceof Iterable<?>) {
            Iterable<?> list = (Iterable<?>) obj;
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                toJson(item, sb);
                first = false;
            }
            sb.append(']');
        } else if (obj.getClass().isArray()) {
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                toJson(java.lang.reflect.Array.get(obj, i), sb);
            }
            sb.append(']');
        } else {
            // POJO serialization via reflection
            sb.append('{');
            Field[] fields = obj.getClass().getDeclaredFields();
            boolean first = true;
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object val = field.get(obj);
                    if (!first) sb.append(',');
                    sb.append('"').append(escapeJson(field.getName())).append("\":");
                    toJson(val, sb);
                    first = false;
                } catch (Exception ignored) {
                }
            }
            sb.append('}');
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = String.format("\\u%04x", (int) c);
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==========================================
    // PARSING
    // ==========================================

    public static Object parse(String json) {
        if (json == null) return null;
        String trimmed = json.trim();
        if (trimmed.isEmpty()) return null;
        Parser parser = new Parser(trimmed);
        return parser.parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object res = parse(json);
        if (res instanceof Map<?, ?>) {
            return (Map<String, Object>) res;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object res = parse(json);
        if (res instanceof List<?>) {
            return (List<Object>) res;
        }
        return new ArrayList<>();
    }

    private static class Parser {
        private final String src;
        private int idx = 0;

        Parser(String src) {
            this.src = src;
        }

        private void skipWhitespace() {
            while (idx < src.length() && Character.isWhitespace(src.charAt(idx))) {
                idx++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (idx >= src.length()) return null;
            char c = src.charAt(idx);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"' || c == '\'') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            
            // Lenient unquoted string fallback
            int start = idx;
            while (idx < src.length() && src.charAt(idx) != ',' && src.charAt(idx) != '}' && src.charAt(idx) != ']' && !Character.isWhitespace(src.charAt(idx))) {
                idx++;
            }
            return src.substring(start, idx);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            idx++; // skip '{'
            skipWhitespace();
            if (idx < src.length() && src.charAt(idx) == '}') {
                idx++;
                return map;
            }

            while (idx < src.length()) {
                skipWhitespace();
                String key;
                if (src.charAt(idx) == '"' || src.charAt(idx) == '\'') {
                    key = parseString();
                } else {
                    int start = idx;
                    while (idx < src.length() && src.charAt(idx) != ':' && !Character.isWhitespace(src.charAt(idx))) {
                        idx++;
                    }
                    key = src.substring(start, idx).trim();
                }

                skipWhitespace();
                if (idx >= src.length() || src.charAt(idx) != ':') {
                    throw new IllegalArgumentException("Expected ':' at position " + idx);
                }
                idx++; // skip ':'
                Object value = parseValue();
                map.put(key, value);

                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == '}') {
                    idx++;
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + idx);
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            idx++; // skip '['
            skipWhitespace();
            if (idx < src.length() && src.charAt(idx) == ']') {
                idx++;
                return list;
            }

            while (idx < src.length()) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == ']') {
                    idx++;
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + idx);
                }
            }
            return list;
        }

        private String parseString() {
            idx++; // skip opening '"'
            StringBuilder sb = new StringBuilder();
            while (idx < src.length()) {
                char c = src.charAt(idx++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (idx >= src.length()) throw new IllegalArgumentException("Unterminated escape sequence");
                    char esc = src.charAt(idx++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (idx + 4 > src.length()) throw new IllegalArgumentException("Invalid unicode escape");
                            String hex = src.substring(idx, idx + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            idx += 4;
                            break;
                        default:
                            sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", idx)) {
                idx += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", idx)) {
                idx += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Expected boolean at position " + idx);
        }

        private Object parseNull() {
            if (src.startsWith("null", idx)) {
                idx += 4;
                return null;
            }
            throw new IllegalArgumentException("Expected null at position " + idx);
        }

        private Number parseNumber() {
            int start = idx;
            if (src.charAt(idx) == '-') idx++;
            while (idx < src.length() && (Character.isDigit(src.charAt(idx)) || src.charAt(idx) == '.' || src.charAt(idx) == 'e' || src.charAt(idx) == 'E' || src.charAt(idx) == '+' || src.charAt(idx) == '-')) {
                idx++;
            }
            String numStr = src.substring(start, idx);
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            } else {
                long val = Long.parseLong(numStr);
                if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                    return (int) val;
                }
                return val;
            }
        }
    }

    // Helper conversion methods for safe Map access
    public static String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : def;
    }

    public static double getDouble(Map<String, Object> map, String key, double def) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (Exception ignored) {}
        }
        return def;
    }

    public static int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception ignored) {}
        }
        return def;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return def;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map<?, ?>) return (Map<String, Object>) val;
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?>) return (List<Object>) val;
        return Collections.emptyList();
    }
}
