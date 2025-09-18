package com.ineos.oxide;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.ineos.oxide.base.security.model.entities.DBUser;
import com.ineos.oxide.base.security.services.ServiceDBUser;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme(value = "oxide")
@PWA(name = "oxide", shortName = "oxide", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

	@Value("${initialUsersInDev}")
	private String initialUsersInDev;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// If profile is set to dev, then create 2 default DBUsers
	@Bean
	@Profile("dev")
	public int initDefaultUsers(ServiceDBUser userRepository) {
		if (userRepository.count() == 0 && initialUsersInDev != null && !initialUsersInDev.isEmpty()
				&& initialUsersInDev.contains(",")) {
			String[] usernames = initialUsersInDev.split(",");
			for (String username : usernames) {
				userRepository.save(new DBUser(username, username));
			}
		}
		return 1;
	}

}