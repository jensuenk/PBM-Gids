package com.ineos.oxide.base.security.services;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ineos.oxide.base.security.model.entities.User;

public class SecurityUtils {

	public static Optional<User> getCurrentUser() {
		if (isUserLoggedIn() && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User) {
			return Optional.of((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
		}
		// Anonymous or no authentication.
		return Optional.empty();

	}

	/**
	 * Checks if the user is logged in.
	 *
	 * @return true if the user is logged in. False otherwise.
	 */
	public static boolean isUserLoggedIn() {
		return isUserLoggedIn(SecurityContextHolder.getContext().getAuthentication());
	}

	private static boolean isUserLoggedIn(Authentication authentication) {
		return authentication != null
				&& !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
	}

	public static boolean isCurrentUser(User user) {
		return getCurrentUser().map(currentUser -> currentUser.getUsername().equals(user.getUsername())).orElse(false);
	}

	public static boolean isUserInRole(String string) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(string));
	}

}
