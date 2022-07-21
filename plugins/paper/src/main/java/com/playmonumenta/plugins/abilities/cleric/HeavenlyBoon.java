package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {

	private static final double HEAVENLY_BOON_1_CHANCE = 0.08;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.16;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0.05;
	private static final int HEAVENLY_BOON_1_DURATION = 20 * 20;
	private static final int HEAVENLY_BOON_2_DURATION = 50 * 20;

	private final KillTriggeredAbilityTracker mTracker;
	private final double mChance;
	private final int mDuration;

	private @Nullable Crusade mCrusade;

	public HeavenlyBoon(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Heavenly Boon");
		mInfo.mScoreboardId = "HeavenlyBoon";
		mInfo.mShorthandName = "HB";
		mInfo.mDescriptions.add("Whenever you are hit with a positive splash potion, the effects are also given to other players in a 12 block radius. In addition, whenever you kill an undead mob or deal damage to a boss (R1 100/R2 200), you have a 8% chance to be splashed with an Instant Health I potion, as well as either a Speed I, Regen I, or Absorption I potion with 20 second duration.");
		mInfo.mDescriptions.add("The chance to be splashed upon killing an Undead increases to 16%, the effect potions can now also be Strength and Resistance, and the durations of each are increased to 50 seconds.");
		mTracker = new KillTriggeredAbilityTracker(this);
		mChance = getAbilityScore() == 1 ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE;
		mDuration = getAbilityScore() == 1 ? HEAVENLY_BOON_1_DURATION : HEAVENLY_BOON_2_DURATION;
		mDisplayItem = new ItemStack(Material.SPLASH_POTION, 1);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion,
	                                           PotionSplashEvent event) {
		if (mPlayer == null) {
			return false;
		}

		if (!(potion.getShooter() instanceof Player)) {
			return true;
		}

		boolean hasPositiveEffects = PotionUtils.hasPositiveEffects(PotionUtils.getEffects(potion.getItem()));
		if (PotionUtils.hasNegativeEffects(potion.getItem()) && !hasPositiveEffects) {
			return true;
		}

		if (event.isCancelled()) {
			//Another cleric already spread this potion
			return false;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), HEAVENLY_BOON_RADIUS, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				/* Apply full-strength effects to players within range */
				for (PotionEffect effect : PotionUtils.getEffects(potion.getItem())) {
					PotionUtils.applyPotion(mPlugin, p, effect);
				}

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
			return false;
		}

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		mTracker.updateDamageDealtToBosses(event);
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (shouldGenDrops) {
			triggerOnKill(event.getEntity());
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (
			mPlayer != null
				&& Crusade.enemyTriggersAbilities(mob, mCrusade)
				&& FastUtils.RANDOM.nextDouble() < mChance
		) {
			ItemStack potions;

			if (getAbilityScore() == 1) {
				int rand = FastUtils.RANDOM.nextInt(4);
				if (rand == 0 || rand == 1) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, mDuration, 0,
					                                         "Splash Potion of Regeneration");
				} else if (rand == 2) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, mDuration, 0,
					                                         "Splash Potion of Absorption");
				} else {
					potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, mDuration, 0,
					                                         "Splash Potion of Speed");
				}
			} else {
				int rand = FastUtils.RANDOM.nextInt(5);
				if (rand == 0) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, mDuration, 0,
					                                         "Splash Potion of Regeneration");
				} else if (rand == 1) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, mDuration, 0,
					                                         "Splash Potion of Absorption");
				} else if (rand == 2) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, mDuration, 0,
					                                         "Splash Potion of Speed");
				} else if (rand == 3) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.INCREASE_DAMAGE, 1, mDuration, 0,
					                                         "Splash Potion of Strength");
				} else {
					potions = ItemUtils.createStackedPotions(PotionEffectType.DAMAGE_RESISTANCE, 1, mDuration, 0,
					                                         "Splash Potion of Resistance");
				}
			}

			ItemUtils.addPotionEffect(potions, PotionInfo.HEALING);

			Location pos = mPlayer.getLocation().add(0, 1, 0);
			EntityUtils.spawnCustomSplashPotion(mPlayer, potions, pos);
		}
	}
}
