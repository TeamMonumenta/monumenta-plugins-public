package com.playmonumenta.plugins.particle.colors;

import java.util.function.BiFunction;
import org.bukkit.Color;

public class NamedBlendMode implements BlendMode {
	private final BiFunction<Color, Color, Color> mBlendFunction;

	private NamedBlendMode(BiFunction<Color, Color, Color> blendFunction) {
		mBlendFunction = blendFunction;
	}

	@Override
	public Color apply(Color background, Color foreground) {
		return mBlendFunction.apply(background, foreground);
	}

	public static final NamedBlendMode REPLACE = new NamedBlendMode((background, foreground) -> foreground);

	public static final NamedBlendMode MULTIPLY = new NamedBlendMode((background, foreground) -> {
		double bgR = (double) background.getRed() / 255.0;
		double bgG = (double) background.getGreen() / 255.0;
		double bgB = (double) background.getBlue() / 255.0;
		double fgR = (double) foreground.getRed() / 255.0;
		double fgG = (double) foreground.getGreen() / 255.0;
		double fgB = (double) foreground.getBlue() / 255.0;

		return Color.fromRGB((int) (bgR * fgR * 255), (int) (bgG * fgG * 255), (int) (bgB * fgB * 255));
	});

	public static final NamedBlendMode OVERLAY = new NamedBlendMode((background, foreground) -> {
		double bgR = (double) background.getRed() / 255.0;
		double bgG = (double) background.getGreen() / 255.0;
		double bgB = (double) background.getBlue() / 255.0;
		double fgR = (double) foreground.getRed() / 255.0;
		double fgG = (double) foreground.getGreen() / 255.0;
		double fgB = (double) foreground.getBlue() / 255.0;

		return Color.fromRGB(
			bgR < 0.5 ? (int) (2 * bgR * fgR * 255) : (int) ((1 - 2 * (1 - bgR) * (1 - fgR)) * 255),
			bgG < 0.5 ? (int) (2 * bgG * fgG * 255) : (int) ((1 - 2 * (1 - bgG) * (1 - fgG)) * 255),
			bgB < 0.5 ? (int) (2 * bgB * fgB * 255) : (int) ((1 - 2 * (1 - bgB) * (1 - fgB)) * 255)
		);
	});

	public static final NamedBlendMode XOR = new NamedBlendMode((background, foreground) -> {
		int bgR = background.getRed();
		int bgG = background.getGreen();
		int bgB = background.getBlue();
		int fgR = foreground.getRed();
		int fgG = foreground.getGreen();
		int fgB = foreground.getBlue();

		return Color.fromRGB(bgR ^ fgR, bgG ^ fgG, bgB ^ fgB);
	});
}
