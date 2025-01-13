package com.example.api.dto;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.BodyPart;
import java.io.IOException;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Slf4j
public record EmailContent(
        String username,
        String messageId,
        String content,
        String subject,
        Address[] from
) {
    public static EmailContent fromMessage(Message message, String username, String messageId)
            throws MessagingException, IOException {
        String extractedContent = extractContent(message);
        String cleanedContent = cleanContent(extractedContent);

        return new EmailContent(
                username,
                messageId,
                cleanedContent,
                message.getSubject(),
                message.getFrom()
        );
    }

    private static String extractContent(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        log.info("Content type: {}", content != null ? content.getClass().getName() : "null");

        switch (content) {
            case null -> {
                return "";
            }

            case String stringContent -> {
                return stringContent;
            }
            case Multipart multipart -> {
                return extractMultipartContent(multipart);
            }
            default -> {
            }
        }

        log.warn("Nieoczekiwany typ zawartości: {}", content.getClass());

        return content.toString();
    }

    private static String extractMultipartContent(Multipart multipart) throws MessagingException, IOException {
        StringBuilder contentBuilder = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType().toLowerCase();

            log.info("Bodypart content type: {}", contentType);

            try {
                Object partContent = bodyPart.getContent();

                if (partContent instanceof String stringContent) {
                    contentBuilder.append(stringContent).append("\n");
                } else if (partContent instanceof Multipart nestedMultipart) {
                    contentBuilder.append(extractMultipartContent(nestedMultipart)).append("\n");
                }
            } catch (Exception e) {
                log.error("Błąd ekstrakcji części wiadomości", e);
            }
        }

        return contentBuilder.toString().trim();
    }

    private static String cleanContent(String content) {
        if (content == null) return "";

        String cleanedHtml = cleanHtmlContent(content);
        String cleanedText = cleanedHtml.trim().replaceAll("\\s+", " ");
        cleanedText = removeDuplicateLinks(cleanedText);

        return cleanedText;
    }

    private static String cleanHtmlContent(String content) {
        if (content == null) return "";
        return Jsoup.clean(content, Safelist.none());
    }

    private static String removeDuplicateLinks(String content) {
        if (content == null) return "";

        return Pattern.compile("(https?://\\S+)")
                .matcher(content)
                .replaceAll("$1");
    }
}