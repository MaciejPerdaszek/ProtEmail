package com.example.api.dto;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.BodyPart;
import java.io.IOException;
import com.example.api.exception.EmailProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public record EmailContent(
        String username,
        String messageId,
        String content,
        String subject,
        Address[] from
) {
    public static EmailContent fromMessage(Message message, String username, String messageId) {
        try {
            if (message == null) {
                throw new IllegalArgumentException("Message cannot be null");
            }

            String extractedContent = extractContent(message);
            log.info("Successfully extracted content for message: {}", extractedContent);

            return new EmailContent(
                    username,
                    messageId,
                    extractedContent,
                    message.getSubject(),
                    message.getFrom()
            );
        } catch (MessagingException e) {
            log.error("Failed to process email message with ID: {}. Error: {}", messageId, e.getMessage(), e);
            throw new EmailProcessingException("Failed to process email message", e);
        } catch (IOException e) {
            log.error("IO error while processing email with ID: {}. Error: {}", messageId, e.getMessage(), e);
            throw new EmailProcessingException("IO error while processing email", e);
        } catch (Exception e) {
            log.error("Unexpected error while processing email with ID: {}. Error: {}", messageId, e.getMessage(), e);
            throw new EmailProcessingException("Unexpected error while processing email", e);
        }
    }

    private static String extractContent(Message message) throws MessagingException, IOException {
        try {
            Object content = message.getContent();

            if (content instanceof String) {
                return handleStringContent(message, (String) content);
            } else if (content instanceof Multipart) {
                return extractMultipartContent((Multipart) content);
            }

            log.warn("Unsupported content type: {}", content.getClass().getName());
            return "";
        } catch (MessagingException | IOException e) {
            log.error("Error extracting content from message: {}", e.getMessage());
            throw e;
        }
    }

    private static String handleStringContent(Message message, String content) throws MessagingException {
        try {
            String contentType = message.getContentType().toLowerCase();

            if (contentType.contains("text/plain")) {
                return content;
            } else if (contentType.contains("text/html")) {
                return cleanHtmlContent(content);
            }

            log.warn("Unhandled string content type: {}", contentType);
            return content;
        } catch (MessagingException e) {
            log.error("Error determining content type: {}", e.getMessage());
            throw e;
        }
    }

    private static String extractMultipartContent(Multipart multipart) throws MessagingException, IOException {
        try {
            String plainText = null;
            String htmlContent = null;

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart == null) {
                    log.warn("Null body part found at index: {}", i);
                    continue;
                }

                String contentType = bodyPart.getContentType().toLowerCase();
                Object content = bodyPart.getContent();

                if (content == null) {
                    log.warn("Null content found in body part at index: {}", i);
                    continue;
                }

                if (contentType.contains("text/plain")) {
                    plainText = content.toString();
                } else if (contentType.contains("text/html")) {
                    htmlContent = cleanHtmlContent(content.toString());
                }
            }

            if (plainText == null && htmlContent == null) {
                log.warn("No text or HTML content found in multipart message");
            }

            return htmlContent != null ? htmlContent : (plainText != null ? plainText : "");
        } catch (MessagingException | IOException e) {
            log.error("Error processing multipart content: {}", e.getMessage());
            throw e;
        }
    }

    private static String cleanHtmlContent(String html) {
        try {
            if (html == null) {
                log.warn("Null HTML content provided");
                return "";
            }
            Document doc = Jsoup.parse(html);
            return doc.text();
        } catch (Exception e) {
            log.error("Error cleaning HTML content: {}", e.getMessage());
            return html;
        }
    }
}
