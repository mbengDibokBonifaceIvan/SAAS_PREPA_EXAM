package com.ivan.notification_service.infrastructure.adapter.out.persistence;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        JpaNotificationRepositoryAdapter.class,
        PersistenceMapper.class
})
@ActiveProfiles("test")
class JpaNotificationRepositoryAdapterTest {

    @Autowired
    private JpaNotificationRepositoryAdapter adapter;

    @Autowired
    private NotificationJpaRepository jpaRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Save & FindById : devrait persister et récupérer la notification via le domaine")
    void shouldSaveAndFindNotification() {
        // GIVEN
        Notification notification = createSampleNotification("Test Title");

        // WHEN
        adapter.save(notification);
        Optional<Notification> found = adapter.findById(notification.getId());

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Title");
        assertThat(found.get().getRecipient()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("FindByUserId : devrait retourner une page de notifications pour un utilisateur")
    void shouldFindNotificationsByUserIdPaged() {
        // GIVEN
        adapter.save(createSampleNotification("Notification 1"));
        adapter.save(createSampleNotification("Notification 2"));
        
        // Notification pour un autre utilisateur
        Notification otherNotification = Notification.builder()
                .userId(UUID.randomUUID())
                .recipient("other@test.com")
                .title("Other")
                .message("Message")
                .type(NotificationType.EMAIL)
                .build();
        adapter.save(otherNotification);

        // WHEN
        Page<Notification> result = adapter.findByUserId(userId, PageRequest.of(0, 10));

        // THEN
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(n -> n.getUserId().equals(userId));
    }

    @Test
    @DisplayName("Update : devrait mettre à jour le statut d'une notification existante")
    void shouldUpdateStatus() {
        // GIVEN
        Notification notification = createSampleNotification("Initial Title");
        adapter.save(notification);
        
        // WHEN : On simule le passage à SENT dans le domaine
        notification.markAsSent(); 
        adapter.save(notification);

        // THEN
        NotificationEntity entity = jpaRepository.findById(notification.getId()).orElseThrow();
        assertThat(entity.getStatus().name()).isEqualTo("SENT");
        assertThat(jpaRepository.count()).isEqualTo(1); // Pas de doublon créé
    }

    private Notification createSampleNotification(String title) {
        return Notification.builder()
                .userId(userId)
                .recipient("user@test.com")
                .title(title)
                .message("Contenu du message")
                .type(NotificationType.EMAIL)
                .build();
    }
}