package com.razza.bookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for sending emails to users.
 *
 * it handles event update notifications
 *
 * Uses asynchronous execution to avoid blocking request threads.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM = "noreply@bookingsystem.com";

    /**
     * Sends an email notification to a user when an event is updated.
     *
     * The email contains:
     * - updated event date
     * - updated event location
     *
     * This method runs asynchronously.
     *
     * @param to recipient email address
     * @param newDate updated event date
     * @param newLocation updated event location
     * @throws RuntimeException if email sending fails
     */
    @Async
    public void sendEventUpdateEmail(
            String to,
            OffsetDateTime newDate,
            String newLocation
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject("Event Update Notification");

            String formattedDate = newDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            String htmlContent = buildHtmlContent(formattedDate, newLocation);

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }

    /**
     * Builds the HTML body for the event update email.
     *
     * @param date formatted event date string
     * @param location event location
     * @return HTML email content as a string
     */
    private String buildHtmlContent(String date, String location) {
        return """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2> Event Updated</h2>
                        <p>Hello,</p>
                        <p>An event you booked has been updated:</p>
                        <ul>
                            <li><strong> New Date:</strong> %s</li>
                            <li><strong> New Location:</strong> %s</li>
                        </ul>
                        <p>Please check your booking for more details.</p>
                        <br/>
                        <p>Regards,<br/>Booking System</p>
                    </body>
                </html>
                """.formatted(date, location);
    }
}