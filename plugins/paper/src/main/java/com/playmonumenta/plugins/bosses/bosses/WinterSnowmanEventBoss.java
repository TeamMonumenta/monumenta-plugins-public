package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import java.util.Collections;
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
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class WinterSnowmanEventBoss extends BossAbilityGroup {
	public static final String deathMetakey = "PLAYER_SNOWMAN_DEATH_METAKEY";
	public static final String identityTag = "boss_winter_snowman";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WinterSnowmanEventBoss(plugin, boss);
	}

	public WinterSnowmanEventBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);

		if (!(boss instanceof Snowman)) {
			throw new Exception("boss_winter_snowman only works on snowmen!");
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (damager instanceof Player player && player.getGameMode().equals(GameMode.CREATIVE)) {
			// This event happens like normal
			return;
		}

		// If hit by a snowball thrown by a player, damage the snowman by 1 HP after this tick is over
		if (damager instanceof Snowball && source instanceof Player) {
			Location loc = mBoss.getLocation();
			loc.getWorld().playSound(loc, Sound.BLOCK_CORAL_BLOCK_BREAK, SoundCategory.HOSTILE, 2, 0);
			loc.getWorld().spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.1);
			event.setDamage(1);
			return;
		}

		// Snowmen can not be damaged by default
		event.setCancelled(true);
	}

	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player) {
			Player player = (Player)event.getHitEntity();
			if ((player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) && !player.isDead() && player.getHealth() > 0) {
				AbsorptionUtils.subtractAbsorption(player, 2);

				Location loc = player.getLocation().add(0, 1.4, 0);
				loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 2, 2);
				loc.getWorld().spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.1);
				if (AbsorptionUtils.getAbsorption(player) <= 0) {
					if (mBoss.getCustomName() != null) {
						player.setMetadata(deathMetakey, new FixedMetadataValue(mPlugin, mBoss.getUniqueId().toString()));
					}
					player.setHealth(0);
				}
			}
		}
	}

	@Override
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}

	@Override
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		event.getAffectedEntities().remove(mBoss);
	}
}
