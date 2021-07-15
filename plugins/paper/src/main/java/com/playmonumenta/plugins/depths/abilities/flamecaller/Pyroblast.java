package com.playmonumenta.plugins.depths.abilities.flamecaller;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class Pyroblast extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyroblast";

	public static final int COOLDOWN = 12 * 20;
	public static final int[] DAMAGE = {20, 25, 30, 35, 40};
	private static final int PARTICLE_DIST = 16;
	private static final double RADIUS = 4.0;
	private static final int DURATION = 4 * 20;
	public static final String META_DATA_TAG = "PyroblastArrow";

	public Pyroblast(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TNT_MINECART;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mLinkedSpell = ClassAbility.PYROBLAST;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	public void execute() {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.4f);

		Arrow arrow = mPlayer.launchProjectile(Arrow.class);

		arrow.setPierceLevel(0);
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(2.0));
		arrow.setMetadata(META_DATA_TAG, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SOUL_FIRE_FLAME);
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.CAMPFIRE_SIGNAL_SMOKE);
		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				if (arrow == null || mT > COOLDOWN) {
					arrow.remove();

					this.cancel();
				}
				if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
					explode(arrow, arrow.getLocation());

					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (proj instanceof AbstractArrow && proj.hasMetadata(META_DATA_TAG)) {
			explode((AbstractArrow) proj, le.getLocation());
		}

		return true;
	}

	private void explode(AbstractArrow arrow, Location loc) {
		World world = arrow.getWorld();

		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < PARTICLE_DIST; i++) {
			loc.add(dir);

			if (loc.getBlock().getType().isSolid()) {
				world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 2, 2, 2, 0);
				world.spawnParticle(Particle.FLAME, loc, 25, 2, 2, 2, 0);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, .75f, 1);

				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, RADIUS);
				for (LivingEntity e : mobs) {
					EntityUtils.applyFire(mPlugin, DURATION, e, mPlayer);
					EntityUtils.damageEntity(mPlugin, e, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
				}
			}
		}
		loc = arrow.getLocation();
		world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0);
		world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 2, 2, 2, 0);
		world.spawnParticle(Particle.FLAME, loc, 25, 2, 2, 2, 0);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, .75f, 1);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, RADIUS);
		for (LivingEntity e : mobs) {
			EntityUtils.applyFire(mPlugin, DURATION, e, mPlayer);
			EntityUtils.damageEntity(mPlugin, e, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
		}
		arrow.remove();
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return true;
		}

		if (mPlayer.isSneaking()) {
			arrow.remove();
			putOnCooldown();
			execute();
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting a bow while sneaking fires an exploding arrow, which deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage within a " + RADIUS + " block radius of it and sets nearby mobs on fire for " + DURATION / 20 + " seconds upon impact. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}

