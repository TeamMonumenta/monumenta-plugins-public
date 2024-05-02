package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class AbilityHotbar {

	public static final String ABILITY_HOTBAR_TAG = "AbilityHotbarEnabled";
	private static @MonotonicNonNull AbilityHotbar INSTANCE = null;
	private static final int PERIOD = 5;

	private final Plugin mPlugin;
	private final HashMap<UUID, BossBar> mBossBarHashMap = new HashMap<>();

	public AbilityHotbar(Plugin plugin) {
		this.mPlugin = plugin;
		INSTANCE = this;

		// Update all hotbars every 5 ticks.
		BukkitRunnable mTimer = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (mBossBarHashMap.containsKey(player.getUniqueId())) {
						continue;
					}

					BossBar bossBar = BossBar.bossBar(Component.text(), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
					mBossBarHashMap.put(player.getUniqueId(), bossBar);
				}

				mBossBarHashMap.entrySet().removeIf(entry -> {
						Player player = Bukkit.getPlayer(entry.getKey());
						return player == null || !player.isOnline();
					});

				for (UUID uuid : mBossBarHashMap.keySet()) {
					Player player = Bukkit.getPlayer(uuid);
					BossBar bossBar = mBossBarHashMap.get(uuid);
					updateHotbar(player, bossBar);
				}
			}
		};

		mTimer.runTaskTimer(plugin, 0, PERIOD);
	}

	/**
	 * Generates generic cooldown text for each individual ability.
	 * Overwritten by ability.getHotbarMessage if it exists.
	 */
	private static Component getAbilityComponent(Player player, Ability ability) {
		if (INSTANCE == null || !playerHasAbilityHotbarEnabled(player)) {
			return Component.text("");
		}

		if (ability instanceof DepthsAbility depthsAbility) {
			return getDepthsAbilityComponent(player, depthsAbility);
		}

		if (ability.getHotbarMessage() != null) {
			return ability.getHotbarMessage();
		}

		ClassAbility classAbility = ability.getInfo().getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
		int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;
		int maxCharges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getMaxCharges() : 0;

		TextColor color = ability.getInfo().getActionBarColor();
		String name = getAbilityName(ability);

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name, color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (charges > 0 && maxCharges > 1) {
			output = output.append(Component.text(charges + "/" + maxCharges, (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));
		} else if (AbilityUtils.isSilenced(player)) {
			output = output.append(Component.text(((int) Math.ceil(AbilityUtils.getSilenceDuration(player) / 20.0)) + "s", NamedTextColor.RED));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}


	/**
	 * Generates generic cooldown text for each individual DEPTHS / ZENITH ability.
	 * Overwritten by ability.getHotbarMessage if it exists.
	 */
	private static Component getDepthsAbilityComponent(Player player, DepthsAbility ability) {
		if (INSTANCE == null || !playerHasAbilityHotbarEnabled(player)) {
			return Component.text("");
		}

		if (ability.getHotbarMessage() != null) {
			return ability.getHotbarMessage();
		}

		DepthsTrigger depthsTrigger = ability.getInfo().getDepthsTrigger();

		ClassAbility classAbility = ability.getInfo().getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
		int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;
		int maxCharges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getMaxCharges() : 0;

		TextColor color = ability.getInfo().getActionBarColor();
		String name = switch (depthsTrigger) {
			case RIGHT_CLICK -> "Right";
			case SHIFT_LEFT_CLICK -> "↓ Left";
			case SHIFT_RIGHT_CLICK -> "↓ Right";
			case SHIFT_BOW -> "↓ Bow";
			case SWAP -> "Swap";
			case LIFELINE -> "Life";
			default -> "";
		};

		if (name.equals("")) {
			if (ability instanceof RapidFire) {
				name = "RF";
			} else {
				return Component.text("");
			}
		}

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name, color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (charges > 0 && maxCharges > 1) {
			output = output.append(Component.text(charges + "/" + maxCharges, (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));
		} else if (AbilityUtils.isSilenced(player)) {
			output = output.append(Component.text(((int) Math.ceil(AbilityUtils.getSilenceDuration(player) / 20.0)) + "s", NamedTextColor.RED));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}

	/**
	 * Generates ability hotbar text based on player's current abilities.
	 */
	public static void updateHotbar(Player player, BossBar bossBar) {
		if (INSTANCE == null || !playerHasAbilityHotbarEnabled(player)) {
			player.hideBossBar(bossBar);
			return;
		}

		List<Ability> abilityList = INSTANCE.mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilitiesIgnoringSilence().stream()
			.filter(ability -> shouldHandleAbility(player, ability))
			.sorted(Comparator.comparing(AbilityHotbar::getAbilityName))
			.toList();

		if (abilityList.size() <= 0) {
			player.hideBossBar(bossBar);
			return;
		}

		// Create Output List
		Component output = Component.text("");

		for (int i = 0; i < abilityList.size(); i++) {
			Ability ability = abilityList.get(i);
			Component abilityComponent = getAbilityComponent(player, ability);

			if (abilityComponent.equals(Component.text(""))) {
				continue;
			}

			output = output.append(abilityComponent);

			if (i + 1 < abilityList.size()) {
				output = output.append(Component.text(" "));
			}
		}

		bossBar.name(output);
		player.showBossBar(bossBar);
	}

	private static String getAbilityName(Ability ability) {
		if (ability instanceof DepthsAbility depthsAbility) {
			// For the sake of ordering it using the alphabetical
			switch (depthsAbility.getInfo().getDepthsTrigger()) {
				case RIGHT_CLICK:
					return "a";
				case SHIFT_LEFT_CLICK:
					return "b";
				case SHIFT_RIGHT_CLICK:
					return "c";
				case SHIFT_BOW:
					return "d";
				case SWAP:
					return "e";
				case LIFELINE:
					return "f";
				default:
					break;
			}
		}

		if (ability.getInfo().getHotbarName() != null) {
			return ability.getInfo().getHotbarName();
		}
		return "Error";
	}

	public static boolean playerHasAbilityHotbarEnabled(Player player) {
		return player.getScoreboardTags().contains(ABILITY_HOTBAR_TAG);
	}

	private static boolean shouldHandleAbility(Player player, Ability ability) {
		return ability != null
			&& (ability.getInfo().getBaseCooldown(player, ability.getAbilityScore()) > 0 || ability instanceof AbilityWithChargesOrStacks
			|| ability instanceof AlchemicalArtillery); // these are passives with modes
	}
}
