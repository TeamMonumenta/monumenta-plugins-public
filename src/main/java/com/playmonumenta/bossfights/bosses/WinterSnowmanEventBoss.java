package com.playmonumenta.bossfights.bosses;

import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.Plugin;

import net.minecraft.server.v1_13_R2.EntityLiving;

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
			new BukkitRunnable() {
				@Override
				public void run() {
					mBoss.setHealth(Math.max(0, mBoss.getHealth() - 1));
				}
			}.runTaskLater(mPlugin, 0);
			return;
		}

		// Snowmen can not be damaged by default
		event.setCancelled(true);
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player) {
			Player player = (Player)event.getHitEntity();
			if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) {
				EntityLiving handle = ((CraftPlayer)player).getHandle();
				float absorb = handle.getAbsorptionHearts();
				if (absorb > 0) {
					absorb -= 2;
					handle.setAbsorptionHearts(absorb);
					if (absorb <= 0) {
						if (mBoss.getCustomName() != null) {
							player.setMetadata(deathMetakey, new FixedMetadataValue(mPlugin, mBoss.getCustomName()));
						}
						player.setHealth(0);
					}
				}
			}
		}

	};
}
