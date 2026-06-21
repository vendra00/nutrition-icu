package t1tanic.nutritionicu.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper for UI components (which aren't Spring beans) to check the current user's role. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
