package com.playmonumenta.plugins.utils;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;

public class TableFormatter {
	public static final int UNIT_TO_PX = 4;

	public record TabulationResult(int width, Component text) {
	}

	private static final int[] ASCII_WIDTHS = {
		9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 4, 1, 3, 4, 4,
		4, 4, 1, 2, 2, 4, 4, 2, 3, 2, 4, 4, 3, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 3, 4, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
		4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 4, 2, 4, 4, 2, 4, 4, 4, 4, 4, 3, 4, 4, 3, 3, 4, 3, 4, 4,
		4, 4, 4, 4, 4, 3, 4, 4, 4, 4, 4, 4, 3, 1, 3, 4, 9,
	};
	private static final int SPACE_WIDTH = ASCII_WIDTHS[' '];

	/**
	 * Computes the display length, in the number of <i>pixels</i>.
	 *
	 * @param str the string
	 * @return the display length
	 */
	public static int computeDisplayLength(String str) {
		return str.codePoints().map(x -> {
			if (x < 128) {
				return ASCII_WIDTHS[x];
			}

			throw new IllegalArgumentException("unknown codepoint: " + x);
		}).sum();
	}

	/**
	 * Computes the display length, in the number of <i>pixels</i>.
	 *
	 * @param component the string
	 * @return the display length
	 */
	public static int computeDisplayLength(Component component) {
		return computeDisplayLength(MessagingUtils.plainText(component));
	}

	private static Component tabulateRow(
		TextColor backgroundColor, List<? extends Component> text, int[] padLens, String delimiter
	) {
		return IntStream.range(0, text.size())
			.mapToObj(i -> {
				final var comp = text.get(i);
				final var padWidth = padLens[i] - computeDisplayLength(comp);

				return Component.empty()
					.append(comp)
					.append(Component.text(" ".repeat(padWidth / SPACE_WIDTH)))
					.append(Component.text("|".repeat(padWidth % SPACE_WIDTH)).color(backgroundColor));
			})
			.collect(Component.toComponent(Component.text(delimiter)));
	}

	public static TabulationResult tabulate(
		TextColor backgroundColor, List<? extends Component> headers, List<List<Component>> entries, String delimiter
	) {
		Preconditions.checkArgument(!headers.isEmpty());
		final var cols = headers.size();
		final var widths = headers.stream().mapToInt(TableFormatter::computeDisplayLength).toArray();

		for (final var row : entries) {
			Preconditions.checkArgument(row.size() == cols);
			for (int i = 0; i < row.size(); i++) {
				widths[i] = Math.max(widths[i], computeDisplayLength(row.get(i)));
			}
		}

		final var totalWidth = Arrays.stream(widths).sum() + (cols - 1) * computeDisplayLength(delimiter);

		final var output = new ArrayList<Component>();
		output.add(tabulateRow(backgroundColor, headers, widths, delimiter));
		output.add(Component.text("-".repeat(totalWidth / computeDisplayLength("-"))));
		entries.stream().map(x -> tabulateRow(backgroundColor, x, widths, delimiter)).forEach(output::add);
		return new TabulationResult(totalWidth, Component.join(JoinConfiguration.newlines(), output));
	}
}
