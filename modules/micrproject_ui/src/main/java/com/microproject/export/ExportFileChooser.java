/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.util.Optional;

public interface ExportFileChooser {
	Optional<ExportTarget> choose(String projectName, Component parentComponent);
}
