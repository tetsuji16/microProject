package com.projectlibre1.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

/**
 * Creates object streams for persisted ProjectLibre data with an explicit class
 * boundary and resource limits.
 */
public final class SafeObjectInput {
	private static final Logger logger = Logger.getLogger(SafeObjectInput.class.getName());
	static final long MAX_DEPTH = 256;
	static final long MAX_REFERENCES = 1_000_000;
	static final long MAX_ARRAY_LENGTH = 1_000_000;
	static final long MAX_STREAM_BYTES = 256L * 1024L * 1024L;

	private static final String[] ALLOWED_APPLICATION_PREFIXES = {
		"com.projectlibre1.",
		"org.projectlibre.",
		"org.projectlibre1.",
		"org.jdesktop.swingx.calendar." // legacy workspace compatibility
	};
	private static final String[] ALLOWED_JDK_PACKAGES = {
		"java.lang",
		"java.math",
		"java.text",
		"java.time",
		"java.util",
		"java.util.concurrent",
		"java.util.concurrent.locks",
		"java.awt",
		"java.awt.font",
		"java.awt.geom",
		"java.awt.print",
		"javax.print",
		"javax.print.attribute",
		"javax.print.attribute.standard",
		"javax.swing",
		"javax.swing.border",
		"javax.swing.event",
		"javax.swing.table",
		"javax.swing.tree",
		"javax.swing.undo"
	};
	private static final String[] ALLOWED_JDK_CLASSES = {
		"sun.util.calendar.ZoneInfo" // serialized by legacy java.util.TimeZone instances in POD files
	};

	private static final ObjectInputFilter PROJECTLIBRE_FILTER = SafeObjectInput::checkInput;

	private SafeObjectInput() {
	}

	public static ObjectInputStream create(InputStream input) throws IOException {
		ObjectInputStream stream = new ObjectInputStream(input);
		stream.setObjectInputFilter(PROJECTLIBRE_FILTER);
		return stream;
	}

	private static ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo info) {
		if (info.depth() > MAX_DEPTH
				|| info.references() > MAX_REFERENCES
				|| info.streamBytes() > MAX_STREAM_BYTES
				|| info.arrayLength() > MAX_ARRAY_LENGTH) {
			return ObjectInputFilter.Status.REJECTED;
		}

		Class<?> type = info.serialClass();
		if (type == null)
			return ObjectInputFilter.Status.UNDECIDED;
		while (type.isArray())
			type = type.getComponentType();
		if (type.isPrimitive())
			return ObjectInputFilter.Status.ALLOWED;

		String className = type.getName();
		for (String allowedClass : ALLOWED_JDK_CLASSES) {
			if (className.equals(allowedClass))
				return ObjectInputFilter.Status.ALLOWED;
		}
		for (String prefix : ALLOWED_APPLICATION_PREFIXES) {
			if (className.startsWith(prefix))
				return ObjectInputFilter.Status.ALLOWED;
		}
		String packageName = type.getPackageName();
		for (String allowedPackage : ALLOWED_JDK_PACKAGES) {
			if (packageName.equals(allowedPackage))
				return ObjectInputFilter.Status.ALLOWED;
		}
		logger.warning("Rejected serialized class: " + className);
		return ObjectInputFilter.Status.REJECTED;
	}
}
