/**
 * Copyright (c) 2019 Rico Pahlisch and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.IOException;

/**
 * thrown when gcs initialisation failed
 */
public class GcpInitialisationFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param e
     *            wrapped exception
     */
    public GcpInitialisationFailedException(final IOException e) {
        super(e);
    }
}
