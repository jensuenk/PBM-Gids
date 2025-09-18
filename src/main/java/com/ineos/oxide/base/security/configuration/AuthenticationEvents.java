package com.ineos.oxide.base.security.configuration;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.ineos.oxide.base.services.HasLogger;

@Component
public class AuthenticationEvents implements HasLogger {
  @EventListener
  public void onSuccess(AuthenticationSuccessEvent success) {
    getLogger().info("User {} has logged in", success.getAuthentication().getName());
  }

  @EventListener
  public void onFailure(AbstractAuthenticationFailureEvent failures) {
    getLogger().info("User {} has failed to log in", failures.getAuthentication().getName());
  }

}