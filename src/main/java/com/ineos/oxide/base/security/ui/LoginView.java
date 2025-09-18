package com.ineos.oxide.base.security.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import com.ineos.oxide.base.security.services.SecurityUtils;
import com.ineos.oxide.base.services.HasLogger;
import com.ineos.oxide.base.services.HasResources;
import com.ineos.oxide.pbmgids.ui.RootView;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.PostConstruct;

@Route
@PropertySource("classpath:application.yaml")
public class LoginView extends LoginOverlay
		implements AfterNavigationObserver, BeforeEnterObserver, HasDynamicTitle, HasLogger, HasResources {
	@Value("${spring.application.name}")
	private String title;

	public LoginView() {
	}

	@PostConstruct
	public void init() {
		LoginI18n i18n = LoginI18n.createDefault();
		i18n.setHeader(new LoginI18n.Header());
		i18n.getHeader().setTitle(title);
		i18n.getHeader().setDescription(getProperty("login.description"));
		i18n.setAdditionalInformation(getProperty("login.additional_information"));
		i18n.setForm(new LoginI18n.Form());
		i18n.getForm().setSubmit(getProperty("login.form.submit"));
		i18n.getForm().setTitle(getProperty("login.form.title"));
		i18n.getForm().setUsername(getProperty("login.form.username"));
		i18n.getForm().setPassword(getProperty("login.form.password"));
		setI18n(i18n);
		setForgotPasswordButtonVisible(false);
		setAction("login");
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		setError(
				event.getLocation().getQueryParameters().getParameters().containsKey(
						"error"));
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (SecurityUtils.isUserLoggedIn()) {
			event.forwardTo(RootView.class);
		} else {
			setOpened(true);
		}
	}

	@Override
	public String getPageTitle() {
		return title;
	}

}
