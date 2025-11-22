package com.playmonumenta.plugins.guis.peb;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.commands.GlowingCommand;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import java.util.Arrays;
import java.util.Locale;
import org.bukkit.Material;

final class GlowingOptionsPage extends PebPage {
	private final class GlowingFlag implements ReactiveValue<Boolean> {
		private final GlowingCommand.Option mOption;

		private GlowingFlag(GlowingCommand.Option option) {
			Preconditions.checkArgument(option != GlowingCommand.Option.ALL);
			this.mOption = option;
		}

		@Override
		public Boolean get() {
			return (mPackedOptions.get() & (1 << mOption.mPackedIndex)) != 0;
		}

		@Override
		public void set(Boolean val) {
			int mask = 1 << mOption.mPackedIndex;

			if (val) {
				mPackedOptions.set(mPackedOptions.get() | mask);
			} else {
				mPackedOptions.set(mPackedOptions.get() & ~mask);
			}
		}
	}

	private static final String[] EAGLE_EYE_OPTIONS = Arrays.stream(EagleEye.GlowingOption.values())
		.map(opt -> opt.mDescription)
		.toArray(String[]::new);
	private final ReactiveValue<Integer> mPackedOptions;

	GlowingOptionsPage(PebGui gui) {
		super(
			gui,
			Material.SPECTRAL_ARROW,
			"Glowing Settings",
			"Choose which entity types should glow. Overlapping categories will glow if any option is enabled."
		);
		mPackedOptions = ReactiveValue.scoreboard(gui, GlowingCommand.SCOREBOARD_OBJECTIVE, 0);
	}

	@Override
	protected void render() {
		super.render();

		// Toggle items
		makeOption(Material.PLAYER_HEAD, "Other Players", GlowingCommand.Option.OTHER_PLAYERS)
			.disableHead()
			.set(2, 0);
		addOption(Material.PLAYER_HEAD, "Yourself", GlowingCommand.Option.SELF, 1);
		addOption(Material.ZOMBIE_HEAD, "Mobs", GlowingCommand.Option.MOBS, 2);
		addOption(Material.WITHER_SKELETON_SKULL, "Elite Mobs", GlowingCommand.Option.ELITES, 3);

		addOption(Material.DRAGON_HEAD, "Bosses", GlowingCommand.Option.BOSSES, 5);
		addOption(Material.GLASS, "Invisible Entities", GlowingCommand.Option.INVISIBLE, 6);
		addOption(Material.IRON_INGOT, "Items", GlowingCommand.Option.ITEMS, 7);
		addOption(Material.IRON_NUGGET, "Miscellaneous", GlowingCommand.Option.MISC, 8);

		entry(
			Material.GOLD_INGOT,
			"Enable All",
			"Enable glowing for all entities"
		).onMouseClick(() -> mPackedOptions.set(0)).set(3, 3);

		entry(
			Material.DIRT,
			"Disable All",
			"Disable all glowing effects"
		).onMouseClick(() -> mPackedOptions.set(0xffffffff)).set(3, 5);

		// Eagle Eye cycle
		entry(
			Material.BOW,
			"Eagle Eye Options",
			"Additional glowing settings for Eagle Eye"
		).cycle(EagleEye.GLOWING_OPTION_SCOREBOARD_NAME, EAGLE_EYE_OPTIONS).set(4, 4);
	}

	private PebEntryHelper makeOption(Material icon, String name, GlowingCommand.Option option) {
		return entry(icon, name, "Toggle glowing for " + name.toLowerCase(Locale.ROOT))
			.invertedToggle("Glowing: ", new GlowingFlag(option));
	}

	private void addOption(Material icon, String name, GlowingCommand.Option option, int col) {
		makeOption(icon, name, option).set(2, col);
	}
}
