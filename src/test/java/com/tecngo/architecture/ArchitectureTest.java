package com.tecngo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tecngo");

    @Test
    void restControllersUseControllerSuffix() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(productionClasses);
    }

    @Test
    void repositoriesAreInterfaces() {
        classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces()
                .check(productionClasses);
    }

    @Test
    void repositoriesDoNotDependOnWebOrServiceLayers() {
        noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controller..", "..service..")
                .check(productionClasses);
    }

    @Test
    void servicesDoNotDependOnControllers() {
        noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(productionClasses);
    }

    @Test
    void entitiesDoNotDependOnWebOrServiceLayers() {
        noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controller..", "..service..", "..dto..")
                .check(productionClasses);
    }
}
