package t1tanic.nutritionicu.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.User;
import t1tanic.nutritionicu.model.enums.Role;
import t1tanic.nutritionicu.repo.UserRepository;

/**
 * Seeds demo login accounts on first startup (when no users exist): an admin and a doctor. Passwords are
 * BCrypt-hashed. Change/disable for real deployments.
 */
@Slf4j
@Component
@Order(1)
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("admin", passwordEncoder.encode("admin"), "Administrator", Role.ADMIN));
        userRepository.save(new User("doctor", passwordEncoder.encode("doctor"), "Dr. Demo", Role.DOCTOR));
        log.info("Seeded demo accounts: admin (ADMIN), doctor (DOCTOR)");
    }
}
