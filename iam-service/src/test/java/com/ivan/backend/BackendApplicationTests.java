package com.ivan.backend;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    @org.junit.jupiter.api.DisplayName("Contexte : l'application doit démarrer sans erreur")
    void contextLoads() {
        // Vérifie simplement que le contexte Spring s'initialise
    }

  @Test
@org.junit.jupiter.api.DisplayName("Main : devrait appeler SpringApplication.run")
void mainMethodTest() {
    try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
        // On simule l'exécution du main
        BackendApplication.main(new String[]{});

        // On vérifie que SpringApplication.run a bien été appelé avec notre classe
        springApplicationMock.verify(() -> 
            SpringApplication.run(BackendApplication.class, new String[]{})
        );
    }
}
}
