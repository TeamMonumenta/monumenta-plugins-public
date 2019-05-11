package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;


/*
 * Withering Gaze: Sprinting and left clicking unleashes a cone of
 * magic in the direction the player faces that freezes all enemies
 * in it’s path (bosses are given slowness 3) and gives wither III
 * for 6/10 seconds. 30/20 second cooldown.
 */
public class WitheringGaze extends Ability {

	public WitheringGaze(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "WitheringGaze";
		mInfo.linkedSpell = Spells.WITHERING_GAZE;
		mInfo.cooldown = getAbilityScore() == 1 ? 20 * 30 : 20 * 20;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast() {
		Player player = mPlayer;
		Location loc = player.getLocation().add(0, 0.65, 0); // the Y height is higher so that the skill doesn't get stomped by halfslabs
		Vector direction = loc.getDirection().setY(0).normalize();
		int duration = getAbilityScore() == 1 ? 20 * 6 : 20 * 10;
		player.getLocation().getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1f, 0.4f);
		player.getLocation().getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1f, 1f);
		new BukkitRunnable() {
			double t = 0;
			double damagerange = 1.15;
			double r = 1;
			@Override
			public void run() {

				t += 1;
				Vector vec;
				for (double degree = 0; degree < 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
					vec = VectorUtils.rotateXAxis(vec, 0);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					mWorld.spawnParticle(Particle.SPELL_WITCH, l, 3, 0.15, 0.15, 0.15, 0.15);
					mWorld.spawnParticle(Particle.SPELL_MOB, l, 3, 0.15, 0.15, 0.15, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, l, 2, 0.15, 0.15, 0.15, 0.05);
				}
				r += 0.55;

				for (Entity e : player.getNearbyEntities(damagerange, damagerange * 2, damagerange)) {
					if (EntityUtils.isHostileMob(e)) {
						Vector eVec = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
						if (direction.dot(eVec) > 0.4) {
							LivingEntity le = (LivingEntity) e;
							if (EntityUtils.isBoss(le) || ((e instanceof Player) && AbilityManager.getManager().isPvPEnabled((Player)e))) {
								PotionUtils.applyPotion(player, le, new PotionEffect(PotionEffectType.SLOW, duration, 2));
							} else {
								if (!EntityUtils.isFrozen(le)) {
									EntityUtils.applyFreeze(mPlugin, duration, le);
								}
							}
							PotionUtils.applyPotion(player, le, new PotionEffect(PotionEffectType.WITHER, duration, 2));
						}
					}
				}

				damagerange += 1;
				loc.add(direction.clone().multiply(0.75));
				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
				}

				if (t >= 9) {
					this.cancel();

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && InventoryUtils.isScytheItem(mHand);
	}

}
