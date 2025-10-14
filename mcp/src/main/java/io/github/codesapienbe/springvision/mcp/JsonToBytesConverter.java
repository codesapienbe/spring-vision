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
 * - ToolContext -> extracts parameters and recursively processes
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
            return extractBytesFromMap(map);
        }

        if (input instanceof JsonNode node) {
            return jsonNodeToBytes(node);
        }

        // Handle ToolContext or any other object by converting to Map
        if (input.getClass().getName().contains("ToolContext")) {
            try {
                // Convert ToolContext to a map and process it
                Map<?, ?> contextMap = mapper.convertValue(input, Map.class);
                return extractBytesFromMap(contextMap);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to extract data from ToolContext: " + e.getMessage(), e);
            }
        }

        // Last resort: try converting via ObjectMapper to JsonNode
        try {
            JsonNode node = mapper.valueToTree(input);
            return jsonNodeToBytes(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported input type: " + input.getClass().getName());
        }
    }

    private static byte[] extractBytesFromMap(Map<?, ?> map) {
        // Try common image parameter names in order of priority
        String[] imageKeys = {"imageInput", "image", "imageBytes", "imageData", "data", "bytes"};

        for (String key : imageKeys) {
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value != null) {
                    try {
                        return toBytes(value);
                    } catch (Exception e) {
                        // Try the next key if this one fails
                    }
                }
            }
        }

        // If no specific image key found, look for any array-like or string value
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object v = e.getValue();
            if (v instanceof List<?> || v instanceof String || v instanceof byte[] || v instanceof JsonNode) {
                try {
                    return toBytes(v);
                } catch (Exception ex) {
                    // Try the next entry if this one fails
                }
            }
        }

        throw new IllegalArgumentException("Map provided but no usable image data found in keys: " + map.keySet());
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

            // Try other common image keys
            String[] imageKeys = {"imageInput", "image", "imageBytes", "imageData"};
            for (String key : imageKeys) {
                JsonNode imageNode = node.get(key);
                if (imageNode != null) {
                    return jsonNodeToBytes(imageNode);
                }
            }

            throw new IllegalArgumentException("JSON object did not contain a 'data' or image field with bytes");
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
