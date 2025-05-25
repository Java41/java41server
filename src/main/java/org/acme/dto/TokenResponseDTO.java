package org.acme.dto;


import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Ответ с токенами")
public class TokenResponseDTO {
    @Schema(description = "JWT access token", examples = "eyJhbG...")
    public String accessToken;

    @Schema(description = "Refresh token", examples = "f47ac10b-58cc-...")
    public String refreshToken;

    public TokenResponseDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
