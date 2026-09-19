/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
/**
 * JAXB configuration-field compatibility boundary.
 *
 * <p>The application domain uses {@code com.microproject.field} for runtime
 * field definitions and values.  This package is retained for the legacy
 * configuration dictionary and XML adapters used by the exchange layer and
 * by the compatibility node model.  New domain code must not introduce a
 * second field implementation here; convert at the configuration boundary
 * instead.  The duplicate names are intentional until the persisted JAXB
 * model can be migrated without breaking existing configuration files.</p>
 */
package com.microproject.core.fields;
