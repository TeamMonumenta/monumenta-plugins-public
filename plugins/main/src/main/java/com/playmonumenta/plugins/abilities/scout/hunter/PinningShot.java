package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/*
 * Pinning Shot: The first arrow shot at a non-boss mob
 * pins it for 5 seconds, rendering it immobile. Shooting
 * a pinned enemy removes the pin. At level 2, pin
 * duration is increased to 10 seconds and shooting a
 * pinned enemy increases arrow damage by 30% (stacks
 * with vulnerability).
 */
public class PinningShot extends Ability {

	private static final double PINNING_SHOT_DAMAGE_MULTIPLIER = 1.3;
	public PinningShot(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "PinningShot";
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (damagee.hasMetadata("PinningShotEnemyIsPinned")) {
			damagee.removeMetadata("PinningShotEnemyIsPinned", mPlugin);
			damagee.removePotionEffect(PotionEffectType.SLOW);
			mWorld.playSound(damagee.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, arrow.getLocation(), 8, 0, 0, 0, 0.2);
			if (getAbilityScore() > 1) {
				event.setDamage(event.getDamage() * PINNING_SHOT_DAMAGE_MULTIPLIER);
			}
		}
		if (!damagee.hasMetadata("PinningShotEnemyHasBeenPinned")) {
			damagee.setMetadata("PinningShotEnemyHasBeenPinned", new FixedMetadataValue(mPlugin, null));
			damagee.setMetadata("PinningShotEnemyIsPinned", new FixedMetadataValue(mPlugin, null));
			mWorld.playSound(damagee.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, arrow.getLocation(), 20, 0, 0, 0, 0.2);
			mWorld.spawnParticle(Particle.SNOWBALL, arrow.getLocation(), 30, 0, 0, 0, 0.25);

			int duration = getAbilityScore() == 1 ? 20 * 5 : 20 * 10;
			if (damagee instanceof Player) {
				duration *= 0.5;
			}
			damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 6));

			new BukkitRunnable() {
				@Override
				public void run() {
					if (damagee.hasMetadata("PinningShotEnemyIsPinned")) {
						damagee.removeMetadata("PinningShotEnemyIsPinned", mPlugin);
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, duration);
		}
		return true;
	}

}
