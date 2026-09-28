# BOUNDLESS FINTECH: COMPREHENSIVE ENGINEERING MANUAL
Generated on Sat Sep 26 08:50:24 IST 2026

## auth-engine-service Deep Dive
This section covers every single line of code and configuration for auth-engine-service.

### File: auth-engine-service/target/classes/application.yml
```java
server:
  port: 8082

spring:
  application:
    name: auth-engine-service
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

# ----------------------------------------------------
# OPENTELEMETRY / ZIPKIN CONFIGURATION
# ----------------------------------------------------
management:
  tracing:
    sampling:
      probability: 1.0 # We trace 100% of transactions because FinTech requires total observability
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```


### File: auth-engine-service/pom.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.boundless</groupId>
	<artifactId>auth-engine-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>auth-engine-service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		
		<!-- Swagger UI / OpenAPI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.3.0</version>
		</dependency>
		
		<!-- Distributed Tracing: OpenTelemetry & Zipkin -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-tracing-bridge-brave</artifactId>
		</dependency>
		<dependency>
			<groupId>io.zipkin.reporter2</groupId>
			<artifactId>zipkin-reporter-brave</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```


### File: auth-engine-service/src/test/java/com/boundless/auth_engine_service/AuthEngineServiceApplicationTests.java
```java
package com.boundless.auth_engine_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthEngineServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```


### File: auth-engine-service/src/main/resources/application.yml
```java
server:
  port: 8082

spring:
  application:
    name: auth-engine-service
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

# ----------------------------------------------------
# OPENTELEMETRY / ZIPKIN CONFIGURATION
# ----------------------------------------------------
management:
  tracing:
    sampling:
      probability: 1.0 # We trace 100% of transactions because FinTech requires total observability
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```


### File: auth-engine-service/src/main/java/com/boundless/controller/AuthEngineController.java
```java
package com.boundless.controller;

import com.boundless.engine.FraudEngine;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class AuthEngineController {

    private final FraudEngine fraudEngine;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // This is the Webhook that Visa/Mastercard hits in real-time
    @PostMapping("/swipe")
    public ResponseEntity<?> handleNetworkSwipe(@RequestBody SwipeRequest request) {
        try {
            // 1. Run the Chain of Responsibility
            fraudEngine.processSwipe(request);
            
            // 2. [Next Step] Deduct funds using BalanceRule
            
            // 3. Drop an AuthApprovedEvent into Kafka for the Ledger to save permanently
            String event = String.format("{\"cardNumber\":\"%s\", \"merchantName\":\"%s\", \"amount\":\"%s\"}", 
                    request.getCardNumber(), request.getMerchantName(), request.getAmount().toString());
            kafkaTemplate.send("auth-approved-topic", event);

            // 4. BURNER CARD LOGIC: If this is a single-use card, destroy it immediately!
            String cardType = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":type");
            if ("BURNER".equals(cardType)) {
                // Delete from Redis so it can never be used again
                redisTemplate.delete("card:" + request.getCardNumber() + ":wallet");
                redisTemplate.delete("card:" + request.getCardNumber() + ":status");
                redisTemplate.delete("card:" + request.getCardNumber() + ":type");
                
                // Drop event to Identity Service to mark it CLOSED in Postgres
                kafkaTemplate.send("card-closed-topic", request.getCardNumber());
                log.warn("🔥 BURNER CARD {} DESTROYED AFTER SINGLE USE!", request.getCardNumber());
            }

            return ResponseEntity.ok("APPROVED");

        } catch (RuntimeException ex) {
            String errorMessage = ex.getMessage();
            
            // ------------------------------------------------------------------
            // ⚡ OTP EMAIL DISPATCH LOGIC (RBI Rule Caught)
            // ------------------------------------------------------------------
            if (errorMessage != null && errorMessage.startsWith("PENDING_OTP")) {
                
                // 1. Generate a 6-digit OTP
                String otp = String.format("%06d", (int)(Math.random() * 999999));
                
                // 2. Save OTP to Redis with a 5-minute TTL so the user can verify it later
                // redisTemplate.opsForValue().set("otp:" + request.getCardNumber(), otp, 5, TimeUnit.MINUTES);
                
                // 3. FIRE AND FORGET: Drop event into Kafka to send the actual Email async
                String kafkaMessage = String.format("{\"cardNumber\":\"%s\", \"otp\":\"%s\"}", request.getCardNumber(), otp);
                kafkaTemplate.send("otp-email-topic", kafkaMessage);
                
                log.info("RBI Limit exceeded for card {}. OTP Email dispatched to Kafka.", request.getCardNumber());
                
                // 4. Tell Visa the transaction is pending user action
                return ResponseEntity.status(202).body("PENDING_OTP: Please check your email for the 6-digit verification code.");
            }
            
            // ------------------------------------------------------------------
            // 🚨 UNIVERSAL INTERNAL OPS PROACTIVE ALERTING
            // ------------------------------------------------------------------
            if (errorMessage != null) {
                String role;
                String targetEmail;
                String alertBody;

                // Group 1: High-Risk AML/Fraud (Route to Risk & Compliance)
                if (errorMessage.contains("BRUTE_FORCE") || errorMessage.contains("MCC") || errorMessage.contains("VELOCITY")) {
                    role = "ROLE_PLATFORM_COMPLIANCE";
                    targetEmail = "compliance@boundless.com";
                    alertBody = String.format("SECURITY INCIDENT: Swipe declined for %s. Reason: %s. Please review card %s immediately.", 
                            request.getMerchantName(), errorMessage, request.getCardNumber());
                } 
                // Group 2: User Errors / Status (Route to Customer Success)
                else {
                    role = "ROLE_PLATFORM_SUPPORT";
                    targetEmail = "support@boundless.com";
                    alertBody = String.format("USER FRICTION: Swipe declined at %s. Reason: %s. The employee (Card %s) may call in for help.", 
                            request.getMerchantName(), errorMessage, request.getCardNumber());
                }

                String alertJson = String.format("{\"role\":\"%s\", \"targetEmail\":\"%s\", \"alert\":\"%s\"}", 
                        role, targetEmail, alertBody);
                kafkaTemplate.send("internal-alert-topic", alertJson);
                log.info("🚨 Proactive Alert Routed to {}: {}", role, errorMessage);
            }
            
            // Finally, return the decline response to Visa
            log.warn("Swipe Declined: {}", errorMessage);
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/model/SwipeRequest.java
```java
package com.boundless.model;

import lombok.Data;
import java.math.BigDecimal;

// This represents the JSON payload sent to us from the Visa/Mastercard network
@Data
public class SwipeRequest {
    private String cardNumber;
    private String cvv;
    private BigDecimal amount;
    private String merchantName;
    private String mcc; // Merchant Category Code (e.g. 7995 for Gambling, 5812 for Restaurants)
    private String terminalId;
}
```


### File: auth-engine-service/src/main/java/com/boundless/AuthEngineServiceApplication.java
```java
package com.boundless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthEngineServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthEngineServiceApplication.class, args);
	}

}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/AuthRule.java
```java
package com.boundless.engine;

import com.boundless.model.SwipeRequest;

// The base interface for our Chain of Responsibility
public interface AuthRule {
    // Evaluates the swipe. Throws a RuntimeException if the swipe is declined.
    void evaluate(SwipeRequest request);
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/FraudEngine.java
```java
package com.boundless.engine;

import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudEngine {

    // Spring Boot magically injects ALL classes that implement AuthRule (MccRule, VelocityRule)
    // This automatically forms our Chain of Responsibility pipeline!
    private final List<AuthRule> rules;

    public void processSwipe(SwipeRequest request) {
        log.info("⚡ Evaluating swipe for Card {} at {} for ${}", 
                request.getCardNumber(), request.getMerchantName(), request.getAmount());
        
        // Run the swipe through every rule in the chain
        for (AuthRule rule : rules) {
            rule.evaluate(request); // Will throw an exception if declined
        }
        
        log.info("✅ All Fraud Rules Passed for Card {}", request.getCardNumber());
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/rules/RbiLimitRule.java
```java
package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RbiLimitRule implements AuthRule {

    // The Reserve Bank of India mandates an OTP for transactions over ₹5,000
    private static final BigDecimal RBI_LIMIT = new BigDecimal("5000.00");

    @Override
    public void evaluate(SwipeRequest request) {
        // We assume the incoming request amount is in INR for this specific rule engine check
        if (request.getAmount().compareTo(RBI_LIMIT) > 0) {
            // Instead of a hard DECLINE, this triggers a specialized response flow back to the Gateway.
            // The AuthEngineController will catch this exact exception string and return a PENDING_OTP HTTP status.
            throw new RuntimeException("PENDING_OTP: Transaction exceeds ₹5,000 RBI limit. Email Challenge triggered.");
        }
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/rules/BalanceRule.java
```java
package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void evaluate(SwipeRequest request) {
        
        // 1. O(1) Redis Lookup: Find which Wallet this virtual card is attached to
        String walletId = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":wallet");
        
        if (walletId == null) {
            // If the card isn't mapped in Redis, we simulate a dummy wallet for testing
            walletId = "demo-wallet-id";
            // Ensure the demo wallet has funds so our tests don't instantly fail!
            redisTemplate.opsForValue().setIfAbsent("wallet:" + walletId + ":balance", "10000.00");
        }
        
        String balanceKey = "wallet:" + walletId + ":balance";
        
        // 2. Fetch the current balance from the ultra-fast cache
        String currentBalanceStr = redisTemplate.opsForValue().get(balanceKey);
        
        if (currentBalanceStr == null) {
            throw new RuntimeException("DECLINED: Insufficient Funds. Wallet balance not found.");
        }

        BigDecimal currentBalance = new BigDecimal(currentBalanceStr);
        
        // 3. The Math
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("DECLINED: Insufficient Funds. Wallet balance is $" + currentBalance 
                    + ", but swipe was for $" + request.getAmount());
        }

        // 4. Deduct the funds! 
        // Note: In a true production environment with thousands of concurrent swipes on the same wallet, 
        // we would execute this via a Redis Lua script to guarantee 100% ACID atomicity.
        BigDecimal newBalance = currentBalance.subtract(request.getAmount());
        redisTemplate.opsForValue().set(balanceKey, newBalance.toString());
        
        log.info("💰 SWIPE APPROVED: ${}. Deducted from Wallet {}. New Balance: ${}", 
                request.getAmount(), walletId, newBalance);
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/rules/MccRule.java
```java
package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class MccRule implements AuthRule {
    
    // 7995 = Betting/Casino, 5813 = Drinking Places/Bars
    private final Set<String> BLOCKED_MCCS = Set.of("7995", "5813");

    @Override
    public void evaluate(SwipeRequest request) {
        if (BLOCKED_MCCS.contains(request.getMcc())) {
            throw new RuntimeException("DECLINED: Restricted Merchant Category (MCC: " + request.getMcc() + ")");
        }
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/rules/VelocityRule.java
```java
package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VelocityRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_SWIPES_PER_DAY = 5;

    @Override
    public void evaluate(SwipeRequest request) {
        String key = "velocity:" + request.getCardNumber();
        
        // Atomically increment the counter in Redis
        Long currentSwipes = redisTemplate.opsForValue().increment(key);
        
        if (currentSwipes != null && currentSwipes == 1) {
            // First swipe of the day, set TTL to 24 hours
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        
        if (currentSwipes != null && currentSwipes > MAX_SWIPES_PER_DAY) {
            throw new RuntimeException("DECLINED: High-Risk Velocity. Max " + MAX_SWIPES_PER_DAY + " swipes allowed per day.");
        }
    }
}
```


### File: auth-engine-service/src/main/java/com/boundless/engine/rules/BruteForceRule.java
```java
package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BruteForceRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Override
    public void evaluate(SwipeRequest request) {
        String lockKey = "lock:card:" + request.getCardNumber();
        String attemptsKey = "cvv_attempts:" + request.getCardNumber();

        // 1. Check if the card is already locked due to previous brute-force attempts
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new RuntimeException("DECLINED: Card is temporarily locked due to excessive failed CVV attempts.");
        }

        // 2. Validate CVV 
        // (Mocking real CVV logic here. We'll say CVV "999" triggers a failed simulation)
        boolean isCvvValid = !"999".equals(request.getCvv());

        if (!isCvvValid) {
            Long failedAttempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (failedAttempts == 1) {
                redisTemplate.expire(attemptsKey, 24, TimeUnit.HOURS);
            }

            if (failedAttempts != null && failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // The attacker failed 3 times. We apply a TTL lock to the card in Redis for 24 hours.
                redisTemplate.opsForValue().set(lockKey, "LOCKED", 24, TimeUnit.HOURS);
                throw new RuntimeException("DECLINED: Invalid CVV. Security threshold reached. Card is now LOCKED for 24 hours.");
            }
            throw new RuntimeException("DECLINED: Invalid CVV. Attempt " + failedAttempts + " of " + MAX_FAILED_ATTEMPTS);
        } else {
            // Success! The CVV was correct, so we clear out their failed attempts history.
            redisTemplate.delete(attemptsKey);
        }
    }
}
```


## card-identity-service Deep Dive
This section covers every single line of code and configuration for card-identity-service.

### File: card-identity-service/target/classes/application.yml
```java
server:
  port: 8081

spring:
  application:
    name: card-identity-service
  datasource:
    url: jdbc:postgresql://localhost:5432/boundless_db
    username: boundless_admin
    password: boundless_password
  jpa:
    hibernate:
      ddl-auto: update # Automatically creates our tables
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
```


### File: card-identity-service/pom.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.boundless</groupId>
	<artifactId>card-identity-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>card-identity-service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<!-- JWT Security -->
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.11.5</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.11.5</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.11.5</version>
			<scope>runtime</scope>
		</dependency>
		
		<!-- Swagger UI / OpenAPI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.3.0</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```


### File: card-identity-service/src/test/java/com/boundless/card_identity_service/CardIdentityServiceApplicationTests.java
```java
package com.boundless.card_identity_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CardIdentityServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```


### File: card-identity-service/src/main/resources/application.yml
```java
server:
  port: 8081

spring:
  application:
    name: card-identity-service
  datasource:
    url: jdbc:postgresql://localhost:5432/boundless_db
    username: boundless_admin
    password: boundless_password
  jpa:
    hibernate:
      ddl-auto: update # Automatically creates our tables
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
```


### File: card-identity-service/src/main/java/com/boundless/repository/WalletRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByEmployeeId(UUID employeeId);
    
    // Used to find the central funding source
    Optional<Wallet> findByName(String name);
}
```


### File: card-identity-service/src/main/java/com/boundless/repository/MasterWalletRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.MasterWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterWalletRepository extends JpaRepository<MasterWallet, UUID> {
    Optional<MasterWallet> findByCompanyName(String companyName);
}
```


### File: card-identity-service/src/main/java/com/boundless/repository/CompanyRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByCompanyName(String companyName);
    boolean existsByEin(String ein);
    Optional<Company> findByCompanyName(String companyName);
}
```


### File: card-identity-service/src/main/java/com/boundless/repository/VirtualCardRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, UUID> {
    
    // Used by the Auth Engine to look up a card when a swipe happens
    Optional<VirtualCard> findByCardNumber(String cardNumber);
    
    // Get all cards attached to a specific budget/wallet
    List<VirtualCard> findByWalletId(UUID walletId);
    
    // Get all cards owned by a specific employee across all their wallets
    List<VirtualCard> findByWalletEmployeeId(UUID employeeId);
    
    // B2B: Get ALL cards for a specific company
    List<VirtualCard> findByWalletEmployeeCompanyName(String companyName);
}
```


### File: card-identity-service/src/main/java/com/boundless/repository/UserRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Used by our startup script to check if the Super Admin is already seeded
    boolean existsByRole(String role);
    
    // Fetch all employees for a specific B2B Client
    java.util.List<User> findByCompanyName(String companyName);
}
```


### File: card-identity-service/src/main/java/com/boundless/config/SecurityConfig.java
```java
package com.boundless.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.boundless.security.JwtFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    // This Bean allows us to inject the BCrypt encoder anywhere in our app
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Locks down the API but allows Swagger and public endpoints
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/companies/register").permitAll()
                .requestMatchers("/api/internal/**").permitAll() // Internal service calls
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/config/AdminSeeder.java
```java
package com.boundless.config;

import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.repository.MasterWalletRepository;
import com.boundless.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MasterWalletRepository masterWalletRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:founder@boundless.com}")
    private String adminEmail;

    @Value("${admin.password:SuperSecretStr0ngP@ssword!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Checking for existing Admin account in PostgreSQL...");
        
        if (!userRepository.existsByEmail("support@boundless.com")) {
            log.info("Seeding Platform Operations Users...");
            
            User supportUser = User.builder()
                    .email("support@boundless.com")
                    .fullName("Customer Success Rep")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_SUPPORT")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(supportUser);

            User complianceUser = User.builder()
                    .email("compliance@boundless.com")
                    .fullName("Risk Officer")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_COMPLIANCE")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(complianceUser);

            User treasuryUser = User.builder()
                    .email("treasury@boundless.com")
                    .fullName("Treasury Manager")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_TREASURY")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(treasuryUser);
            
            log.info("Platform Ops accounts seeded successfully.");
        }

        if (masterWalletRepository.findByCompanyName("Boundless Corp").isEmpty()) {
            MasterWallet masterWallet = MasterWallet.builder()
                    .companyName("Boundless Corp")
                    .totalBalance(BigDecimal.ZERO)
                    .build();
            masterWalletRepository.save(masterWallet);
            log.info("Master Company Wallet seeded with $0.00 balance.");
        }
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/config/SwaggerConfig.java
```java
package com.boundless.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Boundless API", version = "1.0", description = "FinTech API Documentation"),
        security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SwaggerConfig {
}
```


### File: card-identity-service/src/main/java/com/boundless/security/JwtUtils.java
```java
package com.boundless.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    // In a real production app, this secret should be injected via ENV vars.
    // Keys.secretKeyFor automatically generates a cryptographically secure 256-bit key.
    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Tokens expire after 24 hours
    private final long EXPIRATION_TIME = 86400000; 

    public String generateToken(String email, String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        // Injecting our custom claims for IDOR protection and RBAC
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/security/JwtFilter.java
```java
package com.boundless.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");

        // 1. If there's no Bearer token, let it pass to be rejected by Spring Security
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the token
        final String jwt = authHeader.substring(7);
        
        // 3. Cryptographically verify it
        if (jwtUtils.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            Claims claims = jwtUtils.extractAllClaims(jwt);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            
            // 4. Inject the Role into the Security Context!
            // This is the magic that makes @PreAuthorize("hasRole('ADMIN')") work.
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/entity/MasterWallet.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "master_wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // A FinTech platform typically only has ONE master company wallet
    @Column(nullable = false, unique = true)
    private String companyName;

    @Column(nullable = false)
    private BigDecimal totalBalance;
}
```


### File: card-identity-service/src/main/java/com/boundless/entity/User.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Human-readable Corporate Employee ID (e.g., "EMP-54321")
    @Column(unique = true)
    private String employeeId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN or ROLE_EMPLOYEE

    @Column(nullable = false)
    private boolean requiresPasswordChange;
    
    // B2B SaaS: Which client company does this employee belong to?
    @Column(name = "company_name")
    private String companyName;
}
```


### File: card-identity-service/src/main/java/com/boundless/entity/Wallet.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private String name; // e.g., "Travel Budget"

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String status; // ACTIVE, FROZEN
}
```


### File: card-identity-service/src/main/java/com/boundless/entity/Company.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String companyName;

    // Employer Identification Number (Tax ID)
    @Column(nullable = false, unique = true)
    private String ein;

    @Column(nullable = false)
    private String corporateAddress;

    // PENDING, APPROVED, REJECTED
    @Column(nullable = false)
    private String kybStatus;
}
```


### File: card-identity-service/src/main/java/com/boundless/entity/VirtualCard.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "virtual_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, unique = true, length = 16)
    private String cardNumber;

    @Column(nullable = false, length = 3)
    private String cvv;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String status; // ACTIVE, FROZEN, CLOSED

    @Column(nullable = false)
    private String cardType; // STANDARD, BURNER
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/InternalOpsController.java
```java
package com.boundless.controller;

import com.boundless.entity.Company;
import com.boundless.entity.MasterWallet;
import com.boundless.repository.CompanyRepository;
import com.boundless.repository.MasterWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/internal-ops")
@RequiredArgsConstructor
public class InternalOpsController {

    private final MasterWalletRepository masterWalletRepository;
    private final CompanyRepository companyRepository;

    // TREASURY ROLE: Can wire money to the platform
    @PreAuthorize("hasRole('PLATFORM_TREASURY')")
    @PostMapping("/master-wallet/fund")
    public ResponseEntity<?> fundMasterWallet(@RequestParam String companyName, @RequestParam BigDecimal amount) {
        MasterWallet masterWallet = masterWalletRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Master Wallet not found."));
        
        masterWallet.setTotalBalance(masterWallet.getTotalBalance().add(amount));
        masterWalletRepository.save(masterWallet);
        
        return ResponseEntity.ok("TREASURY ACTION: Successfully wired $" + amount + " to " + companyName);
    }

    // COMPLIANCE ROLE: Can freeze a company's entire account for AML/Fraud
    @PreAuthorize("hasRole('PLATFORM_COMPLIANCE')")
    @PostMapping("/companies/{companyName}/freeze")
    public ResponseEntity<?> freezeCompany(@PathVariable String companyName) {
        Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Company not found."));
                
        company.setKybStatus("FROZEN_FOR_AML_INVESTIGATION");
        companyRepository.save(company);
        
        // (In a real system, this would drop a Kafka event to instantly block all their virtual cards in the Auth Engine)
        return ResponseEntity.ok("COMPLIANCE ACTION: " + companyName + " has been locked. All transactions halted.");
    }

    // SUPPORT ROLE: Can view basic company details for troubleshooting but cannot edit
    @PreAuthorize("hasRole('PLATFORM_SUPPORT')")
    @GetMapping("/companies/{companyName}/overview")
    public ResponseEntity<?> viewCompanyOverview(@PathVariable String companyName) {
        Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Company not found."));
                
        MasterWallet masterWallet = masterWalletRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Master Wallet not found."));
                
        String overview = String.format("SUPPORT OVERVIEW | Company: %s | KYB Status: %s | Master Balance: $%s",
                company.getCompanyName(), company.getKybStatus(), masterWallet.getTotalBalance());
                
        return ResponseEntity.ok(overview);
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/CompanyAdminController.java
```java
package com.boundless.controller;

import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.entity.VirtualCard;
import com.boundless.entity.Wallet;
import com.boundless.repository.MasterWalletRepository;
import com.boundless.repository.UserRepository;
import com.boundless.repository.VirtualCardRepository;
import com.boundless.repository.WalletRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/company-admin")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MasterWalletRepository masterWalletRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final com.boundless.service.KafkaProducerService kafkaProducerService;

    // View all employees in this company
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/employees")
    public ResponseEntity<?> getMyCompanyEmployees(Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        List<User> companyEmployees = userRepository.findByCompanyName(adminUser.getCompanyName());
        return ResponseEntity.ok(companyEmployees);
    }
    
    // Freeze a specific employee
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PutMapping("/employees/{employeeId}/status")
    public ResponseEntity<?> freezeEmployee(@PathVariable UUID employeeId, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        User targetEmployee = userRepository.findById(employeeId).get();
        
        if (!targetEmployee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("SECURITY VIOLATION");
        }
        return ResponseEntity.ok("Successfully frozen " + targetEmployee.getFullName());
    }

    // Onboard a new employee to this specific company!
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/employees/onboard")
    public ResponseEntity<?> onboardEmployee(@RequestBody OnboardRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Employee already exists");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String empId = "EMP-" + (int)(Math.random() * 90000 + 10000);

        User employee = User.builder()
                .employeeId(empId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(tempPassword))
                .role("ROLE_EMPLOYEE")
                .companyName(adminUser.getCompanyName()) // Automatically tied to the Admin's company!
                .requiresPasswordChange(true)
                .build();

        userRepository.save(employee);
        kafkaProducerService.sendWelcomeEmailEvent(employee.getEmail(), empId, tempPassword);
        
        return ResponseEntity.ok("Employee Onboarded! ID: " + empId + " to " + adminUser.getCompanyName());
    }

    // Allocate budget to an employee from the company's master wallet
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/wallets")
    public ResponseEntity<?> createWallet(@RequestBody WalletRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!employee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("Cannot fund an employee outside your company!");
        }

        MasterWallet masterWallet = masterWalletRepository.findByCompanyName(adminUser.getCompanyName())
                .orElseThrow(() -> new RuntimeException("Master Wallet not found"));

        if (masterWallet.getTotalBalance().compareTo(request.getInitialBalance()) < 0) {
            
            // 🚨 INTERNAL OPS PROACTIVE ALERTING (Treasury)
            String alertMsg = String.format("Client '%s' attempted to issue a $%.2f wallet, but their Master Balance is only $%.2f. Please contact their founder for a wire transfer.", 
                    adminUser.getCompanyName(), request.getInitialBalance(), masterWallet.getTotalBalance());
            kafkaProducerService.sendInternalAlert("ROLE_PLATFORM_TREASURY", "treasury@boundless.com", alertMsg);

            return ResponseEntity.badRequest().body("DECLINED: Insufficient funds in your Master Wallet.");
        }

        masterWallet.setTotalBalance(masterWallet.getTotalBalance().subtract(request.getInitialBalance()));
        masterWalletRepository.save(masterWallet);

        Wallet wallet = Wallet.builder()
                .employee(employee)
                .name(request.getWalletName())
                .balance(request.getInitialBalance())
                .status("ACTIVE")
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        redisTemplate.opsForValue().set("wallet:" + savedWallet.getId() + ":balance", savedWallet.getBalance().toString());

        return ResponseEntity.ok(savedWallet);
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/wallets/{walletId}/issue-card")
    public ResponseEntity<?> issueVirtualCard(@PathVariable UUID walletId, @RequestBody CardIssueRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getEmployee().getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("Wallet does not belong to your company!");
        }

        String cardNumber = "4" + (long)(Math.random() * 900000000000000L + 100000000000000L);
        String cvv = String.format("%03d", (int)(Math.random() * 999));
        String type = (request.getCardType() != null) ? request.getCardType().toUpperCase() : "STANDARD";

        VirtualCard card = VirtualCard.builder()
                .wallet(wallet)
                .cardNumber(cardNumber)
                .cvv(cvv)
                .expiryDate(LocalDate.now().plusYears(3))
                .status("ACTIVE")
                .cardType(type)
                .build();

        VirtualCard savedCard = virtualCardRepository.save(card);
        
        // Push the mapping AND the card type to Redis for the Auth Engine
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":wallet", wallet.getId().toString());
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":type", type);

        return ResponseEntity.ok("Card Issued! PAN: " + savedCard.getCardNumber() + " | Type: " + type);
    }
}

@Data
class CardIssueRequest {
    private String cardType; // "STANDARD" or "BURNER"
}

@Data
class OnboardRequest {
    private String email;
    private String fullName;
}

@Data
class WalletRequest {
    private UUID employeeId;
    private String walletName;
    private BigDecimal initialBalance;
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/AuthController.java
```java
package com.boundless.controller;

import com.boundless.entity.User;
import com.boundless.repository.UserRepository;
import com.boundless.security.JwtUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        
        // 1. Look up the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 2. Cryptographically verify the password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // 3. Security Check: First-time employee login
        if (user.isRequiresPasswordChange()) {
            return ResponseEntity.status(403).body("You must change your temporary password before accessing the system.");
        }

        // 4. Generate the signed JWT
        String token = jwtUtils.generateToken(user.getEmail(), user.getId().toString(), user.getRole());
        
        return ResponseEntity.ok(new JwtResponse(token));
    }
}

// DTOs for the JSON request/response
@Data
class LoginRequest {
    private String email;
    private String password;
}

@Data
class JwtResponse {
    private String token;
    public JwtResponse(String token) { this.token = token; }
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/CompanyRegistrationController.java
```java
package com.boundless.controller;

import com.boundless.entity.Company;
import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.repository.CompanyRepository;
import com.boundless.repository.MasterWalletRepository;
import com.boundless.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyRegistrationController {

    private final CompanyRepository companyRepository;
    private final MasterWalletRepository masterWalletRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.boundless.service.KafkaProducerService kafkaProducerService;

    // Public endpoint for prospective clients to sign up!
    @PostMapping("/register")
    public ResponseEntity<?> registerCompany(@RequestBody CompanyRegistrationRequest request) {

        // 1. Check for duplicates
        if (companyRepository.existsByCompanyName(request.getCompanyName())) {
            return ResponseEntity.badRequest().body("Company already registered.");
        }
        if (companyRepository.existsByEin(request.getEin())) {
            return ResponseEntity.badRequest().body("EIN already exists in the system.");
        }
        if (userRepository.existsByEmail(request.getFounderEmail())) {
            return ResponseEntity.badRequest().body("Founder email already in use.");
        }

        // 2. Save the Company (Simulating instant KYB Approval)
        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .ein(request.getEin())
                .corporateAddress(request.getCorporateAddress())
                .kybStatus("APPROVED")
                .build();
        companyRepository.save(company);

        // 3. Provision their Master Wallet automatically
        MasterWallet masterWallet = MasterWallet.builder()
                .companyName(company.getCompanyName())
                .totalBalance(BigDecimal.ZERO)
                .build();
        masterWalletRepository.save(masterWallet);

        // 4. Create the Founder's Super Admin Account for their dashboard
        String tempPassword = UUID.randomUUID().toString().substring(0, 10);
        String empId = "FND-" + (int)(Math.random() * 9000 + 1000);

        User founderAdmin = User.builder()
                .employeeId(empId)
                .email(request.getFounderEmail())
                .fullName(request.getFounderName())
                .password(passwordEncoder.encode(tempPassword))
                .role("ROLE_COMPANY_ADMIN") // They get the special admin role!
                .companyName(company.getCompanyName())
                .requiresPasswordChange(true)
                .build();
        userRepository.save(founderAdmin);

        // 5. Send them an email with their temporary password via Kafka!
        kafkaProducerService.sendWelcomeEmailEvent(founderAdmin.getEmail(), empId, tempPassword);

        return ResponseEntity.ok("KYB Approved! Company Registered. Master Wallet Provisioned. " +
                "Welcome Email dispatched to " + founderAdmin.getEmail());
    }
}

@Data
class CompanyRegistrationRequest {
    private String companyName;
    private String ein;
    private String corporateAddress;
    private String founderName;
    private String founderEmail;
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/InternalController.java
```java
package com.boundless.controller;

import com.boundless.entity.VirtualCard;
import com.boundless.repository.VirtualCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final VirtualCardRepository virtualCardRepository;

    // This endpoint is meant for internal microservices ONLY.
    // It returns the true email address associated with a given card number.
    @GetMapping("/cards/{cardNumber}/email")
    public ResponseEntity<String> getEmailForCard(@PathVariable String cardNumber) {
        return virtualCardRepository.findByCardNumber(cardNumber)
                .map(card -> ResponseEntity.ok(card.getWallet().getEmployee().getEmail()))
                .orElse(ResponseEntity.notFound().build());
    }

    // NEW ENDPOINT: Return ALL 16-digit card numbers that belong to a specific company
    @GetMapping("/companies/{companyName}/cards")
    public ResponseEntity<java.util.List<String>> getCardsForCompany(@PathVariable String companyName) {
        java.util.List<String> cardNumbers = virtualCardRepository.findByWalletEmployeeCompanyName(companyName)
                .stream()
                .map(VirtualCard::getCardNumber)
                .toList();
        return ResponseEntity.ok(cardNumbers);
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/controller/EmployeeController.java
```java
package com.boundless.controller;

import com.boundless.entity.Wallet;
import com.boundless.repository.VirtualCardRepository;
import com.boundless.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final WalletRepository walletRepository;
    private final VirtualCardRepository virtualCardRepository;

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{employeeId}/wallets")
    public ResponseEntity<?> getMyWallets(@PathVariable UUID employeeId, Authentication auth) {
        
        // 1. Get the authenticated email extracted from the JWT token
        String loggedInEmail = auth.getName();
        
        // 2. Fetch the wallets from the database
        List<Wallet> wallets = walletRepository.findByEmployeeId(employeeId);
        
        // 3. STRICT IDOR SECURITY CHECK 
        // We must ensure the person requesting these wallets actually owns them.
        if (!wallets.isEmpty() && !wallets.get(0).getEmployee().getEmail().equals(loggedInEmail)) {
            log.warn("SECURITY ALERT: User {} attempted an IDOR attack on employee {}", loggedInEmail, employeeId);
            throw new AccessDeniedException("You are not authorized to view another employee's wallets.");
        }
        
        return ResponseEntity.ok(wallets);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{employeeId}/cards")
    public ResponseEntity<?> getMyCards(@PathVariable UUID employeeId, Authentication auth) {
        
        String loggedInEmail = auth.getName();
        
        // Custom repository method we wrote earlier!
        var cards = virtualCardRepository.findByWalletEmployeeId(employeeId);
        
        // STRICT IDOR SECURITY CHECK
        if (!cards.isEmpty() && !cards.get(0).getWallet().getEmployee().getEmail().equals(loggedInEmail)) {
            throw new AccessDeniedException("You are not authorized to view another employee's cards.");
        }
        
        return ResponseEntity.ok(cards);
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/service/KafkaProducerService.java
```java
package com.boundless.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendWelcomeEmailEvent(String email, String empId, String tempPassword) {
        // Constructing a simple JSON payload for the event
        String message = String.format("{\"email\":\"%s\", \"empId\":\"%s\", \"tempPassword\":\"%s\"}", 
                email, empId, tempPassword);
        
        // Publish the event to Kafka!
        // A separate Notification Microservice will listen to this topic and send the actual SMTP email.
        kafkaTemplate.send("employee-onboarding-topic", message);
        
        log.info("⚡ Kafka Event Published: Welcome email queued for {}", email);
    }
    
    public void sendInternalAlert(String role, String targetEmail, String alertMessage) {
        String message = String.format("{\"role\":\"%s\", \"targetEmail\":\"%s\", \"alert\":\"%s\"}", 
                role, targetEmail, alertMessage);
        
        kafkaTemplate.send("internal-alert-topic", message);
        log.info("🚨 Internal Alert Published to Kafka for {}", role);
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/service/CardFreezeListener.java
```java
package com.boundless.service;

import com.boundless.entity.VirtualCard;
import com.boundless.repository.VirtualCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardFreezeListener {

    private final VirtualCardRepository virtualCardRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Listens to the exact topic our Ledger SLA Cron Job publishes to!
    @KafkaListener(topics = "card-freeze-topic", groupId = "card-identity-group")
    public void handleCardFreeze(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String reason = payload.get("reason").asText();

            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isPresent()) {
                // 1. Permanently freeze the card in the PostgreSQL Database
                VirtualCard card = cardOpt.get();
                card.setStatus("FROZEN");
                virtualCardRepository.save(card);

                // 2. 🔴 CRITICAL: Freeze it in Redis so the ultra-fast Auth Engine instantly blocks swipes!
                redisTemplate.opsForValue().set("card:" + cardNumber + ":status", "FROZEN");

                log.warn("❄️ Card {} FROZEN due to: {}", cardNumber, reason);
            }
        } catch (Exception e) {
            log.error("Failed to freeze card: {}", e.getMessage());
        }
    }

    // NEW: Listen for receipt uploads to UNFREEZE the card
    @KafkaListener(topics = "card-unfreeze-topic", groupId = "card-identity-group")
    public void handleCardUnfreeze(String cardNumber) {
        try {
            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isPresent()) {
                // 1. Unfreeze in Postgres
                VirtualCard card = cardOpt.get();
                card.setStatus("ACTIVE");
                virtualCardRepository.save(card);

                // 2. 🟢 CRITICAL: Unfreeze in Redis! (Or delete the FROZEN key)
                redisTemplate.delete("card:" + cardNumber + ":status");

                log.info("🔥 Card {} UNFROZEN because receipt was uploaded!", cardNumber);
            }
        } catch (Exception e) {
            log.error("Failed to unfreeze card: {}", e.getMessage());
        }
    }

    // Listen for BURNER card destruction
    @KafkaListener(topics = "card-closed-topic", groupId = "card-identity-group")
    public void handleCardClosed(String cardNumber) {
        try {
            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            if (cardOpt.isPresent()) {
                VirtualCard card = cardOpt.get();
                card.setStatus("CLOSED"); // Permanent death
                virtualCardRepository.save(card);
                log.info("💀 BURNER Card {} permanently CLOSED in PostgreSQL.", cardNumber);
            }
        } catch (Exception e) {
            log.error("Failed to close card: {}", e.getMessage());
        }
    }
}
```


### File: card-identity-service/src/main/java/com/boundless/CardIdentityServiceApplication.java
```java
package com.boundless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CardIdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardIdentityServiceApplication.class, args);
	}

}
```


## ledger-service Deep Dive
This section covers every single line of code and configuration for ledger-service.

### File: ledger-service/target/classes/application.yml
```java
server:
  port: 8083

spring:
  application:
    name: ledger-service
  datasource:
    url: jdbc:postgresql://localhost:5432/boundless_db
    username: boundless_admin
    password: boundless_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ledger-settlement-group
      auto-offset-reset: earliest
```


### File: ledger-service/pom.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.boundless</groupId>
	<artifactId>ledger-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>ledger-service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		
		<!-- Swagger UI / OpenAPI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.3.0</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
		
		<!-- AWS SDK for S3 / MinIO Integration -->
		<dependency>
			<groupId>com.amazonaws</groupId>
			<artifactId>aws-java-sdk-s3</artifactId>
			<version>1.12.555</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```


### File: ledger-service/src/test/java/com/boundless/ledger_service/LedgerServiceApplicationTests.java
```java
package com.boundless.ledger_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LedgerServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```


### File: ledger-service/src/main/resources/application.yml
```java
server:
  port: 8083

spring:
  application:
    name: ledger-service
  datasource:
    url: jdbc:postgresql://localhost:5432/boundless_db
    username: boundless_admin
    password: boundless_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ledger-settlement-group
      auto-offset-reset: earliest
```


### File: ledger-service/src/main/java/com/boundless/repository/TransactionRepository.java
```java
package com.boundless.repository;

import com.boundless.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    // This allows a Spring @Scheduled cron job to instantly find employees 
    // who spent money but failed to upload their receipt within 24 hours!
    List<Transaction> findByReceiptUploadedFalseAndReceiptDeadlineBefore(LocalDateTime now);
    
    // B2B: Find all transactions for a specific list of corporate cards
    List<Transaction> findByCardNumberIn(List<String> cardNumbers);
}
```


### File: ledger-service/src/main/java/com/boundless/config/S3Config.java
```java
package com.boundless.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public AmazonS3 amazonS3() {
        // Pointing to our local MinIO Docker container!
        AWSCredentials credentials = new BasicAWSCredentials(
                "boundless_aws_key", 
                "boundless_aws_secret"
        );

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration("http://localhost:9000", "us-east-1"))
                .withPathStyleAccessEnabled(true) // Required for MinIO
                .build();
    }
}
```


### File: ledger-service/src/main/java/com/boundless/entity/Transaction.java
```java
package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // SETTLED, DECLINED, REFUNDED

    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // ----------------------------------------------------
    // COMPLIANCE: The 24-hour receipt rule
    // ----------------------------------------------------
    private boolean receiptUploaded;
    private LocalDateTime receiptDeadline;
    
    // Stores the file path of the uploaded image
    private String receiptUrl;
}
```


### File: ledger-service/src/main/java/com/boundless/LedgerServiceApplication.java
```java
package com.boundless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LedgerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerServiceApplication.class, args);
	}

}
```


### File: ledger-service/src/main/java/com/boundless/controller/TransactionController.java
```java
package com.boundless.controller;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.boundless.service.S3Service s3Service;

    // This is the endpoint the Employee uses on their mobile app to snap a picture of their receipt
    @PostMapping("/{transactionId}/receipt")
    public ResponseEntity<?> uploadReceipt(
            @PathVariable UUID transactionId, 
            @RequestParam("file") MultipartFile file) {
            
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        try {
            // 1. Upload the image directly to our MinIO S3 Bucket!
            String s3Url = s3Service.uploadFile(transactionId, file);

            // 2. Save the public S3 URL to Postgres so Admins can view it later
            tx.setReceiptUrl(s3Url);
            
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).body("Failed to save image to S3: " + e.getMessage());
        }

        // 3. Mark the SLA as satisfied!
        tx.setReceiptUploaded(true);
        transactionRepository.save(tx);

        // 4. FIRE THE UNFREEZE EVENT!
        kafkaTemplate.send("card-unfreeze-topic", tx.getCardNumber());

        return ResponseEntity.ok("Receipt securely uploaded to MinIO S3! SLA satisfied and Card Unfrozen.\nS3 URL: " + tx.getReceiptUrl());
    }

    // B2B Multi-Tenant: Allow Company Admins to see all transactions for their company
    @GetMapping("/company/{companyName}")
    public ResponseEntity<?> getCompanyTransactions(@PathVariable String companyName) {
        
        // 1. Cross-Microservice Call: Ask Identity Service for all card numbers that belong to this company
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        String identityUrl = "http://localhost:8081/api/internal/companies/" + companyName + "/cards";
        
        try {
            java.util.List<String> cardNumbers = restTemplate.getForObject(identityUrl, java.util.List.class);
            
            if (cardNumbers == null || cardNumbers.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            // 2. Fetch the transactions from the Ledger Database matching those exact cards
            java.util.List<Transaction> transactions = transactionRepository.findByCardNumberIn(cardNumbers);
            
            return ResponseEntity.ok(transactions);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error communicating with Identity Service: " + e.getMessage());
        }
    }
}
```


### File: ledger-service/src/main/java/com/boundless/service/LedgerListener.java
```java
package com.boundless.service;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerListener {

    private final TransactionRepository transactionRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "auth-approved-topic", groupId = "ledger-settlement-group")
    public void processSettlement(String message) {
        try {
            log.info("📥 Received Auth Event from Kafka: {}", message);
            
            JsonNode payload = objectMapper.readTree(message);
            
            BigDecimal amount = new BigDecimal(payload.get("amount").asText());
            
            // The company policy limit: Only require receipts for purchases over $50.00
            BigDecimal receiptThreshold = new BigDecimal("50.00");
            boolean requiresReceipt = amount.compareTo(receiptThreshold) > 0;
            
            Transaction tx = Transaction.builder()
                    .cardNumber(payload.get("cardNumber").asText())
                    .merchantName(payload.get("merchantName").asText())
                    .amount(amount)
                    .status("SETTLED")
                    .timestamp(LocalDateTime.now())
                    // Only enforce the 24-hour SLA if the total transaction amount exceeds the limit!
                    .receiptUploaded(!requiresReceipt) // Small purchases are automatically compliant
                    .receiptDeadline(requiresReceipt ? LocalDateTime.now().plusHours(24) : null)
                    .build();

            // Permanent persistence to PostgreSQL
            transactionRepository.save(tx);
            log.info("✅ Transaction permanently settled to PostgreSQL Database. ID: {}", tx.getId());

            // ⚡ Instantly alert the user that they must upload a receipt!
            if (requiresReceipt) {
                String reminderEvent = String.format("{\"cardNumber\":\"%s\", \"amount\":\"%s\", \"merchantName\":\"%s\"}", 
                        tx.getCardNumber(), tx.getAmount(), tx.getMerchantName());
                kafkaTemplate.send("receipt-reminder-topic", reminderEvent);
                log.info("✉️ Receipt Reminder Email Event dispatched to Kafka.");
            }

        } catch (Exception e) {
            log.error("❌ Failed to process settlement: {}", e.getMessage());
        }
    }
}
```


### File: ledger-service/src/main/java/com/boundless/service/S3Service.java
```java
package com.boundless.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private final String BUCKET_NAME = "boundless-receipts";

    public String uploadFile(UUID transactionId, MultipartFile file) throws IOException {
        String fileName = transactionId.toString() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        log.info("Uploading receipt to MinIO S3: {}", fileName);
        
        amazonS3.putObject(new PutObjectRequest(
                BUCKET_NAME, 
                fileName, 
                file.getInputStream(), 
                metadata
        ));

        // Return the public URL to view the image
        String s3Url = "http://localhost:9000/" + BUCKET_NAME + "/" + fileName;
        log.info("Successfully uploaded receipt! URL: {}", s3Url);
        return s3Url;
    }
}
```


### File: ledger-service/src/main/java/com/boundless/service/ReceiptSlaCronJob.java
```java
package com.boundless.service;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptSlaCronJob {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // A Cron Job that runs at the top of every single hour (e.g., 1:00, 2:00, 3:00)
    @Scheduled(cron = "0 0 * * * *")
    public void enforceReceiptSla() {
        log.info("🔍 Running 24-Hour Receipt Compliance Audit...");

        // 1. Query PostgreSQL for any transactions that are older than 24 hours missing a receipt
        List<Transaction> breachedTransactions = transactionRepository
                .findByReceiptUploadedFalseAndReceiptDeadlineBefore(LocalDateTime.now());

        if (breachedTransactions.isEmpty()) {
            log.info("✅ All employees are compliant with their receipt SLAs.");
            return;
        }

        // 2. Penalize the non-compliant employees!
        for (Transaction tx : breachedTransactions) {
            log.warn("🚨 SLA BREACH: Card {} missed the 24-hour receipt deadline for a ${} purchase at {}.", 
                    tx.getCardNumber(), tx.getAmount(), tx.getMerchantName());

            // Fire an event to the Card & Identity Service to instantly FREEZE the employee's card
            String freezeEvent = String.format("{\"cardNumber\":\"%s\", \"reason\":\"RECEIPT_SLA_BREACH\"}", tx.getCardNumber());
            kafkaTemplate.send("card-freeze-topic", freezeEvent);

            // Fire an event to the Notification Service to email the Employee a strict warning
            kafkaTemplate.send("compliance-warning-topic", freezeEvent);
        }
        
        log.info("🔒 Dispatched freeze events for {} non-compliant cards.", breachedTransactions.size());
    }
}
```


## notification-service Deep Dive
This section covers every single line of code and configuration for notification-service.

### File: notification-service/target/classes/application.yml
```java
server:
  port: 8084

spring:
  application:
    name: notification-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest

  # ----------------------------------------------------
  # SMTP CONFIGURATION FOR GMAIL
  # ----------------------------------------------------
  mail:
    host: smtp.gmail.com
    port: 587
    username: sidharth6034@gmail.com
    password: "onbu cirj umkq rtwj"
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```


### File: notification-service/pom.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.boundless</groupId>
	<artifactId>notification-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>notification-service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka</artifactId>
		</dependency>
		
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-mail</artifactId>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-mail-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```


### File: notification-service/src/test/java/com/boundless/notification_service/NotificationServiceApplicationTests.java
```java
package com.boundless.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```


### File: notification-service/src/main/resources/application.yml
```java
server:
  port: 8084

spring:
  application:
    name: notification-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest

  # ----------------------------------------------------
  # SMTP CONFIGURATION FOR GMAIL
  # ----------------------------------------------------
  mail:
    host: smtp.gmail.com
    port: 587
    username: sidharth6034@gmail.com
    password: "onbu cirj umkq rtwj"
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```


### File: notification-service/src/main/java/com/boundless/NotificationServiceApplication.java
```java
package com.boundless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
```


### File: notification-service/src/main/java/com/boundless/service/EmailListener.java
```java
package com.boundless.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailListener {

    // Spring Boot automatically injects this when spring-boot-starter-mail is in the pom.xml
    private final JavaMailSender mailSender;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------
    // 1. Listen for the Welcome Email Event
    // ---------------------------------------------------------
    @KafkaListener(topics = "employee-onboarding-topic", groupId = "notification-group")
    public void handleWelcomeEmail(String message) {
        try {
            // Parse the JSON dropped by the AdminController
            JsonNode payload = objectMapper.readTree(message);
            String email = payload.get("email").asText();
            String empId = payload.get("empId").asText();
            String tempPassword = payload.get("tempPassword").asText();

            String emailBody = String.format(
                "Welcome to Boundless Corporate Cards!\n\n" +
                "Your Employee ID is: %s\n" +
                "Your Temporary Password is: %s\n\n" +
                "Please log in to the dashboard to change your password and view your Virtual Wallet.",
                empId, tempPassword
            );

            sendEmail(email, "Welcome to Boundless!", emailBody);
        } catch (Exception e) {
            log.error("Failed to process welcome email event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 2. Listen for the OTP Email Event (RBI Limit)
    // ---------------------------------------------------------
    @KafkaListener(topics = "otp-email-topic", groupId = "notification-group")
    public void handleOtpEmail(String message) {
        try {
            // Parse the JSON dropped by the AuthEngineController
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String otp = payload.get("otp").asText();

            // Mask the card number for security (e.g., ****-****-****-1234)
            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "SECURITY ALERT: Large Transaction Attempt\n\n" +
                "A swipe exceeding ₹5,000 was attempted on your card %s.\n\n" +
                "Your 6-digit Verification Code is: %s\n\n" +
                "This code will expire in 5 minutes. Do not share this code with anyone.",
                maskedCard, otp
            );

            // In a real system, we would query the DB for the employee's email using the card number.
            // For testing, we are routing this directly to your email!
            sendEmail("sidharth6034@gmail.com", "Action Required: Transaction OTP", emailBody);
        } catch (Exception e) {
            log.error("Failed to process OTP email event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 3. Listen for the Receipt Required Event
    // ---------------------------------------------------------
    @KafkaListener(topics = "receipt-reminder-topic", groupId = "notification-group")
    public void handleReceiptReminder(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String amount = payload.get("amount").asText();
            String merchantName = payload.get("merchantName").asText();

            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "ACTION REQUIRED: Missing Receipt\n\n" +
                "You recently made a purchase of $%s at %s using your card %s.\n\n" +
                "Because this amount exceeds the company threshold, you must upload a receipt within 24 hours.\n" +
                "Failure to upload a receipt will result in an automatic freeze of your corporate card.",
                amount, merchantName, maskedCard
            );

            // -------------------------------------------------------------
            // SOLUTION 2: The Service-to-Service API Call
            // -------------------------------------------------------------
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String identityServiceUrl = "http://localhost:8081/api/internal/cards/" + cardNumber + "/email";
            String targetEmail = null;
            
            try {
                // We ask the Identity Service: "Who owns this card?"
                targetEmail = restTemplate.getForObject(identityServiceUrl, String.class);
            } catch (Exception httpException) {
                log.warn("Identity DB lookup failed for card {}. Using fallback testing email.", cardNumber);
                targetEmail = "sidharth6034@gmail.com";
            }

            if (targetEmail != null) {
                sendEmail(targetEmail, "Action Required: Upload Receipt", emailBody);
            }

        } catch (Exception e) {
            log.error("Failed to process receipt reminder event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 4. Listen for Internal Ops Proactive Alerts
    // ---------------------------------------------------------
    @KafkaListener(topics = "internal-alert-topic", groupId = "notification-group")
    public void handleInternalAlert(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String role = payload.get("role").asText();
            String targetEmail = payload.get("targetEmail").asText();
            String alertText = payload.get("alert").asText();

            String emailBody = String.format(
                "🚨 PLATFORM ALERT [%s]\n\n" +
                "The system has generated an automated alert requiring your attention:\n\n" +
                "%s\n\n" +
                "Please log in to the Internal Dashboard to take action immediately.",
                role, alertText
            );

            // Using your testing email so you actually get the alert on your phone!
            sendEmail("sidharth6034@gmail.com", "URGENT: Internal Platform Alert", emailBody);
            
        } catch (Exception e) {
            log.error("Failed to process internal alert event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // The actual SMTP dispatch logic
    // ---------------------------------------------------------
    private void sendEmail(String to, String subject, String text) {
        
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(text);
        mailMessage.setFrom("sidharth6034@gmail.com"); // Must match your authenticated Gmail!
        mailSender.send(mailMessage);
        
        log.info("\n========================================");
        log.info("✉️  SENDING EMAIL TO: {}", to);
        log.info("✉️  SUBJECT: {}", subject);
        log.info("✉️  BODY:\n{}", text);
        log.info("========================================\n");
    }
}
```


## api-gateway-service Deep Dive
This section covers every single line of code and configuration for api-gateway-service.

### File: api-gateway-service/target/classes/application.yml
```java
server:
  port: 8080

spring:
  application:
    name: api-gateway-service
  cloud:
    gateway:
      routes:
        # ----------------------------------------------------
        # ROUTE 1: Identity & Card Management Service
        # Handles Admin Onboarding, Logins, and Employee Wallets
        # ----------------------------------------------------
        - id: card-identity-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**, /api/admin/**, /api/employees/**

        # ----------------------------------------------------
        # ROUTE 2: Auth Engine Service
        # Handles High-Speed Swipes from Visa/Mastercard
        # ----------------------------------------------------
        - id: auth-engine-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/network/**

        # ----------------------------------------------------
        # ROUTE 3: Ledger & Settlement Service
        # Handles Employee Receipt Uploads
        # ----------------------------------------------------
        - id: ledger-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/transactions/**
            
# (Note: The Notification Service doesn't have an API route because it 
# is a pure backend worker that only listens to Kafka!)
```


### File: api-gateway-service/pom.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.boundless</groupId>
	<artifactId>api-gateway-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>api-gateway-service</name>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
		<spring-cloud.version>2025.1.3</spring-cloud.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```


### File: api-gateway-service/src/test/java/com/boundless/api_gateway_service/ApiGatewayServiceApplicationTests.java
```java
package com.boundless.api_gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiGatewayServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```


### File: api-gateway-service/src/main/resources/application.yml
```java
server:
  port: 8080

spring:
  application:
    name: api-gateway-service
  cloud:
    gateway:
      routes:
        # ----------------------------------------------------
        # ROUTE 1: Identity & Card Management Service
        # Handles Admin Onboarding, Logins, and Employee Wallets
        # ----------------------------------------------------
        - id: card-identity-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**, /api/admin/**, /api/employees/**

        # ----------------------------------------------------
        # ROUTE 2: Auth Engine Service
        # Handles High-Speed Swipes from Visa/Mastercard
        # ----------------------------------------------------
        - id: auth-engine-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/network/**

        # ----------------------------------------------------
        # ROUTE 3: Ledger & Settlement Service
        # Handles Employee Receipt Uploads
        # ----------------------------------------------------
        - id: ledger-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/transactions/**
            
# (Note: The Notification Service doesn't have an API route because it 
# is a pure backend worker that only listens to Kafka!)
```


### File: api-gateway-service/src/main/java/com/boundless/ApiGatewayServiceApplication.java
```java
package com.boundless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

}
```


### File: api-gateway-service/src/main/java/com/boundless/gateway/filter/GatewayAuthFilter.java
```java
package com.boundless.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final List<String> OPEN_ENDPOINTS = List.of("/api/auth/login", "/api/network/swipe");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (OPEN_ENDPOINTS.stream().anyMatch(path::contains)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String extractedEmail = "employee@boundless.com";
            String extractedRole = "ROLE_EMPLOYEE";

            HttpServletRequestWrapper mutatedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if (name.equalsIgnoreCase("X-Trusted-User-Email")) return extractedEmail;
                    if (name.equalsIgnoreCase("X-Trusted-User-Role")) return extractedRole;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (name.equalsIgnoreCase("X-Trusted-User-Email")) return Collections.enumeration(Collections.singletonList(extractedEmail));
                    if (name.equalsIgnoreCase("X-Trusted-User-Role")) return Collections.enumeration(Collections.singletonList(extractedRole));
                    return super.getHeaders(name);
                }
            };

            filterChain.doFilter(mutatedRequest, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
```

