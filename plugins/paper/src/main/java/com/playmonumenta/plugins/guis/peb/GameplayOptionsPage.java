package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityHotbar;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.cosmetics.punches.PlayerPunches;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import com.playmonumenta.plugins.itemstats.enchantments.Darksight;
import com.playmonumenta.plugins.itemstats.enchantments.Radiant;
import com.playmonumenta.plugins.listeners.PotionConsumeListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

final class GameplayOptionsPage extends PebPage {
	private enum RocketJumpSetting {
		NONE(0),
		SELF(1),
		ALL(100);

		private final int mScoreboardValue;

		RocketJumpSetting(int value) {
			this.mScoreboardValue = value;
		}

		private static RocketJumpSetting get(Player player) {
			int value = ScoreboardUtils.getScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE).orElse(0);

			return Arrays.stream(values())
				.filter(x -> x.mScoreboardValue == value)
				.findFirst()
				.orElse(NONE);
		}

		private void set(Player player) {
			ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, mScoreboardValue);
		}
	}

	private enum EliteFinisherSelfSetting {
		GLOW_SHOW(true, true),
		GLOW_HIDE(true, false),
		NO_GLOW_SHOW(false, true),
		NO_GLOW_HIDE(false, false);

		private final boolean mGlow;
		private final boolean mShow;

		EliteFinisherSelfSetting(boolean glow, boolean show) {
			this.mGlow = glow;
			this.mShow = show;
		}

		private static EliteFinisherSelfSetting get(Player player) {
			boolean glow = ScoreboardUtils.checkTag(player, EliteFinishers.FINISHER_GLOW_TAG);
			boolean show = ScoreboardUtils.checkTag(player, EliteFinishers.FINISHER_SHOW_TAG);

			return Arrays.stream(values())
				.filter(x -> x.mShow == show && x.mGlow == glow)
				.findFirst()
				.orElse(GLOW_SHOW);
		}

		private void set(Player player) {
			final var tags = player.getScoreboardTags();
			tags.remove(EliteFinishers.FINISHER_GLOW_TAG);
			tags.remove(EliteFinishers.FINISHER_SHOW_TAG);

			if (mGlow) {
				tags.add(EliteFinishers.FINISHER_GLOW_TAG);
			}

			if (mShow) {
				tags.add(EliteFinishers.FINISHER_SHOW_TAG);
			}
		}
	}

	GameplayOptionsPage(PebGui gui) {
		super(gui, Material.DIAMOND_SWORD, "Gameplay Options", "Options related to gameplay");
	}

	private static boolean isOnPlayerPunchToggleCooldown(Player player, UUID playerUuid, CosmeticsManager cosmeticsManager, long currentTime) {
		long lastToggle = cosmeticsManager.mOptOutPunchCooldowns.getOrDefault(playerUuid, 0L);

		if (currentTime - lastToggle < CosmeticsManager.OPT_OUT_PUNCH_COOLDOWN) {
			long remainingTime = (CosmeticsManager.OPT_OUT_PUNCH_COOLDOWN - (currentTime - lastToggle)) / 1000;
			player.sendMessage(Component.text("You must wait " + remainingTime + " more second" + (remainingTime == 1 ? "" : "s") + " before toggling this option!", NamedTextColor.RED));
			return true;
		}

		return false;
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.NETHER_STAR,
			"Particle Options",
			"Click to choose how many particles will be shown for different categories."
		).switchTo(PebGui.PARTIAL_PARTICLES_PAGE).set(2, 2);

		entry(
			Material.SPECTRAL_ARROW,
			"Glowing Options",
			"Click to choose your preferences for the \"glowing\" effect."
		).switchTo(PebGui.GLOWING_PAGE).set(2, 3);

		entry(
			Material.JUKEBOX,
			"Music Options",
			"Click to choose your preferences across a wide variety of music"
		).switchTo(PebGui.SOUND_CONTROLS_PAGE).set(2, 4);

		entry(
			Material.NOTE_BLOCK,
			"Passive ability sounds",
			"Click to toggle whether some sounds from long-lasting ability effects and enchantments are played."
		).invertedToggle("Passive ability sounds: ", AbilityUtils.PASSIVE_SOUNDS_DISABLED_TAG).set(2, 5);

		entry(
			Material.GLASS_BOTTLE,
			"Inventory Drink",
			"Click to toggle drinking potions with a right click in any inventory."
		).toggle("Inventory drink: ", PotionConsumeListener.INVENTORY_DRINK_TAG).set(2, 6);

		entry(
			Material.PAPER,
			"Ability Hotbar",
			"Click to toggle ability HUD."
		).toggle("Ability hotbar: ", AbilityHotbar.ABILITY_HOTBAR_TAG).set(3, 2);

		entry(
			Material.LANTERN,
			"Toggle Darksight",
			"Click to toggle whether Darksight provides Night Vision."
		).invertedToggle("Night vision: ", Darksight.DARKSIGHT_DISABLED_TAG).set(3, 3);

		entry(
			Material.SOUL_LANTERN,
			"Toggle Radiant",
			"Click to toggle whether Radiant provides Night Vision."
		).invertedToggle("Night vision: ", Radiant.NIGHTVISION_DISABLED_TAG).set(3, 4);

		entry(
			Material.FIREWORK_ROCKET,
			"Rocket Jump",
			"Click to configure rocket jump settings."
		).cycle(
			ReactiveValue.fromEnum(mGui, RocketJumpSetting.class, RocketJumpSetting::get, RocketJumpSetting::set),
			"Disabled",
			"Self only",
			"All"
		).set(3, 5);

		entry(
			Material.ZOMBIE_HEAD,
			"Cloned Finisher Elites Visibility - Self",
			"Click to toggle whether your cloned elites in finishers glow and are visible."
		).cycle(
			ReactiveValue.fromEnum(mGui, EliteFinisherSelfSetting.class, EliteFinisherSelfSetting::get, EliteFinisherSelfSetting::set),
			"Show and Glow",
			"Hide and Glow",
			"Show and Don't Glow",
			"Hide completely"
		).set(3, 6);

		entry(
			Material.SKELETON_SKULL,
			"Cloned Finisher Elites Visibility - Other",
			"Click to toggle whether other players' cloned elites in finishers glow and are visible."
		).invertedToggle("Show Others' Elite Finisher Clone: ", ReactiveValue.tag(mGui, EliteFinishers.FINISHER_HIDE_OTHER_TAG)).set(4, 6);

		entry(
			Material.CHEST,
			"Toggle auto-storage of LOOTBOX shares",
			"Click to toggle whether your shares of a loot chest are stored in the LOOTBOX if you open the chest." +
				" Also automatically closes the chest. Only works with the Box of Endless Echoes, " +
				"and applies 1.5s of slowness to you upon opening the chest."
		).toggle("Auto-store: ", "UseLootboxOnSelf").set(4, 2);

		entry(
			Material.ENDER_EYE,
			"Toggle Spectating After Death",
			"Click to toggle whether you spectate the area in which you die for 3 seconds after dying."
		).invertedToggle("Spectating after death: ", RespawnStasis.SPECTATE_DISABLE_TAG).set(4, 3);

		entry(
			Material.TRIDENT,
			"Disable Depth Strider while Riptiding",
			"Click to toggle whether depth strider is disabled constantly while holding a riptide trident, or only while riptiding."
		).toggle(
			"Disabled while: ",
			Constants.Tags.DEPTH_STRIDER_DISABLED_ONLY_WHILE_RIPTIDING,
			"<white>riptiding", "<white>holding a Riptide trident"
		).set(4, 4);

		final var playerPunchesOptOut = ReactiveValue.togglePermission(mGui, "monumenta.cosmetics.punchoptout");

		entry(
			Material.FEATHER,
			"Opt Out of Player Punches",
			"Click to toggle whether you are opted out of being able to punch other players and having the ability to be launched."
		).lore("").lore("Player Punches: " + (playerPunchesOptOut.get() ? "<white>opted out" : "<white>opted in")).onMouseClick(() -> {
			UUID playerUuid = getPlayer().getUniqueId();
			CosmeticsManager cosmeticsManager = CosmeticsManager.getInstance();
			long currentTime = System.currentTimeMillis();

			if (isOnPlayerPunchToggleCooldown(getPlayer(), playerUuid, cosmeticsManager, currentTime)) {
				return;
			}

			if (playerPunchesOptOut.get()) {
				playerPunchesOptOut.set(false);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> PlayerPunches.handleLogin(getPlayer()), 20);
				getPlayer().sendMessage(Component.text("You have opted back in to Player Punches!", NamedTextColor.GOLD));
			} else {
				playerPunchesOptOut.set(true);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> PlayerPunches.handleLogin(getPlayer()), 20);
				getPlayer().sendMessage(Component.text("You have opted out of Player Punches!", NamedTextColor.GOLD));
			}

			cosmeticsManager.mOptOutPunchCooldowns.put(playerUuid, currentTime);
		}).set(4, 5);
	}
}
