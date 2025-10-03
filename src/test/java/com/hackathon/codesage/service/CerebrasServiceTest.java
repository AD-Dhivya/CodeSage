
package com.hackathon.codesage.service;

import com.hackathon.codesage.service.CerebrasService;
import com.hackathon.codesage.service.VaultSecretService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CerebrasServiceTest {

    @Test
    void testCerebrasServiceConstructorWithVault() {
        // Mock Vault service
        VaultSecretService mockVaultService = Mockito.mock(VaultSecretService.class);
        when(mockVaultService.getCerebrasApiKey()).thenReturn("test-api-key");

        // Create service with mocked Vault
        CerebrasService service = new CerebrasService(mockVaultService);

        // If constructor succeeds, test passes
        assertNotNull(service);
        System.out.println("✅ CerebrasService constructor with Vault works");
    }
}
