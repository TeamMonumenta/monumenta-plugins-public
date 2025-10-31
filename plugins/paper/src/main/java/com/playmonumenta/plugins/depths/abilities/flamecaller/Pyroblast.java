package com.playmonumenta.plugins.depths.abilities.flamecaller;

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
import java.util.List;
import java.util.WeakHashMap;
import net.kyori.adventure.text.format.TextColor;
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

public class Pyroblast extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyroblast";

	public static final int COOLDOWN = 12 * 20;
	public static final int[] DAMAGE = {20, 24, 28, 32, 36, 44};
	private static final int RADIUS = 4;
	private static final int DURATION = 4 * 20;

	public static final String CHARM_COOLDOWN = "Pyroblast Cooldown";

	public static final DepthsAbilityInfo<Pyroblast> INFO =
		new DepthsAbilityInfo<>(Pyroblast.class, ABILITY_NAME, Pyroblast::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.PYROBLAST)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.TNT_MINECART)
			.descriptions(Pyroblast::getDescription)
			.priorityAmount(949); // Needs to trigger before Rapid Fire

	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	private final double mRadius;
	private final double mDamage;
	private final int mFireDuration;

	public Pyroblast(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.PYROBLAST_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PYROBLAST_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.PYROBLAST_FIRE_DURATION.mEffectName, DURATION);
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(damager)) {
			explode(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls itself by removing the arrow
	}

	// Since Snowballs disappear after landing, we need an extra detection for when it hits the ground.
	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && mPlayerItemStatsMap.containsKey(proj)) {
			explode(proj, proj.getLocation());
		}
	}

	private void explode(Projectile proj, Location loc) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		if (playerItemStats != null) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);
			for (LivingEntity mob : mobs) {
				EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, true, false);
			}
			World world = proj.getWorld();
			double mult = mRadius / RADIUS;
			new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, (int) (40 * mult), 2 * mult, 2 * mult, 2 * mult, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FLAME, loc, (int) (40 * mult), 2 * mult, 2 * mult, 2 * mult, 0).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1);
			proj.remove();
		}
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			|| !mPlayer.isSneaking()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.4f);

		if (projectile instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		mPlayerItemStatsMap.put(projectile, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.SOUL_FIRE_FLAME);
		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.CAMPFIRE_SIGNAL_SMOKE);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(projectile)) {
					projectile.remove();

					this.cancel();
				}
				if (projectile.getVelocity().length() < .05 || projectile.isOnGround()) {
					explode(projectile, projectile.getLocation());

					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	private static Description<Pyroblast> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Shooting a projectile while sneaking fires an exploding projectile, which deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks upon impact. Affected mobs are set on fire for ")
			.addDuration(a -> a.mFireDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}

