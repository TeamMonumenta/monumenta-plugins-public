package com.playmonumenta.plugins.abilities.scout.hunter;

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
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Pinning Shot: The first arrow shot at a non-boss mob
 * pins it for 5 seconds, rendering it immobile. Shooting
 * a pinned enemy removes the pin. At level 2, pin
 * duration is increased to 10 seconds and shooting a
 * pinned enemy increases the base arrow damage by 50%.
 */
public class PinningShot extends Ability {

	private static final double PINNING_SHOT_DAMAGE_MULTIPLIER = 1.3;

	public PinningShot(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Pinning Shot");
		mInfo.scoreboardId = "PinningShot";
		mInfo.mShorthandName = "PSh";
		mInfo.mDescriptions.add("When you shoot a non-boss enemy with a bow you automatically inflict Slowness 7 for 5s. Shooting a pinned enemy removes the slowness. This can only affect each enemy once.");
		mInfo.mDescriptions.add("Slowness 7 is applied for 10s instead. In addition shooting a pinned enemy increases arrow damage by 50%.");
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		// Thw metadata for a pinned enemy is removed in the ScoutPassive class where damage is calculated
		if (!damagee.hasMetadata("PinningShotEnemyHasBeenPinned")) {
			damagee.setMetadata("PinningShotEnemyHasBeenPinned", new FixedMetadataValue(mPlugin, mPlayer.getTicksLived()));
			// For some reason setting the multiplier value for the metadata here isn't working when I tested it using values
			// instead of null, messier workaround is to just have the public method return the Pinning Shot damage multiplier
			double multiplier = getAbilityScore() == 1 ? 1 : PINNING_SHOT_DAMAGE_MULTIPLIER;
			damagee.setMetadata("PinningShotEnemyIsPinned", new FixedMetadataValue(mPlugin, multiplier));
			mWorld.playSound(damagee.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1, 0.5f);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, arrow.getLocation(), 8, 0, 0, 0, 0.2);

			int duration = getAbilityScore() == 1 ? 20 * 5 : 20 * 10;
			if (damagee instanceof Player) {
				duration *= 0.5;
			}
			PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.SLOW, duration, 6));

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
