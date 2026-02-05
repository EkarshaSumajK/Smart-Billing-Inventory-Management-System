package com.smartretail.backend.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.net.ssl.SSLContext;
import jakarta.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "smartretail";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        try {
            // Use simple creation to match TestMongoDriver.java which worked
            System.out.println("Creating MongoClient with URI: " + mongoUri);
            return MongoClients.create(mongoUri);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MongoDB client", e);
        }
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @PostConstruct
    public void testConnection() {
        try {
            System.out.println("Testing MongoDB connection...");
            MongoClient client = mongoClient();
            client.getDatabase(getDatabaseName()).listCollectionNames().first();
            System.out.println("MongoDB connection test PASSED!");
        } catch (Exception e) {
            System.err.println("MongoDB connection test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
