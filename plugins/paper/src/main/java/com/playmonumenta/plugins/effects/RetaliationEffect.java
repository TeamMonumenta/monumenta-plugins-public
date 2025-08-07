package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Retaliation;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class RetaliationEffect extends Effect {
	public static final String effectID = "RetaliationEffect";
	private static final EnumSet<DamageType> SOUND_AND_DEBUFF_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE, DamageType.PROJECTILE);
	private static final EnumSet<DamageType> HALVED_BONUS_DAMAGE_TYPES = DamageEvent.DamageType.getAllProjectileAndMagicTypes();

	private static final int DOT_DURATION = 5 * 20;
	private static final double DOT_DAMAGE_PER_10T = 5;
	private static final String DOT_SOURCE = "RetaliationDot";

	private static final int WEAK_DURATION = 5 * 20;
	private static final double WEAK_POTENCY = 0.2;
	private static final String WEAK_SOURCE = "RetaliationWeak";

	private static final int SLOW_DURATION = 5 * 20;
	private static final double SLOW_POTENCY = 0.2;
	private static final String SLOW_SOURCE = "RetaliationSlow";

	private final double mAmount;
	private String mDebuffTypes;

	public RetaliationEffect(int duration, double amount, String debuffTypes) {
		super(duration, effectID);
		mAmount = amount;
		mDebuffTypes = debuffTypes;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.playSound(player, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.HOSTILE, 1.5f, 0.6f);
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		NamedTextColor color = NamedTextColor.GRAY;
		if (mAmount == Retaliation.BOSS_DAMAGE) {
			color = NamedTextColor.RED;
		} else if (mAmount == Retaliation.ELITE_DAMAGE) {
			color = NamedTextColor.GOLD;
		}

		if (mDebuffTypes.isEmpty()) {
			entity.sendActionBar(Component.text("Retaliation primed!", color));
			return;
		}
		Component icons = Component.text("");
		if (mDebuffTypes.contains(Retaliation.DOT_NAME)) {
			icons = icons.append(Component.text(Retaliation.DOT_NAME, TextColor.color(60, 40, 50)));
		}
		if (mDebuffTypes.contains(Retaliation.WEAK_NAME)) {
			icons = icons.append(Component.text(Retaliation.WEAK_NAME, TextColor.color(150, 50, 75)));
		}
		if (mDebuffTypes.contains(Retaliation.SLOW_NAME)) {
			icons = icons.append(Component.text(Retaliation.SLOW_NAME, TextColor.color(75, 85, 100)));
		}

		entity.sendActionBar(Component.text("Retaliation primed! ", color).append(Component.text("[", NamedTextColor.GRAY)).append(icons).append(Component.text("]", NamedTextColor.GRAY)));
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (!DamageType.getScalableDamageType().contains(event.getType())) {
			return;
		}
		event.updateGearDamageWithMultiplier(1 + (HALVED_BONUS_DAMAGE_TYPES.contains(event.getType()) ? mAmount * 0.5 : mAmount));

		Plugin plugin = Plugin.getInstance();
		if (SOUND_AND_DEBUFF_DAMAGE_TYPES.contains(event.getType()) && entity instanceof Player player) {
			player.playSound(entity.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1f, 1f);
			if (!mDebuffTypes.isEmpty()) {
				player.playSound(enemy.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 1.35f, 1f);

				if (mDebuffTypes.contains(Retaliation.DOT_NAME)) {
					plugin.mEffectManager.addEffect(enemy, DOT_SOURCE, new CustomDamageOverTime(DOT_DURATION, DOT_DAMAGE_PER_10T, 10, null, null));
					player.setFireTicks(0);
					plugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.POISON);
					plugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.WITHER);
				}
				if (mDebuffTypes.contains(Retaliation.WEAK_NAME)) {
					plugin.mEffectManager.addEffect(enemy, WEAK_SOURCE, new PercentDamageDealt(WEAK_DURATION, -WEAK_POTENCY));
				}
				if (mDebuffTypes.contains(Retaliation.SLOW_NAME)) {
					plugin.mEffectManager.addEffect(enemy, SLOW_SOURCE, new PercentSpeed(SLOW_DURATION, -SLOW_POTENCY, SLOW_SOURCE));
				}
				mDebuffTypes = "";
			}
		}
	}

	@Override
	public String toString() {
		return String.format("RetaliationEffect duration:%d", this.getDuration());
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "+" + (int) (100 * mAmount) + "% Retaliation" + (mDebuffTypes.isEmpty() ? "" : " [" + mDebuffTypes + "]");
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}
}
