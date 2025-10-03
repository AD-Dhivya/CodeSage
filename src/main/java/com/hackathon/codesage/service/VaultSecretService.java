
package com.hackathon.codesage.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;
import java.util.Optional;

@Service
public class VaultSecretService {

    @Value("${vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${vault.address:http://127.0.0.1:8200}")
    private String vaultAddress;

    @Value("${vault.token:root}")
    private String vaultToken;

    @Value("${vault.secret.path:secret/data/codesage}")
    private String secretPath;

    @Value("${cerebras.api.key:}")
    private String cerebrasApiKeyProperty;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getCerebrasApiKey() {
        // 1) If explicitly provided via env/property, prefer that
        String apiKeyFromEnv = System.getenv("CEREBRAS_API_KEY");
        String apiKeyDirect = Optional.ofNullable(apiKeyFromEnv)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> Optional.ofNullable(cerebrasApiKeyProperty).orElse(""));
        if (!apiKeyDirect.isBlank()) {
            System.out.println("🔑 Using Cerebras API key from env/property");
            return apiKeyDirect;
        }

        // 2) If Vault is disabled, stop here
        if (!vaultEnabled) {
            System.err.println("⚠️ Vault disabled and no API key provided via env/property");
            return "";
        }

        // 3) Try Vault
        String url = vaultAddress + "/v1/" + secretPath;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", vaultToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Vault response body is null");
            }
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) {
                throw new RuntimeException("No data in Vault response");
            }
            Map<String, String> secretData = (Map<String, String>) data.get("data");
            String apiKey = secretData.get("CEREBRAS_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("CEREBRAS_API_KEY not found in Vault");
            }
            System.out.println("✅ Successfully retrieved API key from Vault");
            return apiKey;
        } catch (Exception e) {
            System.err.println("❌ Failed to retrieve API key from Vault: " + e.getMessage());
            return "";
        }
    }
}
