package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DodgingCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.EntityListener;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Dodging extends Ability {

	/*
	 * This skill is a freaking nightmare because it spans two different events.
	 *
	 * Because of the way the ability system works, one event triggers (which puts
	 * it on cooldown), then the other event is missed because the skill is on
	 * cooldown...
	 */

	/*
	 * Debug findings:
	 * ProjectileHitEvent occurs before EntityDamageByEntityEvent
	 * Tipped Arrows apply after the ProjectileHitEvent is called, meaning we can remove their effects there
	 */

	protected static final int DODGING_SPEED_EFFECT_DURATION = 15 * 20;
	protected static final double PERCENT_SPEED = 0.2;
	protected static final String ATTR_NAME = "DodgingExtraSpeed";
	protected static final int DODGING_COOLDOWN_1 = 12 * 20;
	protected static final int DODGING_COOLDOWN_2 = 10 * 20;
	protected static final double MAGIC_DODGING_COOLDOWN_MULTIPLIER = 2.5;

	public static final String CHARM_COOLDOWN = "Dodging Cooldown";
	public static final String CHARM_SPEED = "Dodging Speed Amplifier";
	public static final String CHARM_DURATION = "Dodging Speed Duration";
	public static final String CHARM_MAGIC_DODGING_COOLDOWN = "Magic Dodging Cooldown";

	public static final AbilityInfo<Dodging> INFO =
		new AbilityInfo<>(Dodging.class, "Dodging", Dodging::new)
			.linkedSpell(ClassAbility.DODGING)
			.scoreboardId("Dodging")
			.shorthandName("Dg")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Dodge a projectile that would otherwise hit you.")
			.cooldown(DODGING_COOLDOWN_1, DODGING_COOLDOWN_2, CHARM_COOLDOWN)
			.displayItem(Material.SHIELD);

	private final double mSpeed;
	private final int mDuration;

	private final DodgingCS mCosmetic;
	private int mTriggerTick = 0;

	public Dodging(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpeed = PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DODGING_SPEED_EFFECT_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new DodgingCS());
	}

	@Override
	public boolean playerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		// Don't proc on Fire Aspect
		if (!(event.getCombuster() instanceof Projectile)) {
			return true;
		}

		// don't proc if the player has fire resistance.
		if (mPlayer.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
			return true;
		}

		// See if we should dodge. If false, allow the event to proceed normally
		if (!dodge()) {
			return true;
		}
		event.setDuration(0);
		return false;
	}


	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (event.getType() == DamageType.PROJECTILE && !event.isBlocked() && dodge()) {
			mPlayer.setNoDamageTicks(20);
			mPlayer.setLastDamage(event.getDamage());
			event.setFlatDamage(0);
			event.setCancelled(true);
		}
	}

	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		// See if we should dodge. If false, allow the event to proceed normally
		if (proj.getShooter() instanceof Player) {
			return true;
		}
		if (mPlayer.getActiveItem().getType() == Material.SHIELD) {
			return true;
		}
		if (proj instanceof Fireball && mPlayer.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
			return true;
		}
		if (!dodge()) {
			return true;
		}

		// Dodging Active.
		if (proj instanceof Arrow arrow) {
			EntityListener.removePotionEffectsFromArrow(arrow);
		}
		proj.remove();
		return true;
	}

	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		// If potion is thrown by a player or a non-living entity (trap), let it continue.
		if (!(event.getEntity().getShooter() instanceof LivingEntity enemy) || enemy instanceof Player) {
			return true;
		}
		return !dodge();
	}

	private boolean dodge() {
		if (mTriggerTick == Bukkit.getServer().getCurrentTick()) {
			// Dodging was activated this tick - allow it
			return true;
		}

		if (isOnCooldown()) {
			/*
			 * This ability is actually on cooldown (and was not triggered this tick)
			 * Don't process dodging
			 */
			return false;
		}

		/*
		 * Make note of which tick this triggered on so that any other event that triggers this
		 * tick will also be dodged
		 */
		mTriggerTick = Bukkit.getServer().getCurrentTick();
		putOnCooldown();

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, ATTR_NAME, new PercentSpeed(mDuration, mSpeed, ATTR_NAME).deleteOnAbilityUpdate(true));

			mCosmetic.dodgeEffectLv2(mPlayer, world, loc);
		}
		mCosmetic.dodgeEffect(mPlayer, world, loc);
		return true;
	}

	private static Description<Dodging> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Block an arrow, thrown potion, blaze fireball, or snowball that would have hit you.")
			.addCooldown(DODGING_COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<Dodging> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When this ability is triggered, you gain ")
			.addPercent(a -> a.mSpeed, PERCENT_SPEED)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, DODGING_SPEED_EFFECT_DURATION)
			.add(" seconds.")
			.addCooldown(DODGING_COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<Dodging> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain a secondary independent cooldown to block a magic attack that would otherwise have hit you. The cooldown is ")
			.addPercent(MAGIC_DODGING_COOLDOWN_MULTIPLIER)
			.add(" of your cooldown for Dodging.");
	}
}
