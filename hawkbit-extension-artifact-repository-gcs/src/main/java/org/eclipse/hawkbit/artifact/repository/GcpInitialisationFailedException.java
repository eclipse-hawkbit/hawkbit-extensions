/**
 * Copyright (c) 2019 Rico Pahlisch and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
