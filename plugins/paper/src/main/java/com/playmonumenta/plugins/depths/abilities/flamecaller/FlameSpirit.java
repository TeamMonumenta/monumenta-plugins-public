package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class FlameSpirit extends DepthsAbility {
	public static final String ABILITY_NAME = "Flame Spirit";
	public static final double[] DAMAGE = {2, 2.5, 3, 3.5, 4, 5.5};
	public static final int COOLDOWN = 12 * 20;
	public static final int DURATION = 6 * 20;
	public static final int FIRE_TICKS = 2 * 20;
	public static final int RADIUS = 4;
	public static final String CHARM_COOLDOWN = "Flame Spirit Cooldown";

	public static final DepthsAbilityInfo<FlameSpirit> INFO =
		new DepthsAbilityInfo<>(FlameSpirit.class, ABILITY_NAME, FlameSpirit::new, DepthsTree.FLAMECALLER, DepthsTrigger.WILDCARD)
			.linkedSpell(ClassAbility.FLAME_SPIRIT)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SOUL_CAMPFIRE)
			.descriptions(FlameSpirit::getDescription);

	private final double mDamage;
	private final double mRadius;
	private final int mFireDuration;
	private final int mDuration;

	private int mLastTick;
	private int mEnemiesDamaged;
	private @Nullable Location mClosestLocation;

	public FlameSpirit(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.FLAME_SPIRIT_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(player, CharmEffects.FLAME_SPIRIT_RADIUS.mEffectName, RADIUS);
		mFireDuration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_FIRE_DURATION.mEffectName, FIRE_TICKS);
		mDuration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_DURATION.mEffectName, DURATION);
		mLastTick = 0;
		mEnemiesDamaged = 0;
	}

	public void summonSpirit() {
		if (mClosestLocation == null) {
			return;
		}
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		Location loc = mClosestLocation.clone();
		cancelOnDeath(new BukkitRunnable() {
			int mTickCount = 0;

			@Override
			public void run() {
				if (mTickCount >= mDuration) {
					this.cancel();
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
					EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer, playerItemStats);
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.FLAME_SPIRIT, playerItemStats), mDamage, true, false, false);
				}

				cancelOnDeath(new BukkitRunnable() {
					double mVerticalAngle = 0;
					double mRotationAngle = 0;
					int mTicksElapsed = 0;

					@Override
					public void run() {
						if (mTicksElapsed >= 20) {
							this.cancel();
						}

						mVerticalAngle += 5.5;
						mRotationAngle += 20;
						mVerticalAngle %= 360;
						mRotationAngle %= 360;

						new PartialParticle(
							Particle.SOUL_FIRE_FLAME,
							loc.add(
								FastUtils.cos(Math.toRadians(mRotationAngle)) * 2,
								FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
								FastUtils.sin(Math.toRadians(mRotationAngle)) * 2
							), 1, 0, 0.01
						).spawnAsPlayerActive(mPlayer);

						new PartialParticle(
							Particle.SOUL_FIRE_FLAME,
							loc.add(
								FastUtils.cos(Math.toRadians(mRotationAngle)) * -2,
								FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
								FastUtils.sin(Math.toRadians(mRotationAngle)) * -2
							), 1, 0, 0.01
						).spawnAsPlayerActive(mPlayer);

						mTicksElapsed++;
					}
				}.runTaskTimer(mPlugin, 0, 1));
				mTickCount += 20;
			}
		}.runTaskTimer(mPlugin, 0, 20));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (isOnCooldown()) {
			return false;
		}
		int currentTick = Bukkit.getCurrentTick();
		if (currentTick != mLastTick) {
			mLastTick = currentTick;
			mEnemiesDamaged = 0;
			mClosestLocation = null;
		}
		mEnemiesDamaged++;
		Location playerLoc = mPlayer.getLocation();
		Location enemyLoc = enemy.getLocation();
		if (mClosestLocation == null || playerLoc.distanceSquared(enemyLoc) < playerLoc.distanceSquared(mClosestLocation)) {
			mClosestLocation = enemyLoc;
		}
		if (mEnemiesDamaged == 3) {
			putOnCooldown();
			summonSpirit();
			mEnemiesDamaged = 0;
		}
		return false;
	}

	private static Description<FlameSpirit> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Damaging 3 or more enemies at once summons a spirit that deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius every second for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and sets affected mobs on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}
