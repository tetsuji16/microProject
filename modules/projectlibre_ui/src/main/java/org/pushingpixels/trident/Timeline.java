package org.pushingpixels.trident;

import org.pushingpixels.trident.api.callback.TimelineCallback;
import org.pushingpixels.trident.api.ease.TimelineEase;

/**
 * Compatibility wrapper for the legacy Trident timeline API.
 *
 * <p>Radiance Trident now exposes a builder-based API under
 * {@code org.pushingpixels.trident.api}. The forked Flamingo sources in this
 * repo still construct timelines directly, so this shim forwards the small
 * subset of methods they use to the new builder.
 */
public final class Timeline {
	private final org.pushingpixels.trident.api.Timeline.BaseBuilder delegate;
	private org.pushingpixels.trident.api.Timeline timeline;

	public Timeline(Object target) {
		this.delegate = org.pushingpixels.trident.api.Timeline.builder(target);
	}

	public Timeline addPropertyToInterpolate(String propertyName, float from,
			float to) {
		this.timeline = null;
		this.delegate.addPropertyToInterpolate(propertyName, from, to);
		return this;
	}

	public Timeline addPropertyToInterpolate(String propertyName, Object from,
			Object to) {
		this.timeline = null;
		this.delegate.addPropertyToInterpolate(propertyName, from, to);
		return this;
	}

	public Timeline addCallback(TimelineCallback callback) {
		this.timeline = null;
		this.delegate.addCallback(callback);
		return this;
	}

	public Timeline setDuration(long duration) {
		this.timeline = null;
		this.delegate.setDuration(duration);
		return this;
	}

	public Timeline setName(String name) {
		this.timeline = null;
		this.delegate.setName(name);
		return this;
	}

	public Timeline setEase(TimelineEase ease) {
		this.timeline = null;
		this.delegate.setEase(ease);
		return this;
	}

	public Timeline setInitialDelay(long delay) {
		this.timeline = null;
		this.delegate.setInitialDelay(delay);
		return this;
	}

	public Timeline setCycleDelay(long delay) {
		this.timeline = null;
		this.delegate.setCycleDelay(delay);
		return this;
	}

	public Timeline setRepeatCount(int count) {
		this.timeline = null;
		this.delegate.setRepeatCount(count);
		return this;
	}

	public Timeline setRepeatBehavior(
			org.pushingpixels.trident.api.Timeline.RepeatBehavior repeatBehavior) {
		this.timeline = null;
		this.delegate.setRepeatBehavior(repeatBehavior);
		return this;
	}

	public void play() {
		this.timeline().play();
	}

	public void playReverse() {
		this.timeline().playReverse();
	}

	public void playSkipping(long skipMillis) {
		this.timeline().playSkipping(skipMillis);
	}

	public void playReverseSkipping(long skipMillis) {
		this.timeline().playReverseSkipping(skipMillis);
	}

	public void playLoop(
			org.pushingpixels.trident.api.Timeline.RepeatBehavior repeatBehavior) {
		this.timeline().playLoop(repeatBehavior);
	}

	public void playLoop(int repeatCount,
			org.pushingpixels.trident.api.Timeline.RepeatBehavior repeatBehavior) {
		this.timeline().playLoop(repeatCount, repeatBehavior);
	}

	public void playLoopSkipping(
			org.pushingpixels.trident.api.Timeline.RepeatBehavior repeatBehavior,
			long skipMillis) {
		this.timeline().playLoopSkipping(repeatBehavior, skipMillis);
	}

	public void playLoopSkipping(int repeatCount,
			org.pushingpixels.trident.api.Timeline.RepeatBehavior repeatBehavior,
			long skipMillis) {
		this.timeline().playLoopSkipping(repeatCount, repeatBehavior, skipMillis);
	}

	public void replay() {
		this.timeline().replay();
	}

	public void replayReverse() {
		this.timeline().replayReverse();
	}

	public void cancel() {
		this.timeline().cancel();
	}

	public void abort() {
		this.timeline().abort();
	}

	public void suspend() {
		this.timeline().suspend();
	}

	public void resume() {
		this.timeline().resume();
	}

	public void end() {
		this.timeline().end();
	}

	private org.pushingpixels.trident.api.Timeline timeline() {
		if (this.timeline == null) {
			this.timeline = this.delegate.build();
		}
		return this.timeline;
	}
}
