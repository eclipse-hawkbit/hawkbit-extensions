/**
 * Copyright (c) 2018 Microsoft and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.ErrorView;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.ConditionalUiErrorHandler;
import org.eclipse.hawkbit.ui.components.NotificationUnreadButton;
import org.eclipse.hawkbit.ui.menu.DashboardMenu;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;
import org.eclipse.hawkbit.ui.push.UIEventProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.annotations.Push;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;

import java.util.List;

/**
 * hawkBit UI implementation.
 */
@SpringUI
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
public class AzureUI extends AbstractHawkbitUI {
    private static final long serialVersionUID = 1L;

    @Autowired
    AzureUI(final EventPushStrategy pushStrategy, final UIEventBus eventBus, final UIEventProvider eventProvider,
            final SpringViewProvider viewProvider, final ApplicationContext context, final DashboardMenu dashboardMenu,
            final ErrorView errorview, final NotificationUnreadButton notificationUnreadButton,
            final UiProperties uiProperties, final VaadinMessageSource i18n,
            final List<ConditionalUiErrorHandler> uiErrorHandler) {
        super(pushStrategy, eventBus, eventProvider, viewProvider, context, dashboardMenu, errorview,
                notificationUnreadButton, uiProperties, i18n, uiErrorHandler);
    }
}
