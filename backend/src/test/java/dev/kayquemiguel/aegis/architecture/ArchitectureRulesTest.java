package dev.kayquemiguel.aegis.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
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


    @Test
    void detectionCoreMustRemainIndependentOfInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage(
                        "dev.kayquemiguel.aegis.detection"
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "..detection.api..",
                        "..detection.persistence..",
                        "..detection.pipeline..",
                        "..detection.config..",
                        "..ingestion..",
                        "..security..",
                        "..observability.."
                )
                .check(classes);
    }


    @Test
    void alertQueryApiMustNotDependOnPersistence() {
        noClasses()
                .that()
                .haveSimpleName("DetectionAlertQueryService")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..detection.persistence.."
                )
                .check(classes);
    }

}
