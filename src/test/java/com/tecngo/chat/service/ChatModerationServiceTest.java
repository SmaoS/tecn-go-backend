package com.tecngo.chat.service;

import com.tecngo.chat.entity.ChatModerationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModerationServiceTest {
    private final ChatModerationService service = new ChatModerationService();

    @Test
    void approvesOrdinaryServiceConversation() {
        var result = service.moderate("Voy en camino y llego en veinte minutos");

        assertThat(result.status()).isEqualTo(ChatModerationStatus.APPROVED);
    }

    @Test
    void blocksThreatsSexualContentAndFraudRisk() {
        assertThat(service.moderate("Te voy a matar").status())
                .isEqualTo(ChatModerationStatus.BLOCKED);
        assertThat(service.moderate("Envíame nudes").status())
                .isEqualTo(ChatModerationStatus.BLOCKED);
        assertThat(service.moderate("Pásame tu código de verificación").status())
                .isEqualTo(ChatModerationStatus.BLOCKED);
    }

    @Test
    void flagsOffensiveLanguageLinksAndSensitiveData() {
        assertThat(service.moderate("Eres un idiota").status())
                .isEqualTo(ChatModerationStatus.FLAGGED);
        assertThat(service.moderate("Escríbeme en https://bit.ly/pago").status())
                .isEqualTo(ChatModerationStatus.FLAGGED);
        assertThat(service.moderate("Mi correo es usuario@example.com").status())
                .isEqualTo(ChatModerationStatus.FLAGGED);
        assertThat(service.moderate("Mi celular es 310 123 4567").status())
                .isEqualTo(ChatModerationStatus.FLAGGED);
    }
}
