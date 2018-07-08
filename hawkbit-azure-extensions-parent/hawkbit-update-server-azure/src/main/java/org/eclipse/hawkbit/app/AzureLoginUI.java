/**
 * Copyright (c) 2018 Microsoft Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.login.AbstractHawkbitLoginUI;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.spring.annotation.SpringUI;

/**
 * hawkBit login UI implementation.
*/
@SpringUI(path = HawkbitTheme.LOGIN_UI_PATH)
public class MyLoginUI extends AbstractHawkbitLoginUI {
    private static final long serialVersionUID = 1L;

    @Autowired
    MyLoginUI(final ApplicationContext context, final VaadinSecurity vaadinSecurity, final VaadinMessageSource i18n,
            final UiProperties uiProperties, final MultitenancyIndicator multiTenancyIndicator) {
        super(context, vaadinSecurity, i18n, uiProperties, multiTenancyIndicator);
    }

}
