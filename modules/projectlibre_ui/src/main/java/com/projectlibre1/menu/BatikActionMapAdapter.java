package com.projectlibre1.menu;

import javax.swing.Action;

import com.projectlibre1.menu.resource.ActionMap;
import com.projectlibre1.menu.resource.MissingListenerException;

final class BatikActionMapAdapter implements ActionMap {
	private final ProjectMenuActionMap delegate;

	BatikActionMapAdapter(ProjectMenuActionMap delegate) {
		this.delegate = delegate;
	}

	@Override
	public Action getAction(String key) throws MissingListenerException {
		return delegate.getAction(key);
	}

	@Override
	public String getStringFromAction(Action action) throws MissingListenerException {
		return delegate.getStringFromAction(action);
	}
}
