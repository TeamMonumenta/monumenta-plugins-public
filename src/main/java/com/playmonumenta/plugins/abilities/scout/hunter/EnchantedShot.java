package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Enchanted Arrow: Right click while looking the ground will
 * prime an enchanted arrow. When the next arrow is fired, this
 * arrow will instantaneously travel in a straight line for 30
 * blocks until hitting a block, piercing through all targets,
 * dealing 20 / 30 damage. (Cooldown: 20s)
 */
public class EnchantedShot extends Ability {

	private boolean active = false;

	public EnchantedShot(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EnchantedArrow";
		mInfo.linkedSpell = Spells.ENCHANTED_ARROW;
		mInfo.cooldown = 20 * 20;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean cast() {
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ENCHANTED_ARROW) && mPlayer.getLocation().getPitch() > 50) {
			Player player = mPlayer;
			active = true;
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.45f);
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;
					mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 4, 0.25, 0, 0.25, 0);
					if (!active || t >= 20 * 10) {
						active = false;
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (active) {
			arrow.remove();
			active = false;
			BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 0.65, 0.65, 0.65);
			double damage = getAbilityScore() == 1 ? 20 : 30;

			Player player = mPlayer;
			Location loc = player.getEyeLocation();
			Vector dir = loc.getDirection().normalize();
			player.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.85f);
			player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 0.65f);
			player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(dir), 10, 0.1, 0.1, 0.1, 0.2);

			List<Mob> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), 30);
			for (int i = 0; i < 30; i++) {
				box.shift(dir);
				Location bLoc = box.getCenter().toLocation(mWorld);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, bLoc, 5, 0.35, 0.35, 0.35, 0);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 2, 0.1, 0.1, 0.1, 0.1);
				for (Mob mob : mobs) {
					if (mob.getBoundingBox().overlaps(box)) {
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
					}
				}
				if (bLoc.getBlock().getType().isSolid()) {
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 150, 0.1, 0.1, 0.1, 0.2);
					player.getWorld().playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					break;
				}
			}
			return false;
		}
		return true;
	}


}
