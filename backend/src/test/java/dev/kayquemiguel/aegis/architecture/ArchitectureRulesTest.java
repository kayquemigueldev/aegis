package dev.kayquemiguel.aegis.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .importPackages("dev.kayquemiguel.aegis");
    }

    @Test
    void domainMustNotDependOnFrameworksOrOtherModules() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "..ingestion..",
                        "..detection..",
                        "..security.."
                )
                .check(classes);
    }

    @Test
    void ingestionMustNotDependOnSecurity() {
        noClasses()
                .that()
                .resideInAPackage("..ingestion..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..security..")
                .check(classes);
    }

    @Test
    void ingestionMustNotPersistDetectionAlerts() {
        noClasses()
                .that()
                .resideInAPackage("..ingestion..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..detection.persistence..")
                .check(classes);
    }

    @Test
    void detectionEngineMustNotDependOnInfrastructure() {
        noClasses()
                .that()
                .haveSimpleName("DetectionEngine")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "..detection.api..",
                        "..detection.persistence..",
                        "..detection.pipeline..",
                        "..security..",
                        "..ingestion.."
                )
                .check(classes);
    }

}
