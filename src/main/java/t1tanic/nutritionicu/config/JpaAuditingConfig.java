package t1tanic.nutritionicu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Turns on Spring Data JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate} are populated. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
