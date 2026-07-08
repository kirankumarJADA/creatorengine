package com.creatorengine.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final AppProperties props;

    public FirebaseConfig(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp already initialised.");
            return;
        }

        GoogleCredentials credentials = loadCredentials();

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials);

        if (StringUtils.hasText(props.getFirebase().getProjectId())) {
            builder.setProjectId(props.getFirebase().getProjectId());
        }

        FirebaseApp.initializeApp(builder.build());

        log.info("Firebase initialised for project '{}'.",
                props.getFirebase().getProjectId());
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String json = props.getFirebase().getCredentialsJson();

        if (StringUtils.hasText(json)) {
            log.info("Loading Firebase credentials from FIREBASE_CREDENTIALS_JSON.");
            try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(in);
            }
        }

        String path = props.getFirebase().getCredentialsPath();

        if (StringUtils.hasText(path)) {
            log.info("Loading Firebase credentials from '{}'.", path);

            Resource resource = path.startsWith("classpath:")
                    ? new ClassPathResource(path.substring("classpath:".length()))
                    : new FileSystemResource(path);

            try (InputStream in = resource.getInputStream()) {
                return GoogleCredentials.fromStream(in);
            }
        }

        log.warn("No Firebase credentials configured - falling back to Application Default Credentials.");
        return GoogleCredentials.getApplicationDefault();
    }
}