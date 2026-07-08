package com.creatorengine.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * Storage bucket name. Defaults to the project's default bucket (visible
     * in your frontend Firebase config as `storageBucket`). Override via the
     * FIREBASE_STORAGE_BUCKET env var / firebase.storage-bucket property if
     * you ever need a different bucket.
     */
    @Value("${firebase.storage-bucket:creatorengine-4eeed.firebasestorage.app}")
    private String storageBucketName;

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
                .setCredentials(credentials)
                .setStorageBucket(storageBucketName);

        if (StringUtils.hasText(props.getFirebase().getProjectId())) {
            builder.setProjectId(props.getFirebase().getProjectId());
        }

        FirebaseApp.initializeApp(builder.build());

        log.info("Firebase initialised for project '{}' with storage bucket '{}'.",
                props.getFirebase().getProjectId(), storageBucketName);
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    /**
     * Bean for uploading files (automation DM images, etc.) to Firebase
     * Storage from the backend. Files uploaded here can be made public and
     * referenced by URL, which is what Instagram's Send API requires for
     * image attachments (it fetches the URL server-side - no raw bytes).
     */
    @Bean
    public Bucket firebaseStorageBucket() {
        return StorageClient.getInstance().bucket();
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