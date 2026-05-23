package com.creatorengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <ul>
 *   <li>{@code @EnableAsync} is kept on for future use, but the
 *       webhook → DM pipeline no longer relies on it — async-ness is
 *       provided by
 *       {@link com.creatorengine.automation.queue.QueueWorker}, which
 *       is the right primitive for production (swappable for Redis/SQS,
 *       bounded, observable via {@code /api/health}).</li>
 *   <li>{@code @EnableScheduling} is staged for the future Meta token
 *       refresh job (see
 *       {@code InstagramApiClient.refreshLongLivedTokenPlaceholder}).
 *       Wire a {@code @Scheduled} method when that ships.</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CreatorEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorEngineApplication.class, args);
    }
}
