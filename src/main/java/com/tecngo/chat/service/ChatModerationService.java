package com.tecngo.chat.service;

import com.tecngo.chat.entity.ChatModerationStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ChatModerationService {
    private static final Pattern THREATS = Pattern.compile(
            "\\b(te voy a matar|te mato|voy a matarte|te voy a hacer dano|te encontrare|amenaza|kill you|hurt you)\\b");
    private static final Pattern SEXUAL = Pattern.compile(
            "\\b(desnud[oa]s?|sexo|sexual|pornografia|porno|nudes?|xxx|onlyfans|escort)\\b");
    private static final Pattern FRAUD = Pattern.compile(
            "\\b(estafa|fraude|suplantacion|documento falso|pago por fuera|evadir tecn?go|codigo de verificacion|contrasena|password)\\b");
    private static final Pattern OFFENSIVE = Pattern.compile(
            "\\b(imbecil|idiota|estupido|malparido|hijueputa|gonorrea|puta|marica)\\b");
    private static final Pattern LINK = Pattern.compile(
            "(https?://|www\\.|bit\\.ly|tinyurl\\.com|t\\.me/|wa\\.me/)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?57\\s?)?(?:3\\d{2})[\\s.-]?\\d{3}[\\s.-]?\\d{4}(?!\\d)");
    private static final Pattern CARD = Pattern.compile(
            "(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)");

    public ModerationResult moderate(String message) {
        String normalized = normalize(message);
        List<String> reasons = new ArrayList<>();
        if (THREATS.matcher(normalized).find()) reasons.add("Threat or intimidation");
        if (SEXUAL.matcher(normalized).find()) reasons.add("Sexual content");
        if (FRAUD.matcher(normalized).find()) reasons.add("Fraud or credential risk");
        if (!reasons.isEmpty()) {
            return new ModerationResult(ChatModerationStatus.BLOCKED, String.join("; ", reasons));
        }
        if (OFFENSIVE.matcher(normalized).find()) reasons.add("Offensive language");
        if (LINK.matcher(message).find()) reasons.add("External or suspicious link");
        if (EMAIL.matcher(message).find()) reasons.add("Email address");
        if (PHONE.matcher(message).find()) reasons.add("Phone number");
        if (CARD.matcher(message).find()) reasons.add("Possible financial data");
        return reasons.isEmpty()
                ? new ModerationResult(ChatModerationStatus.APPROVED, "Automatic rules approved")
                : new ModerationResult(ChatModerationStatus.FLAGGED, String.join("; ", reasons));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public record ModerationResult(ChatModerationStatus status, String reason) {}
}
