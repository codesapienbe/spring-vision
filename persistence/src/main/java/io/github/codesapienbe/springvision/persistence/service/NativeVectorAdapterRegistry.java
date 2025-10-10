package io.github.codesapienbe.springvision.persistence.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry for discovering and accessing {@link NativeVectorAdapter} implementations.
 * This class is populated at startup with all available adapter beans and allows for easy lookup by provider ID.
 */
@Service
public class NativeVectorAdapterRegistry {

    private final Map<String, NativeVectorAdapter> byProvider;

    /**
     * Constructs a new registry and populates it with the provided list of adapters.
     *
     * @param adapters A list of {@link NativeVectorAdapter} beans discovered in the application context.
     */
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

    /**
     * Retrieves the adapter for a specific database provider.
     *
     * @param provider The ID of the provider (e.g., "postgres", "mysql").
     * @return The corresponding {@link NativeVectorAdapter}, or null if no adapter is found for the given provider.
     */
    public NativeVectorAdapter getAdapter(String provider) {
        if (provider == null) return null;
        return byProvider.get(provider.toLowerCase());
    }
}
