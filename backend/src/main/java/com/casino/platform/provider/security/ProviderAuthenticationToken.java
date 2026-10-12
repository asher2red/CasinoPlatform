package com.casino.platform.provider.security;

import com.casino.platform.provider.entity.Provider;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security authentication token for provider requests.
 * Holds authenticated provider information.
 */
public class ProviderAuthenticationToken extends AbstractAuthenticationToken {

    private final Provider provider;
    private final String playerProviderId;

    /**
     * Create unauthenticated token (before authentication).
     */
    public ProviderAuthenticationToken(String playerProviderId) {
        super(Collections.emptyList());
        this.provider = null;
        this.playerProviderId = playerProviderId;
        setAuthenticated(false);
    }

    /**
     * Create authenticated token (after successful authentication).
     */
    public ProviderAuthenticationToken(
            Provider provider,
            String playerProviderId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.provider = provider;
        this.playerProviderId = playerProviderId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getPlayerProviderId() {
        return playerProviderId;
    }
}
