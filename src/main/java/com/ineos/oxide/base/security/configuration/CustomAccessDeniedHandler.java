package com.ineos.oxide.base.security.configuration;

import com.ineos.oxide.base.security.ui.AccessDeniedView;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;

import jakarta.servlet.http.HttpServletResponse;

public class CustomAccessDeniedHandler extends RouteNotFoundError {
    @Override
    public int setErrorParameter(final BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        event.forwardTo(AccessDeniedView.class);
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
