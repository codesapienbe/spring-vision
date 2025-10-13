package io.github.codesapienbe.springvision.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility to coerce various JSON-style inputs into a byte[] payload.
 * <p>
 * Supported input shapes:
 * - byte[] -> returned as-is
 * - String -> treated as Base64 if decodable; otherwise parsed as JSON array of numbers
 * - List/Array -> numeric array (0-255 or -128..127) converted to bytes
 * - Map with key "data" -> value is recursively converted
 * - Jackson JsonNode -> handles binary nodes, arrays of numbers
 */
public final class JsonToBytesConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonToBytesConverter() {
        // utility
    }

    public static byte[] toBytes(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        if (input instanceof byte[] b) {
            return b;
        }

        if (input instanceof String s) {
            s = s.trim();

            // Try base64 first (common case)
            try {
                byte[] decoded = Base64.getDecoder().decode(s);
                if (decoded.length > 0) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // not base64 - try JSON array
            }

            // If it looks like a JSON array, parse as numbers
            if (s.startsWith("[")) {
                try {
                    Integer[] arr = mapper.readValue(s, Integer[].class);
                    return intsToBytes(arr);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("String provided looks like JSON array but failed to parse: " + ex.getMessage(), ex);
                }
            }

            // As a fallback, try to parse as JSON object with data field
            try {
                JsonNode node = mapper.readTree(s);
                return jsonNodeToBytes(node);
            } catch (Exception ignored) {
                // Not JSON; fall through
            }

            throw new IllegalArgumentException("String input is neither base64 nor JSON array/object containing raw bytes");
        }

        if (input instanceof List<?> list) {
            // try to interpret as numbers
            return listToBytes(list);
        }

        if (input instanceof Map<?, ?> map) {
            // common shape: { "data": [ ... ] } or { "data": "base64..." }
            if (map.containsKey("data")) {
                return toBytes(map.get("data"));
            }
            // Handle ToolContext - look for common image parameter names
            if (map.containsKey("imageInput") || map.containsKey("image") || map.containsKey("imageBytes")) {
                Object imageData = map.get("imageInput");
                if (imageData == null) imageData = map.get("image");
                if (imageData == null) imageData = map.get("imageBytes");
                return toBytes(imageData);
            }
            // try to find the first array-like entry
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object v = e.getValue();
                if (v instanceof List<?> || v instanceof String || v instanceof byte[] || v instanceof JsonNode) {
                    return toBytes(v);
                }
            }
            throw new IllegalArgumentException("Map provided but no 'data', 'imageInput', 'image', or 'imageBytes' field or usable payload found");
        }

        if (input instanceof JsonNode node) {
            return jsonNodeToBytes(node);
        }

        // Last resort: try converting via ObjectMapper to JsonNode
        try {
            JsonNode node = mapper.valueToTree(input);
            return jsonNodeToBytes(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported input type: " + input.getClass().getName());
        }
    }

    private static byte[] listToBytes(List<?> list) {
        byte[] out = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object v = list.get(i);
            out[i] = numberToByte(v, i);
        }
        return out;
    }

    private static byte[] intsToBytes(Integer[] arr) {
        byte[] out = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            Integer val = arr[i];
            if (val == null) throw new IllegalArgumentException("Null value at index " + i);
            out[i] = (byte) (val & 0xFF);
        }
        return out;
    }

    private static byte[] jsonNodeToBytes(JsonNode node) {
        if (node.isBinary()) {
            try {
                return node.binaryValue();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to extract binary node: " + e.getMessage(), e);
            }
        }

        if (node.isTextual()) {
            String s = node.textValue();
            return toBytes(s);
        }

        if (node.isArray()) {
            byte[] out = new byte[node.size()];
            int i = 0;
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                JsonNode el = it.next();
                if (!el.isNumber()) throw new IllegalArgumentException("JSON array must contain only numeric values");
                out[i++] = (byte) (el.intValue() & 0xFF);
            }
            return out;
        }

        if (node.isObject()) {
            JsonNode data = node.get("data");
            if (data != null) return jsonNodeToBytes(data);
            throw new IllegalArgumentException("JSON object did not contain a 'data' field with bytes");
        }

        throw new IllegalArgumentException("Unrecognized JsonNode type for conversion to bytes");
    }

    private static byte numberToByte(Object v, int index) {
        if (v == null) throw new IllegalArgumentException("Null value at index " + index);
        if (v instanceof Number n) {
            return (byte) (n.intValue() & 0xFF);
        }
        if (v instanceof String s) {
            try {
                int iv = Integer.parseInt(s.trim());
                return (byte) (iv & 0xFF);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numeric string at index " + index + ": " + s);
            }
        }
        throw new IllegalArgumentException("Unsupported element type in numeric array at index " + index + ": " + v.getClass().getName());
    }
}
