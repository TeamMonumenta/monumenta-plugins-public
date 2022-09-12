package com.playmonumenta.plugins.effects;

public class SplitArrowIframesEffect extends ZeroArgumentEffect {
	public static final String effectID = "SplitArrowIframesEffect";

	public SplitArrowIframesEffect(int duration) {
		super(duration, effectID);
	}

	@Override
	public String toString() {
		return String.format("SplitArrowIframesEffect duration:%d", this.getDuration());
	}
}