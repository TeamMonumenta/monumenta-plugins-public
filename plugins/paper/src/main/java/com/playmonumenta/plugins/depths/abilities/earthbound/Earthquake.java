package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.WeakHashMap;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Earthquake extends DepthsAbility {
	public static final String ABILITY_NAME = "Earthquake";
	public static final int COOLDOWN = 16 * 20;
	public static final int[] DAMAGE = {20, 25, 30, 35, 40, 50};
	public static final int[] SILENCE_DURATION = {80, 90, 100, 110, 120, 140};
	public static final int EARTHQUAKE_TIME = 20;
	public static final double RADIUS = 4;
	public static final double KNOCKBACK = 0.8;

	public static final String CHARM_COOLDOWN = "Earthquake Cooldown";

	public static final DepthsAbilityInfo<Earthquake> INFO =
		new DepthsAbilityInfo<>(Earthquake.class, ABILITY_NAME, Earthquake::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.EARTHQUAKE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.COARSE_DIRT)
			.descriptions(Earthquake::getDescription)
			.priorityAmount(949); // Needs to trigger before Rapid Fire

	private final double mDamage;
	private final int mSilenceDuration;
	private final double mRadius;

	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public Earthquake(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.EARTHQUAKE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CharmEffects.EARTHQUAKE_SILENCE_DURATION.mEffectName, SILENCE_DURATION[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.EARTHQUAKE_RADIUS.mEffectName, RADIUS);
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(damager)) {
			quake(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls by removing the arrow (from the world and the player stats map)
	}

	// Since Snowballs disappear after landing, we need an extra detection for when it hits the ground.
	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && mPlayerItemStatsMap.containsKey(proj)) {
			quake(proj, proj.getLocation());
		}
	}

	private void quake(Projectile projectile, Location loc) {
		World world = mPlayer.getWorld();

		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(projectile);
		if (playerItemStats != null) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks >= EARTHQUAKE_TIME) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
							if (!EntityUtils.isCCImmuneMob(mob)) {
								knockup(mob);
							}

							DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, false, true, false);

							if (!EntityUtils.isBoss(mob)) {
								EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
							}
						}

						for (Player player : PlayerUtils.playersInRange(loc, mRadius, true)) {
							knockup(player);
						}

						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, mRadius / 2, 0.1, mRadius / 2, 0.1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.LAVA, loc, 20, mRadius / 2, 0.3, mRadius / 2, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 3, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1.0f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 1.0f);
						this.cancel();
					} else {
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, mRadius / 2, 0.25, mRadius / 2, 0.1, Bukkit.createBlockData(Material.PODZOL)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, mRadius / 2, 0.25, mRadius / 2, 0.1, Bukkit.createBlockData(Material.GRANITE)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, mRadius / 2, 0.25, mRadius / 2, 0.1, Bukkit.createBlockData(Material.IRON_ORE)).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 2, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.75f, 0.5f);
					}

					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		projectile.remove();
	}

	private void knockup(LivingEntity le) {
		double knockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.EARTHQUAKE_KNOCKBACK.mEffectName, KNOCKBACK);
		le.setVelocity(le.getVelocity().add(new Vector(0.0, knockback, 0.0)));
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			    || !mPlayer.isSneaking()
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown(getModifiedCooldown((int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer))));
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 2, 1.0f);

		if (projectile instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		mPlayerItemStatsMap.put(projectile, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.LAVA);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(projectile)) {
					projectile.remove();

					this.cancel();
				}

				if (projectile.getVelocity().length() < .05 || projectile.isOnGround()) {
					quake(projectile, projectile.getLocation());

					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private static Description<Earthquake> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Earthquake>(color)
			.add("Shooting a projectile while sneaking causes an earthquake ")
			.addDuration(a -> EARTHQUAKE_TIME, EARTHQUAKE_TIME)
			.add(" second after impact. The earthquake deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius, silencing for ")
			.addDuration(a -> a.mSilenceDuration, SILENCE_DURATION[rarity - 1], false, true)
			.add(" seconds and knocking upward.")
			.addCooldown(COOLDOWN);
	}


}
