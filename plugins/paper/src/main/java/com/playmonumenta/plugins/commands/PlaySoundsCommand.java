package com.playmonumenta.plugins.commands;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class PlaySoundsCommand {

	@SuppressWarnings("PMD.EmptyCatchBlock")
	public static void register() {
		MultiLiteralArgument soundCategoryArgument = new MultiLiteralArgument("category",
			Arrays.stream(Sound.Source.values()).map(c -> c.name().toLowerCase(Locale.ROOT)).toArray(String[]::new));
		Argument<?> soundsArgument = new GreedyStringArgument("sounds")
			.replaceSuggestions((info, builder) -> {
				AtomicReference<SuggestionsBuilder> builderRef = new AtomicReference<>(builder);
				try {
					parseSoundEvents(info.currentArg(), info.currentInput().length() - info.currentArg().length(), Sound.Source.MASTER, builderRef);
				} catch (CommandUtils.ParseFailedException ignore) {
					// ignore
				}
				return Objects.requireNonNull(builderRef.get()).buildFuture();
			});

		new CommandAPICommand("playsounds")
			.withPermission("monumenta.command.playsounds")
			.withArguments(
				new EntitySelectorArgument.ManyPlayers("players"),
				new LocationArgument("location"),
				soundCategoryArgument,
				soundsArgument
			).executes((sender, args) -> {
				Location location = Objects.requireNonNull(args.getUnchecked("location"));
				execute(args.getUnchecked("players"),
					(audience, sound) -> audience.playSound(sound, location.getX(), location.getY(), location.getZ()),
					Objects.requireNonNull(args.getUnchecked("category")),
					args.getUnchecked("sounds"));
			}).register();

		new CommandAPICommand("playsounds")
			.withPermission("monumenta.command.playsounds")
			.withArguments(
				new EntitySelectorArgument.ManyPlayers("players"),
				new EntitySelectorArgument.OneEntity("entity"),
				soundCategoryArgument,
				soundsArgument
			).executes((sender, args) -> {
				Entity entity = Objects.requireNonNull(args.getUnchecked("entity"));
				execute(args.getUnchecked("players"),
					(audience, sound) -> audience.playSound(sound, entity),
					Objects.requireNonNull(args.getUnchecked("category")),
					args.getUnchecked("sounds"));
			})
			.register();
	}

	private static void execute(Collection<Player> players, BiConsumer<Audience, Sound> playSoundFunction, String source, String sounds) throws WrapperCommandSyntaxException {
		Sound.Source soundSource;
		try {
			soundSource = Sound.Source.valueOf(source.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw CommandAPI.failWithString("Invalid sound category");
		}
		Event events;
		try {
			events = parseSoundEvents(sounds, 0, soundSource, null);
		} catch (CommandUtils.ParseFailedException e) {
			throw CommandAPI.failWithString("Invalid sound list");
		}
		ForwardingAudience playerAudience = Audience.audience(players);
		Consumer<Sound> playSound = sound -> playSoundFunction.accept(playerAudience, sound);
		events.run(playSound, () -> {
		});
	}

	private abstract static class Event {
		@MonotonicNonNull
		Event mNextEvent;

		abstract void run(Consumer<Sound> playSound, Runnable onFinish);
	}

	private static class DelayEvent extends Event {
		final int mMin;
		final int mMax;

		DelayEvent(int min, int max) {
			mMin = min;
			mMax = max;
		}

		@Override
		public void run(Consumer<Sound> playSound, Runnable onFinish) {
			int delay = FastUtils.randomIntInRange(mMin, mMax);
			if (delay == 0) {
				if (mNextEvent != null) {
					mNextEvent.run(playSound, onFinish);
				} else {
					onFinish.run();
				}
			} else {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					if (mNextEvent != null) {
						mNextEvent.run(playSound, onFinish);
					} else {
						onFinish.run();
					}
				}, delay);
			}
		}
	}

	private static class SoundEvent extends Event {
		final List<Sound> mSounds;

		SoundEvent(List<Sound> sounds) {
			mSounds = sounds;
		}

		@Override
		public void run(Consumer<Sound> playSound, Runnable onFinish) {
			playSound.accept(FastUtils.getRandomElement(mSounds));
			if (mNextEvent != null) {
				mNextEvent.run(playSound, onFinish);
			} else {
				onFinish.run();
			}
		}
	}

	private static class RepeatEvent extends Event {
		final Event mEvents;
		final int mMin;
		final int mMax;

		RepeatEvent(Event events, int min, int max) {
			mEvents = events;
			mMin = min;
			mMax = max;
		}

		@Override
		public void run(Consumer<Sound> playSound, Runnable onFinish) {
			int repeats = FastUtils.randomIntInRange(mMin, mMax);
			run(playSound, onFinish, repeats);
		}

		void run(Consumer<Sound> playSound, Runnable onFinish, int repeats) {
			if (repeats > 0) {
				mEvents.run(playSound, () -> {
					run(playSound, onFinish, repeats - 1);
				});
			} else if (mNextEvent != null) {
				mNextEvent.run(playSound, onFinish);
			} else {
				onFinish.run();
			}
		}
	}

	enum EventType {
		SOUND, DELAY, REPEAT
	}

	private static Event parseSoundEvents(String arg, int offset, Sound.Source source, @Nullable AtomicReference<SuggestionsBuilder> suggestionsBuilder) throws CommandUtils.ParseFailedException {
		CommandUtils.CommandArgumentScanner scanner = new CommandUtils.CommandArgumentScanner(arg, offset, " ,|[]", suggestionsBuilder);
		return scanEvents(source, scanner, -1);
	}

	private static Event scanEvents(Sound.Source source, CommandUtils.CommandArgumentScanner scanner, int endChar) throws CommandUtils.ParseFailedException {
		Event firstEvent = null;
		Event previousEvent = null;
		while (true) {
			EventType type = scanner.scanEnum(EventType.class);
			scanner.next(' ');
			Event event;
			switch (type) {
				case SOUND -> {
					List<Sound> sounds = new ArrayList<>();
					do {
						Sound.Builder soundBuilder = Sound.sound();
						soundBuilder.type(scanner.<Key>scanToken(prefix -> Arrays.stream(org.bukkit.Sound.values()).map(s -> s.key().toString()).filter(key -> key.contains(prefix)).toList(),
							s -> {
								try {
									return Key.key(s);
								} catch (InvalidKeyException e) {
									return null;
								}
							}));
						scanner.next(' ');
						soundBuilder.volume(scanner.scanFloat(0, Float.MAX_VALUE, 1));
						if (scanner.tryNext(' ')) {
							soundBuilder.pitch(scanner.scanFloat(0, 2, 1));
						}
						if (scanner.tryNext(' ')) {
							soundBuilder.seed(scanner.scanLong());
						}
						soundBuilder.source(source);
						sounds.add(soundBuilder.build());
					} while (scanner.tryNext('|'));
					event = new SoundEvent(sounds);
				}
				case DELAY -> {
					int minDelay = scanner.scanInt(0, Integer.MAX_VALUE, 1);
					int maxDelay;
					if (scanner.tryNext(' ')) {
						maxDelay = scanner.scanInt(minDelay, Integer.MAX_VALUE, 1);
					} else {
						maxDelay = minDelay;
					}
					event = new DelayEvent(minDelay, maxDelay);
				}
				case REPEAT -> {
					int min = scanner.scanInt(0, 99, 1);
					scanner.next(' ');
					int max;
					if (!scanner.tryNext('[')) {
						max = scanner.scanInt(min, 99, min);
						scanner.next(' ');
						scanner.next('[');
					} else {
						max = min;
					}
					Event events = scanEvents(source, scanner, ']');
					event = new RepeatEvent(events, min, max);
				}
				default -> {
					throw new CommandUtils.ParseFailedException();
				}
			}
			if (firstEvent == null) {
				firstEvent = event;
			} else {
				Objects.requireNonNull(previousEvent).mNextEvent = event;
			}
			previousEvent = event;
			if (scanner.tryNext(',')) {
				scanner.next(' ');
				continue;
			}
			if (endChar < 0) {
				if (scanner.hasMore()) {
					throw new CommandUtils.ParseFailedException();
				} else {
					return firstEvent;
				}
			} else {
				scanner.next((char) endChar);
				return firstEvent;
			}
		}
	}

}
