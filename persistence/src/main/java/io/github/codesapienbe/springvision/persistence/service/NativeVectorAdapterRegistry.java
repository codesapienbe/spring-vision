package io.github.codesapienbe.springvision.persistence.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry to find NativeVectorAdapter implementations by provider id.
 */
@Service
public class NativeVectorAdapterRegistry {

    private final Map<String, NativeVectorAdapter> byProvider;

    public NativeVectorAdapterRegistry(List<NativeVectorAdapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            this.byProvider = Collections.emptyMap();
        } else {
            Map<String, NativeVectorAdapter> m = new HashMap<>();
            for (NativeVectorAdapter a : adapters) {
                if (a != null && a.provider() != null) m.put(a.provider().toLowerCase(), a);
            }
            this.byProvider = Collections.unmodifiableMap(m);
        }
    }

    public NativeVectorAdapter getAdapter(String provider) {
        if (provider == null) return null;
        return byProvider.get(provider.toLowerCase());
    }
}
