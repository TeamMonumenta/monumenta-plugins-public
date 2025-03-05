package com.playmonumenta.plugins.guis.lib;

import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.entity.Player;

public interface ReactiveValue<T> {
	T get();

	void set(T val);

	default void with(Function<T, T> func) {
		set(func.apply(get()));
	}

	default <U> ReactiveValue<U> xmap(
		Function<T, U> fwd,
		Function<U, T> back
	) {
		return new ReactiveValue<>() {
			@Override
			public U get() {
				return fwd.apply(ReactiveValue.this.get());
			}

			@Override
			public void set(U val) {
				ReactiveValue.this.set(back.apply(val));
			}
		};
	}

	static <T> ReactiveValue<T> of(Gui gui, T initial) {
		return new ReactiveValue<>() {
			private T mValue = initial;

			@Override
			public T get() {
				return mValue;
			}

			@Override
			public void set(T val) {
				gui.markDirty();
				mValue = val;
			}
		};
	}

	static ReactiveValue<Integer> scoreboard(Gui gui, String score, int fallback) {
		return scoreboard(gui, gui.mPlayer, score, fallback);
	}

	static ReactiveValue<Integer> scoreboard(Gui gui, Player player, String score, int fallback) {
		return new ReactiveValue<>() {
			@Override
			public Integer get() {
				return ScoreboardUtils.getScoreboardValue(player, score).orElse(fallback);
			}

			@Override
			public void set(Integer val) {
				gui.markDirty();
				ScoreboardUtils.setScoreboardValue(player, score, val);
			}
		};
	}

	static ReactiveValue<Boolean> binaryScoreboard(Gui gui, String score, boolean fallback) {
		return scoreboard(gui, gui.mPlayer, score, fallback ? 1 : 0)
			.xmap(f -> f != 0, f -> f ? 1 : 0);
	}

	static ReactiveValue<Boolean> tag(Gui gui, String tag) {
		return tag(gui, gui.mPlayer, tag);
	}

	static ReactiveValue<Boolean> tag(Gui gui, Player player, String tag) {
		return new ReactiveValue<>() {
			@Override
			public Boolean get() {
				return ScoreboardUtils.checkTag(player, tag);
			}

			@Override
			public void set(Boolean val) {
				gui.markDirty();
				if (val) {
					player.getScoreboardTags().add(tag);
				} else {
					player.getScoreboardTags().remove(tag);
				}
			}
		};
	}

	static ReactiveValue<Boolean> permission(Gui gui, Player player, String key) {
		return new ReactiveValue<>() {
			@Override
			public Boolean get() {
				return player.hasPermission(key);
			}

			@Override
			public void set(Boolean val) {
				gui.markDirty();
				LuckPermsIntegration.setPermission(player, key, val);
			}
		};
	}

	static ReactiveValue<Boolean> permission(Gui gui, String key) {
		return permission(gui, gui.mPlayer, key);
	}

	static ReactiveValue<Boolean> togglePermission(Gui gui, Player player, String key) {
		return new ReactiveValue<>() {
			@Override
			public Boolean get() {
				return player.hasPermission(key);
			}

			@Override
			public void set(Boolean val) {
				gui.markDirty();
				if (val) {
					LuckPermsIntegration.setPermission(player, key, true);
				} else {
					LuckPermsIntegration.unsetPermission(player, key);
				}
			}
		};
	}

	static ReactiveValue<Boolean> togglePermission(Gui gui, String key) {
		return togglePermission(gui, gui.mPlayer, key);
	}

	@SuppressWarnings("EnumOrdinal")
	static <T extends Enum<T>> ReactiveValue<Integer> fromEnum(
		Gui gui, Player player, Class<T> clazz, Function<Player, T> getter, BiConsumer<T, Player> setter
	) {
		final var entries = clazz.getEnumConstants();

		return new ReactiveValue<>() {
			@Override
			public Integer get() {
				return getter.apply(player).ordinal();
			}

			@Override
			public void set(Integer val) {
				gui.markDirty();
				setter.accept(entries[val % entries.length], player);
			}
		};
	}

	@SuppressWarnings("EnumOrdinal")
	static <T extends Enum<T>> ReactiveValue<Integer> fromEnum(
		Gui gui, Class<T> clazz, Function<Player, T> getter, BiConsumer<T, Player> setter
	) {
		return fromEnum(gui, gui.mPlayer, clazz, getter, setter);
	}
}
