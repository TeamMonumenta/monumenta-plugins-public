package com.playmonumenta.bossfights.bosses;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.utils.Utils;

public class WinterSnowmanEventBoss extends BossAbilityGroup {
	public static final String deathMetakey = "PLAYER_SNOWMAN_DEATH_METAKEY";
	public static final String identityTag = "boss_winter_snowman";
	public static final int detectionRange = 50;

	private final LivingEntity mBoss;
	private final Plugin mPlugin;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WinterSnowmanEventBoss(plugin, boss);
	}

	public WinterSnowmanEventBoss(Plugin plugin, LivingEntity boss) throws Exception {
		mBoss = boss;
		mPlugin = plugin;
		mBoss.setRemoveWhenFarAway(false);

		if (!(boss instanceof Snowman)) {
			throw new Exception("boss_winter_snowman only works on snowmen!");
		}

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();

		if (damager != null && damager instanceof Player && ((Player)damager).getGameMode().equals(GameMode.CREATIVE)) {
			// This event happens like normal
			return;
		}

		// If hit by a snowball thrown by a player, damage the snowman by 1 HP after this tick is over
		if (damager instanceof Snowball && (((Snowball)damager).getShooter() instanceof Player)) {
			Location loc = mBoss.getLocation();
			loc.getWorld().playSound(loc, Sound.BLOCK_CORAL_BLOCK_BREAK, SoundCategory.HOSTILE, 2, 0);
			loc.getWorld().spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.1);
			event.setDamage(1);
			return;
		}

		// Snowmen can not be damaged by default
		event.setCancelled(true);
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player) {
			Player player = (Player)event.getHitEntity();
			if ((player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) && !player.isDead() && player.getHealth() > 0) {
				float absorp = Utils.getAbsorp(player);
				absorp -= 2;
				Utils.setAbsorp(player, absorp);

				Location loc = player.getLocation().add(0, 1.4, 0);
				loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 2, 2);
				loc.getWorld().spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.1);
				if (absorp <= 0) {
					if (mBoss.getCustomName() != null) {
						player.setMetadata(deathMetakey, new FixedMetadataValue(mPlugin, mBoss.getCustomName()));
					}
					player.setHealth(0);
				}
			}
		}
	};

	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}

	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}
}
