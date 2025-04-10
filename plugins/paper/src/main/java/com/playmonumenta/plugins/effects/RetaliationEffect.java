package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class RetaliationEffect extends Effect {
	public static final String effectID = "RetaliationEffect";
	private static final String DAMAGE_BONUS_SOURCE = "RetaliationDamage";
	private static final EnumSet<DamageType> VALID_HIT_DAMAGE_TYPES = DamageType.getScalableDamageType();

	private static final int BURNING_DURATION = 10 * 20;
	private static final double BURNING_VULN = 0.15;
	private static final String BURNING_SOURCE = "RetaliationBurningAttack";
	private static final int SLOWING_DURATION = 10 * 20;
	private static final double SLOWING_SLOW = 0.15;
	private static final String SLOWING_SOURCE = "RetaliationSlowingAttack";
	private static final int SICKENING_DURATION = 5 * 20;
	private static final double SICKENING_DAMAGE_PER_10T = 5;
	private static final String SICKENING_SOURCE = "RetaliationSickeningAttack";
	private static final int HUNGERING_DURATION = 10 * 20;
	private static final double HUNGERING_WEAKEN = 0.15;
	private static final String HUNGERING_SOURCE = "RetaliationHungeringAttack";
	private static final int ANTIHEAL_DURATION = 10 * 20;
	private static final double ANTIHEAL_STRENGTH = 1;
	private static final String ANTIHEAL_SOURCE = "RetaliationAntihealAttack";
	private static final int SILENCE_DURATION = 3 * 20;
	private static final String SILENCE_SOURCE = "RetaliationSilencingAttack";

	private final double mAmount;
	private String mDebuffTypes;
	private final String mIcons;
	private boolean mHasDamagedThisTick = false;

	public RetaliationEffect(int duration, double amount, String debuffTypes, String icons) {
		super(duration, effectID);
		mAmount = amount;
		mDebuffTypes = debuffTypes;
		mIcons = icons;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.playSound(player, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.HOSTILE, 1.5f, 0.6f);
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		switch (mDebuffTypes) {
			case "fire" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.RED));
			case "CustomSlow", "slowness" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.GRAY));
			case "wither", "poison" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.DARK_GRAY));
			case "hunger" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.DARK_GREEN));
			case "CustomAntiHeal" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.DARK_RED));
			case "silence" -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.WHITE));
			case "" -> entity.sendActionBar(Component.text("Retaliation primed!", NamedTextColor.GOLD));
			default -> entity.sendActionBar(Component.text("Retaliation primed! " + mIcons, NamedTextColor.DARK_PURPLE));
		}
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		Plugin plugin = Plugin.getInstance();
		if (VALID_HIT_DAMAGE_TYPES.contains(event.getType()) && entity instanceof Player player && !mHasDamagedThisTick) {
			if (!mDebuffTypes.isEmpty()) {
				player.playSound(enemy.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 1f, 1f);

				if (mDebuffTypes.contains("fire")) {
					plugin.mEffectManager.addEffect(enemy, BURNING_SOURCE, new PercentDamageReceived(BURNING_DURATION, BURNING_VULN));
					EntityUtils.applyFire(plugin, BURNING_DURATION, enemy, player);
				}
				if (mDebuffTypes.contains("CustomSlow") || mDebuffTypes.contains("slowness")) {
					plugin.mEffectManager.addEffect(enemy, SLOWING_SOURCE, new PercentSpeed(SLOWING_DURATION, -SLOWING_SLOW, "RetaliationSlowing"));
				}
				if (mDebuffTypes.contains("wither") || mDebuffTypes.contains("poison")) {
					plugin.mEffectManager.addEffect(enemy, SICKENING_SOURCE, new CustomDamageOverTime(SICKENING_DURATION, SICKENING_DAMAGE_PER_10T, 10, null, null));
					plugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.POISON);
					plugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.WITHER);
				}
				if (mDebuffTypes.contains("hunger")) {
					plugin.mEffectManager.addEffect(enemy, HUNGERING_SOURCE, new PercentDamageDealt(HUNGERING_DURATION, -HUNGERING_WEAKEN));
				}
				if (mDebuffTypes.contains("CustomAntiHeal")) {
					plugin.mEffectManager.addEffect(enemy, ANTIHEAL_SOURCE, new PercentHeal(ANTIHEAL_DURATION, -ANTIHEAL_STRENGTH));
				}
				if (mDebuffTypes.contains("silence")) {
					plugin.mEffectManager.addEffect(enemy, SILENCE_SOURCE, new AbilitySilence(SILENCE_DURATION));
				}
			}

			plugin.mEffectManager.addEffect(enemy, DAMAGE_BONUS_SOURCE, new PercentDamageReceived(1, mAmount, VALID_HIT_DAMAGE_TYPES) {
				@Override
				public void onHurt(LivingEntity entity2, DamageEvent event2) {
					// The damage bonus effect must only work for the player applying it
					if (VALID_HIT_DAMAGE_TYPES.contains(event2.getType()) && event2.getDamager() == entity)	{
					event2.updateDamageWithMultiplier(1 + mAmount);
						player.playSound(entity2.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.25f, 1f);
					}
				}
			});
			mDebuffTypes = "";

			mHasDamagedThisTick = true;
			Bukkit.getScheduler().runTask(plugin, () -> mHasDamagedThisTick = false);
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
		return "+" + (int) (100 * mAmount) + "% Retaliation" + (mDebuffTypes.isEmpty() ? "" : " " + mIcons);
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}
}
