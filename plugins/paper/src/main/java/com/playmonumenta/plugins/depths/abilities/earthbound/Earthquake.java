package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
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
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class Earthquake extends DepthsAbility {
	public static final String ABILITY_NAME = "Earthquake";
	public static final int COOLDOWN = 16 * 20;
	public static final int[] DAMAGE = {20, 25, 30, 35, 40};
	public static final int[] SILENCE_DURATION = {80, 90, 100, 110, 120};
	public static final int EARTHQUAKE_TIME = 20;
	public static final int RADIUS = 4;
	public static final double KNOCKBACK = 0.8;
	public static final int MAX_TICKS = 4 * 20;
	public static final String META_DATA_TAG = "EarthquakeArrow";

	public Earthquake(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.COARSE_DIRT;
		mTree = DepthsTree.EARTHBOUND;
		mInfo.mLinkedSpell = ClassAbility.EARTHQUAKE;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	public void execute() {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);

		Arrow arrow = mPlayer.launchProjectile(Arrow.class);

		arrow.setPierceLevel(0);
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(2.0));
		arrow.setMetadata(META_DATA_TAG, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.LAVA);
		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				if (arrow == null || mT > MAX_TICKS) {
					arrow.remove();

					this.cancel();
				}

				if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
					quake(arrow, arrow.getLocation());

					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (proj instanceof AbstractArrow && proj.hasMetadata(META_DATA_TAG)) {
			quake((AbstractArrow) proj, le.getLocation());
		}

		return true;
	}

	public void quake(AbstractArrow arrow, Location loc) {
		World world = mPlayer.getWorld();
		arrow.remove();

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks >= EARTHQUAKE_TIME) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS)) {
						if (!mob.getName().contains("Dionaea")) {
							knockup(mob);
						}
						EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, null, true, mInfo.mLinkedSpell, true, true, false, true);
						if (!EntityUtils.isBoss(mob)) {
							EntityUtils.applySilence(mPlugin, SILENCE_DURATION[mRarity - 1], mob);
						}
					}
					for (Player player : PlayerUtils.playersInRange(loc, RADIUS, true)) {
						knockup(player);
					}
					for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
						if (entity != null && entity instanceof IronGolem) {
							knockup((IronGolem) entity);
						}
					}

					world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, RADIUS / 2, 0.1, RADIUS / 2, 0.1);
					world.spawnParticle(Particle.LAVA, loc, 20, RADIUS / 2, 0.3, RADIUS / 2, 0.1);
					world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 3, 1.0f);
					world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1, 1.0f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.0f);
					this.cancel();
				} else {
					world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.PODZOL));
					world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.GRANITE));
					world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.IRON_ORE));
					world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);
					world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.75f, 0.5f);
				}

				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void knockup(LivingEntity le) {
		le.setVelocity(le.getVelocity().add(new Vector(0.0, KNOCKBACK, 0.0)));
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
		return "Shooting a bow while sneaking causes an earthquake " + EARTHQUAKE_TIME / 20 + " seconds after impact. The earthquake deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage to mobs in a " + RADIUS + " block radius, silencing for " + DepthsUtils.getRarityColor(rarity) + (float)SILENCE_DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds and knocking upward. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}
