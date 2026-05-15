package com.SmartBank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationListener implements ApplicationListener<AbstractAuthenticationEvent> {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AuthenticationSuccessEvent) {
            String username = event.getAuthentication().getName();
            loginAttemptService.loginSucceeded(username);
        } else if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            String username = event.getAuthentication().getName();
            loginAttemptService.loginFailed(username);
        }
    }
}
