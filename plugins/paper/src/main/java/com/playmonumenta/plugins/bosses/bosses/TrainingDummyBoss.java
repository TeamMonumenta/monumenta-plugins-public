package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class TrainingDummyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_training_dummy";
	public static final int detectionRange = 25;
	private static final DecimalFormat cutoffDigits = new DecimalFormat("0.0####"); // number of 0s/#s determines maximum digits shown
	private static final DecimalFormat holoDigits = new DecimalFormat("0.0"); // number of 0s/#s determines maximum digits shown

	private final Component HOLOGRAM_DEFAULT_NAME = Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
		                                                .append(Component.text("???", NamedTextColor.DARK_AQUA))
		                                                .append(Component.text(" (", NamedTextColor.YELLOW))
		                                                .append(Component.text("???", NamedTextColor.DARK_AQUA))
		                                                .append(Component.text("/", NamedTextColor.YELLOW))
		                                                .append(Component.text("???", NamedTextColor.DARK_AQUA))
		                                                .append(Component.text(")", NamedTextColor.YELLOW));

	private double mDPSCounter = -1;
	private double mDPSCounter10s = -1;
	private double mDPSDisp = -1;
	private double mDPSDisp10s = -1;
	private double mMaxDPS = -1;
	private @Nullable ArmorStand mHologram = null;


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TrainingDummyBoss(plugin, boss);
	}

	public TrainingDummyBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRunAction(() -> {
				boss.setHealth(EntityUtils.getMaxHealth(boss));
			}, 60 * 20)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				if (mHologram != null) {
					mHologram.teleport(mBoss.getEyeLocation().add(0, 0.5, 0));
				}
			})
		);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, null);
		boss.setRemoveWhenFarAway(false);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		double damage = event.getFinalDamage(false);

		// Damage smaller than this is only meant to tag the mob as damaged by a player
		if (damage < 0.01) {
			return;
		}

		String damageString = damageToString(damage);

		if (source instanceof Player player) {
			player.sendMessage(ChatColor.GOLD + "Damage: " + ChatColor.RED + damageString);

			if (mHologram == null) {
				mHologram = (ArmorStand) mBoss.getWorld().spawnEntity(mBoss.getEyeLocation().add(0, 0.5, 0), EntityType.ARMOR_STAND);
				mHologram.setMarker(true);
				mHologram.setInvisible(true);
				mHologram.setInvulnerable(true);
				mHologram.customName(HOLOGRAM_DEFAULT_NAME);
				mHologram.setCustomNameVisible(true);
				mHologram.setGravity(false);
				mHologram.setBasePlate(false);
				mHologram.setCollidable(false);
				EntityUtils.setRemoveEntityOnUnload(mHologram);
			}

			if (mDPSCounter == -1) {
				mDPSCounter = 0;

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					mDPSDisp = mDPSCounter;

					if (mMaxDPS < mDPSCounter) {
						mMaxDPS = mDPSCounter;
					}

					if (mHologram != null) {
						Component hologramDisplay;

						if (mDPSDisp10s >= 0) {
							hologramDisplay = Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
								                  .append(Component.text(damageToString(mDPSDisp, true), NamedTextColor.RED))
								                  .append(Component.text(" (", NamedTextColor.YELLOW))
								                  .append(Component.text(damageToString(mDPSDisp10s, true), NamedTextColor.GREEN))
								                  .append(Component.text("/", NamedTextColor.YELLOW))
								                  .append(Component.text(damageToString(mMaxDPS, true), NamedTextColor.GOLD))
								                  .append(Component.text(")", NamedTextColor.YELLOW));
						} else {
							hologramDisplay = Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
								                  .append(Component.text(damageToString(mDPSDisp, true), NamedTextColor.RED))
								                  .append(Component.text(" (", NamedTextColor.YELLOW))
								                  .append(Component.text("???", NamedTextColor.DARK_AQUA))
								                  .append(Component.text("/", NamedTextColor.YELLOW))
								                  .append(Component.text(damageToString(mMaxDPS, true), NamedTextColor.GOLD))
								                  .append(Component.text(")", NamedTextColor.YELLOW));
						}

						mHologram.customName(hologramDisplay);
					}
					mDPSCounter = -1;
				}, 20);
			}

			if (mDPSCounter10s == -1) {
				mDPSCounter10s = 0;

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (mHologram == null) {
						return;
					}
					mDPSDisp10s = mDPSCounter10s / 10;
					mHologram.customName(Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
						.append(Component.text(damageToString(mDPSDisp, true), NamedTextColor.RED))
						.append(Component.text(" (", NamedTextColor.YELLOW))
						.append(Component.text(damageToString(mDPSDisp10s, true), NamedTextColor.GREEN))
						.append(Component.text("/", NamedTextColor.YELLOW))
						.append(Component.text(damageToString(mMaxDPS, true), NamedTextColor.GOLD))
						.append(Component.text(")", NamedTextColor.YELLOW)));

					mDPSCounter10s = -1;
				}, 200);
			}

			mDPSCounter += damage;
			mDPSCounter10s += damage;
		}

		if (mBoss.isValid() && !mBoss.isDead() && mBoss.getHealth() > 0) {
			mBoss.setHealth(EntityUtils.getAttributeOrDefault(mBoss, Attribute.GENERIC_MAX_HEALTH, 1000));
		}
	}

	private String damageToString(double damage) {
		return damageToString(damage, false);
	}

	private String damageToString(double damage, boolean isHolo) {
		String damageString;
		if (isHolo) {
			damageString = holoDigits.format(damage);
		} else {
			damageString = cutoffDigits.format(damage);
		}
		return damageString;
	}

	@Override
	public void unload() {
		super.unload();
		if (mHologram != null) {
			mHologram.remove();
			mHologram = null;
		}
		mDPSCounter = -1;
		mDPSCounter10s = -1;
		mDPSDisp = -1;
		mDPSDisp10s = -1;
		mMaxDPS = -1;
	}
}
