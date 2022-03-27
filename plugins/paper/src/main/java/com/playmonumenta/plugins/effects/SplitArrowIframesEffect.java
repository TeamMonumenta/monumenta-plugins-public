package com.playmonumenta.plugins.effects;

public class SplitArrowIframesEffect extends ZeroArgumentEffect {

	public SplitArrowIframesEffect(int duration) {
		super(duration);
	}

	@Override
	public String toString() {
		return String.format("SplitArrowIframesEffect duration:%d", this.getDuration());
	}
}