package com.template.vivid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'application Spring Boot.
 *
 * @SpringBootApplication est un raccourci qui combine :
 *   - @Configuration      : déclare cette classe comme source de beans Spring
 *   - @EnableAutoConfiguration : active l'auto-configuration Spring Boot
 *     (détecte PostgreSQL, Redis, Security… et configure les beans par défaut)
 *   - @ComponentScan      : scanne tous les sous-packages pour trouver les
 *     @Component, @Service, @Repository, @Controller, etc.
 *
 * @EnableScheduling : active les méthodes @Scheduled (voir
 *   NotificationServiceImpl#retryFailedChannelNotifications et
 *   #escalateExhaustedNotificationFailures — reprise et alerte des envois
 *   EMAIL/SMS en échec).
 */
@SpringBootApplication
@EnableScheduling
public class VividelaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VividelaApplication.class, args);
    }
}
