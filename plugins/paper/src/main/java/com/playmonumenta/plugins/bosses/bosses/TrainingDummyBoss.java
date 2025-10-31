package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class TrainingDummyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_training_dummy";
	public static final int detectionRange = 25;
	private static final DecimalFormat cutoffDigits = new DecimalFormat("0.0####"); // number of 0s/#s determines maximum digits shown
	private static final DecimalFormat holoDigits = new DecimalFormat("0.0"); // number of 0s/#s determines maximum digits shown

	public static @Nullable DamageEvent.DamageType mNextTrueDamageReplacement = null;

	private final Component HOLOGRAM_DEFAULT_NAME = Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
		.append(Component.text("???", NamedTextColor.DARK_AQUA))
		.append(Component.text(" (", NamedTextColor.YELLOW))
		.append(Component.text("???", NamedTextColor.DARK_AQUA))
		.append(Component.text("/", NamedTextColor.YELLOW))
		.append(Component.text("???", NamedTextColor.DARK_AQUA))
		.append(Component.text(")", NamedTextColor.YELLOW));

	private @Nullable ArmorStand mHologram = null;

	private boolean mSumMode = false;
	private double mMaxDPS = -1;
	private double mTotalDamage;
	private final ArrayDeque<DamageInstance> mDamageInstances = new ArrayDeque<>();

	private record DamageInstance(int mTick, double mDamage) {
	}

	public static class Parameters extends BossParameters {
		public boolean REGEN = true;
	}

	private final boolean mRegen;

	public TrainingDummyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mRegen = p.REGEN;

		SpellManager activeSpells = SpellManager.EMPTY;
		if (mRegen) {
			activeSpells = new SpellManager(List.of(
				new SpellRunAction(() -> boss.setHealth(EntityUtils.getMaxHealth(boss)), 60 * 20)));
		}

		List<Spell> passiveSpells = List.of(
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

		if (source instanceof Player player) {
			String damageString = damageToString(damage);

			DamageEvent.DamageType type = event.getType();
			ClassAbility ability = event.getAbility();
			if (mNextTrueDamageReplacement != null && type == DamageEvent.DamageType.TRUE && (ability == null || !ability.isFake())) {
				type = mNextTrueDamageReplacement;
			}
			mNextTrueDamageReplacement = null;

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
				mHologram.setRemoveWhenFarAway(true);
				EntityUtils.setRemoveEntityOnUnload(mHologram);
			}

			int currentTick = Bukkit.getCurrentTick();

			if (type == DamageEvent.DamageType.MELEE && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
				if (!player.isSneaking()) {
					List<Component> effects = DisplayableEffect.getSortedEffectDisplayComponents(com.playmonumenta.plugins.Plugin.getInstance(), mBoss);
					Component effectHover = MessagingUtils.concatenateComponents(effects);
					player.sendMessage(Component.text("Your punch clears the training dummy of all its status effects.", NamedTextColor.AQUA).hoverEvent(effectHover));
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss);
				} else {
					mSumMode = !mSumMode;
					mTotalDamage = 0;
					mMaxDPS = 0;
					player.sendMessage(Component.text("The training dummy now shows " + (mSumMode ? "sum of damage taken" : "damage taken per second") + ".", NamedTextColor.AQUA));
				}
			} else {
				Component hoverDamage = Component.text("Damage: " + damageString + "\n");
				Component hoverType = Component.text("Type: " + type.getDisplay() + "\n");
				Component hoverAbility = ability == null ? Component.empty() : Component.text("Source: " + ability.getName() + "\n");
				Component hoverMob = mBoss.name().append(Component.newline());
				Component hoverTimestamp = Component.text("Timestamp: " + Bukkit.getCurrentTick());
				Component hover = hoverDamage.append(hoverType).append(hoverAbility).append(hoverMob).append(hoverTimestamp);

				player.sendMessage(Component.text("Damage: ", NamedTextColor.GOLD)
					.append(Component.text(damageString + " " + getTypeSymbol(type), NamedTextColor.RED))
					.hoverEvent(HoverEvent.showText(hover)));

				mDamageInstances.add(new DamageInstance(currentTick, damage));
				mTotalDamage += damage;
				mDamageInstances.removeIf(di -> di.mTick + 10 * 20 <= currentTick);
			}

			if (mSumMode) {
				double damage10s = mDamageInstances.stream().mapToDouble(di -> di.mDamage).sum();
				if (mHologram != null) {
					mHologram.customName(Component.text("Damage 10s / Total: ", NamedTextColor.YELLOW)
						.append(Component.text(damageToString(damage10s, true), NamedTextColor.RED))
						.append(Component.text(" / ", NamedTextColor.YELLOW))
						.append(Component.text(damageToString(mTotalDamage, true), NamedTextColor.GOLD)));
				}
			} else {
				double dps10s = mDamageInstances.stream().mapToDouble(di -> di.mDamage).sum() / 10;
				double dps = mDamageInstances.stream().filter(di -> di.mTick + 20 > currentTick).mapToDouble(di -> di.mDamage).sum();

				if (mMaxDPS < dps) {
					mMaxDPS = dps;
				}
				if (mHologram != null) {
					mHologram.customName(Component.text("DPS (10s / Max): ", NamedTextColor.YELLOW)
						.append(Component.text(damageToString(dps, true), NamedTextColor.RED))
						.append(Component.text(" (", NamedTextColor.YELLOW))
						.append(Component.text(damageToString(dps10s, true), NamedTextColor.GREEN))
						.append(Component.text("/", NamedTextColor.YELLOW))
						.append(Component.text(damageToString(mMaxDPS, true), NamedTextColor.GOLD))
						.append(Component.text(")", NamedTextColor.YELLOW)));
				}
			}

		}

		if (mRegen && mBoss.isValid() && !mBoss.isDead() && mBoss.getHealth() > 0) {
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

	private static String getTypeSymbol(DamageEvent.DamageType type) {
		return switch (type) {
			case MELEE, MELEE_SKILL, MELEE_ENCH -> "🗡";
			case PROJECTILE, PROJECTILE_SKILL, PROJECTILE_ENCH -> "🏹";
			case MAGIC -> "⭐";
			case AILMENT -> "☠";
			default -> "";
		};
	}


	@Override
	public void unload() {
		super.unload();
		ArmorStand hologram = mHologram;
		if (hologram != null && hologram.isValid() && mPlugin.isEnabled()) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (hologram.isValid()) {
					hologram.remove();
				}
			});
		}
		mHologram = null;
		mDamageInstances.clear();
		mMaxDPS = -1;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null) {
			event.setDroppedExp(0);
		}
	}
}
