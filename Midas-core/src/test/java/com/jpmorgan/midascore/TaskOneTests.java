package com.jpmorgan.midascore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

/**
 * TaskOneTests — Module 1 (Project Foundation) Verification
 *
 * Verifies:
 *   T1-01: Application context loads without BeanCreationException
 *   T1-02: All Maven dependencies resolve correctly (mvn clean install)
 *   T1-03: application.yml is valid and loaded (no BindException)
 *   T1-04: BEGIN/END console markers appear
 *
 * Run: mvn test -Dtest=TaskOneTests
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"trader-updates"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskOneTests {

    @Test
    void contextLoads() {
        System.out.println("========== BEGIN TASK 1 ==========");
        System.out.println("[T1-01] Spring application context loaded successfully.");
        System.out.println("[T1-02] All Maven dependencies resolved — build succeeded.");
        System.out.println("[T1-03] application.yml is valid — no BindException.");
        System.out.println("[T1-04] BEGIN/END markers are visible in console.");
        System.out.println("========== END TASK 1 ==========");
    }
}
