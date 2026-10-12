package com.casino.platform.provider.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for managing provider adapters.
 * Automatically registers all ProviderAdapter beans and provides lookup by provider code.
 */
@Component
public class ProviderAdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(ProviderAdapterFactory.class);

    private final Map<String, ProviderAdapter> adapters = new HashMap<>();

    /**
     * Constructor that automatically registers all ProviderAdapter beans.
     * Spring will inject all implementations of ProviderAdapter interface.
     */
    public ProviderAdapterFactory(List<ProviderAdapter> adapterList) {
        for (ProviderAdapter adapter : adapterList) {
            String providerCode = adapter.getProviderCode();
            adapters.put(providerCode.toUpperCase(), adapter);
            log.info("Registered provider adapter: {}", providerCode);
        }
        log.info("Total {} provider adapters registered", adapters.size());
    }

    /**
     * Get adapter by provider code.
     *
     * @param providerCode Provider code (case-insensitive)
     * @return Provider adapter
     * @throws IllegalArgumentException if adapter not found
     */
    public ProviderAdapter getAdapter(String providerCode) {
        ProviderAdapter adapter = adapters.get(providerCode.toUpperCase());
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for provider: " + providerCode);
        }
        return adapter;
    }

    /**
     * Check if adapter exists for provider code.
     *
     * @param providerCode Provider code
     * @return true if adapter exists
     */
    public boolean hasAdapter(String providerCode) {
        return adapters.containsKey(providerCode.toUpperCase());
    }

    /**
     * Get all registered provider codes.
     *
     * @return Set of provider codes
     */
    public java.util.Set<String> getProviderCodes() {
        return adapters.keySet();
    }
}
