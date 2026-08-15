package com.projectlibre.ui.shell;

/** Connects the ribbon switch to the recovery subsystem. */
public interface AutoSaveControl {
	boolean isEnabled();

	void setEnabled(boolean enabled);

	AutoSaveControl DISABLED = new AutoSaveControl() {
		public boolean isEnabled() {
			return false;
		}

		public void setEnabled(boolean enabled) {
		}
	};
}
