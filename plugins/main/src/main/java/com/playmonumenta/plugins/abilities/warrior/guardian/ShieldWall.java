package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Shield Wall: Blocking and then blocking again within 0.25s
 * Creates a 180 degree arc of particles with a height of 5 blocks
 * and width of 4 blocks in front of the user, blocking all enemy
 * projectiles and dealing 6 damage to enemies who pass through the
 * wall. The shield lasts 8/10 seconds. At level 2, this shield knocks
 * back enemies as well. (Ghast fireballs explode on the wall)
 * Cooldown: 30/20 seconds
 */
public class ShieldWall extends Ability {

	private static final int SHIELD_WALL_1_DURATION = 8 * 30;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 6;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 20;

	public ShieldWall(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "ShieldWall";
		mInfo.cooldown = getAbilityScore() == 1 ? SHIELD_WALL_1_COOLDOWN : SHIELD_WALL_2_COOLDOWN;
		mInfo.linkedSpell = Spells.SHIELD_WALL;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	private boolean mPrimed = false;
	@Override
	public boolean cast() {
		if (mPrimed) {
			return false;
		}

		int time = getAbilityScore() == 1 ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION;
		boolean knockback = getAbilityScore() == 1 ? false : true;
		new BukkitRunnable() {
			int t = 0;
			boolean active = false;
			Location loc = mPlayer.getLocation();
			List<BoundingBox> boxes = new ArrayList<BoundingBox>();
			boolean hitboxes = false;

			@Override
			public void run() {
				t++;

				if (!mPlayer.isHandRaised() && !mPlayer.isBlocking() && !active) {
					mPrimed = true;
				}

				if (mPrimed && !active) {
					if (mPlayer.isHandRaised() || mPlayer.isBlocking()) {
						active = true;
						mPrimed = false;
						t = 0;
						mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1.5f);
						mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.8f);
						mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 70, 0, 0, 0, 0.3f);
						putOnCooldown();
					}
				}
				if (active) {
					Vector vec;
					for (int y = 0; y < 5; y++) {
						for (double degree = 0; degree < 180; degree += 10) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(Math.cos(radian1) * 4, y, Math.sin(radian1) * 4);
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							Location l = loc.clone().add(vec);
							mWorld.spawnParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0);
							if (!hitboxes) {
								boxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								                         l.clone().add(0.6, 5, 0.6)));
							}
						}
						hitboxes = true;
					}

					for (BoundingBox box : boxes) {
						for (Entity e : mWorld.getNearbyEntities(box)) {
							Location eLoc = e.getLocation();
							if (e instanceof Projectile) {
								Projectile proj = (Projectile) e;
								if (proj.getShooter() instanceof LivingEntity) {
									LivingEntity shooter = (LivingEntity) proj.getShooter();
									if (!(shooter instanceof Player) || AbilityManager.getManager().isPvPEnabled((Player)shooter)) {
										proj.remove();
										mWorld.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 5, 0, 0, 0, 0.25f);
										mWorld.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.5f);
									}
								}
							} else if (EntityUtils.isHostileMob(e)) {
								LivingEntity le = (LivingEntity) e;
								EntityUtils.damageEntity(mPlugin, le, SHIELD_WALL_DAMAGE, mPlayer);
								if (knockback) {
									MovementUtils.KnockAway(loc, le, 0.3f);
									mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, eLoc, 50, 0, 0, 0, 0.35f);
									mWorld.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
								}
							}
						}
					}
				}
				if (t >= time) {
					this.cancel();
					boxes.clear();
				}

				if (t > 5 && !active) {
					this.cancel();
					mPrimed = false;
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mHand.getType() == Material.SHIELD || oHand.getType() == Material.SHIELD;
	}

}
