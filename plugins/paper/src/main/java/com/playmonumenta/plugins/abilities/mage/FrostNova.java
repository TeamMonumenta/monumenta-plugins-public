package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class FrostNova extends Ability {

	public static final String NAME = "Frost Nova";
	public static final ClassAbility ABILITY = ClassAbility.FROST_NOVA;

	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 10;
	public static final int SIZE = 7;
	public static final double SLOW_MULTIPLIER_1 = 0.2;
	public static final double SLOW_MULTIPLIER_2 = 0.4;
	public static final double ELITE_SLOW_MULTIPLIER_REDUCTION = 0.1;
	public static final double ENHANCED_DAMAGE_MODIFIER = 1.15;
	public static final int DURATION_TICKS = 4 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_FROZEN_DURATION = 1 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS = 18 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_COOLDOWN_TICKS = 16 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Frost Nova Damage";
	public static final String CHARM_COOLDOWN = "Frost Nova Cooldown";
	public static final String CHARM_RANGE = "Frost Nova Range";
	public static final String CHARM_SLOW = "Frost Nova Slowness Amplifier";
	public static final String CHARM_DURATION = "Frost Nova Slowness Duration";
	public static final String CHARM_FROZEN = "Frost Nova Frozen Duration";

	private final float mLevelDamage;
	private final double mLevelSlowMultiplier;

	public FrostNova(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "FrostNova";
		mInfo.mShorthandName = "FN";
		mInfo.mDescriptions.add(
			String.format(
				"While sneaking, left-clicking with a wand unleashes a frost nova," +
					" dealing %s ice magic damage to all enemies in a %s-block sphere around you," +
					" afflicting them with %s%% slowness for %ss, and extinguishing them if they're on fire." +
					" Slowness is reduced by %s%% on elites and bosses, and all players in the nova are also extinguished." +
					" The damage ignores iframes. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_1),
				StringUtils.ticksToSeconds(DURATION_TICKS),
				StringUtils.multiplierToPercentage(ELITE_SLOW_MULTIPLIER_REDUCTION),
				StringUtils.ticksToSeconds(COOLDOWN_TICKS)
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s. Base slowness is increased from %s%% to %s%%.",
				DAMAGE_1,
				DAMAGE_2,
				StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_1),
				StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_2)
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased by %s%% and cooldown is reduced to %ss. Non elites and bosses are frozen for %ss, having their AI and gravity removed.",
				StringUtils.multiplierToPercentage(ENHANCED_DAMAGE_MODIFIER - 1),
				ENHANCED_COOLDOWN_TICKS / 20,
				StringUtils.ticksToSeconds(ENHANCED_FROZEN_DURATION)
			)
		);
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, isEnhanced() ? ENHANCED_COOLDOWN_TICKS : COOLDOWN_TICKS);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.ICE, 1);

		int damage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isEnhanced() ? (int) (damage * ENHANCED_DAMAGE_MODIFIER) : damage);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		int duration = CharmManager.getExtraDuration(mPlayer, CHARM_DURATION) + DURATION_TICKS;
		int frozenDuration = CharmManager.getExtraDuration(mPlayer, CHARM_FROZEN) + ENHANCED_FROZEN_DURATION;
		double size = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), size);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				EntityUtils.applySlow(mPlugin, duration, mLevelSlowMultiplier - ELITE_SLOW_MULTIPLIER_REDUCTION, mob);
			} else {
				EntityUtils.applySlow(mPlugin, duration, mLevelSlowMultiplier, mob);
				if (isEnhanced()) {
					mob.setAI(false);
					mob.setGravity(false);
					new BukkitRunnable() {

						@Override
						public void run() {
							mob.setAI(true);
							mob.setGravity(true);
						}

					}.runTaskLater(mPlugin, frozenDuration);
				}
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, false);

			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), SIZE, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mPlayer.getLocation().add(0, 0.15, 0);

			@Override
			public void run() {
				mRadius += 1.25;

				new PPCircle(Particle.CLOUD, mLoc, mRadius).ringMode(true).count(20).extra(0.1).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.CRIT_MAGIC, mLoc, mRadius).ringMode(true).count(160).extra(0.65).spawnAsPlayerActive(mPlayer);

				if (mRadius >= size + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.45f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1f);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null
			&& ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())
			&& mPlayer.isSneaking();
	}
}
