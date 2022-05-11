package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstance;

import java.util.UUID;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface VirtualDataInstanceCache {

    UUID write(ExternalDataInstance dataInstance);

    ExternalDataInstance read(UUID key);

    boolean contains(UUID key);
}
