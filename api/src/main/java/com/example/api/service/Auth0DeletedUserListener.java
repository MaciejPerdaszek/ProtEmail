package com.example.api.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.LogEventFilter;
import com.auth0.json.mgmt.logevents.LogEvent;
import com.auth0.json.mgmt.logevents.LogEventsPage;
import com.auth0.net.Request;
import com.example.api.repository.MailboxRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Auth0DeletedUserListener {

    private final ManagementAPI mgmt;

    private final MailboxRepository mailboxRepository;

    public Auth0DeletedUserListener(ManagementAPI mgmt, MailboxRepository mailboxRepository) {
        this.mgmt = mgmt;
        this.mailboxRepository = mailboxRepository;
    }

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void checkForDeletedUsers() {
        try {
            log.info("Checking for deleted users");
            LogEventFilter filter = new LogEventFilter();

            long now = System.currentTimeMillis();
            long oneHourAgo = now - (60 * 60 * 1000);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            String fromDate = sdf.format(new Date(oneHourAgo));
            String toDate = sdf.format(new Date(now));

            String query = String.format("description:*delete* AND description:*user* AND date:[%s TO %s]",
                    fromDate, toDate);

            filter.withQuery(query);
            Request<LogEventsPage> request = mgmt.logEvents().list(filter);
            LogEventsPage logEvents = request.execute().getBody();

            for (LogEvent event : logEvents.getItems()) {
                Map<String, Object> details = event.getDetails();
                if (details != null && details.containsKey("request")) {
                    String requestDetails = details.get("request").toString();
                    String userId = extractUserId(requestDetails);

                    if (userId != null) {
                        log.info("Found deleted user: {}", userId);
                        mailboxRepository.deleteByUserId(userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking for deleted users", e);
        }
    }

    private String extractUserId(String input) {
        String pathPrefix = "path=/api/v2/users/";
        int startIndex = input.indexOf(pathPrefix);
        if (startIndex == -1) return null;

        startIndex += pathPrefix.length();
        int endIndex = input.indexOf(",", startIndex);

        if (endIndex == -1) return null;

        String userId = input.substring(startIndex, endIndex);
        try {
            return URLDecoder.decode(userId, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decoding user ID: {}", userId, e);
            return userId;
        }
    }
}
