package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.ScoutClass;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class RangerSpecialization extends BaseSpecialization {
	private World mWorld;

	public RangerSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	public static final String ARROW_QUICKSHOT_METAKEY = "ArrowQuickshotMetakey";
	public static final String PLAYER_DEADEYE_METAKEY = "PlayerDeadeyeMetakey";

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (InventoryUtils.isBowItem(itemInHand)) {
				int quickshot = ScoreboardUtils.getScoreboardValue(player, "Quickshot");
				/*
				 * Quickdraw: Left Clicking with a bow instantly fires an arrow
				 *  that deals 6 damage (+ any other bonuses from skills) and
				 *  inflicts Slowness 3 for 2 seconds (Cooldown: 12 seconds).
				 *  Level 2 decreases the cooldown to 10 seconds and increases
				 *  the arrow damage to 9 + effects.
				 */
				if (quickshot > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.QUICKSHOT)) {

						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.25f);
						mWorld.spawnParticle(Particle.CRIT, player.getEyeLocation().add(player.getLocation().getDirection()), 10, 0, 0, 0, 0.2f);
						Arrow arrow = player.launchProjectile(Arrow.class);
						double damage = quickshot == 1 ? 6 : 9;
						arrow.setMetadata(ARROW_QUICKSHOT_METAKEY, new FixedMetadataValue(mPlugin, damage));
						mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);

						int cooldown = quickshot == 1 ? 20 * 12 : 20 * 10;
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.QUICKSHOT, cooldown);
					}
				}
			} else {
				if (player.isSprinting()) {
					int deadeye = ScoreboardUtils.getScoreboardValue(player, "Deadeye");
					/*
					 * Deadeye: Sprint Attack to prime Deadeye, canceling any forward
					 * movement and taking a 3 block leap backwards. All charged shots
					 * made in the next 4/5 seconds, instead of shooting an arrow, deal
					 * 8/12 damage (plus any other bonuses from skills), inflict Glowing,
					 * and inflict Vulnerability 20% to the nearest enemy in front of you
					 * (within 15 blocks). At Level 2, you also gain Speed 2 duration the
					 * duration of Deadeye. (Cooldown: 20 Seconds from the end of ability
					 * duration) (Use beam/smoke particles from the player to the target
					 * pls if you can)
					 */
					if (deadeye > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.DEADEYE)) {
							player.setMetadata(PLAYER_DEADEYE_METAKEY, new FixedMetadataValue(mPlugin, null));
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 2);
							player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.5f);
							int duration = deadeye == 1 ? 20 * 4 : 20 * 5;
							if (deadeye > 1) {
								mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, duration, 1, true, false));
							}
							Location loc = player.getLocation();
							Vector dir = loc.getDirection();
							double z = dir.getZ() * 0.65;
							double x = dir.getX() * 0.65;
							player.setVelocity(new Vector(-x, 0.35, -z));
							new BukkitRunnable() {
								int t = 0;
								@Override
								public void run() {
									t++;
									mWorld.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 7, 0.25f, 0.1f, 0.25f, 0);

									if (t >= duration) {
										this.cancel();
										player.removeMetadata(PLAYER_DEADEYE_METAKEY, mPlugin);
									}
								}

							}.runTaskTimer(mPlugin, 0, 1);
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DEADEYE, (20 * 20) + duration);
						}
					}
				}
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if ((offHand.getType() == Material.SHIELD && mainHand.getType() != Material.BOW) ||
				    (mainHand.getType() == Material.SHIELD && offHand.getType() != Material.BOW)
				    && !blockClicked.isInteractable()) {
					int disengage = ScoreboardUtils.getScoreboardValue(player, "Disengage");
					/*
					 * Disengage: Sneak Block with a shield to leap backwards 6'ish
					 * blocks from your position, with a bit of vertical velocity
					 * as well. Enemies within melee range of you previous position
					 * are stunned for 3 seconds. (Cooldown: 15 seconds) At Level 2,
					 * you receive 7 seconds of Speed II on landing.
					 */
					if (disengage > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.DISENGAGE)) {
							for (Entity e : player.getNearbyEntities(3, 3, 3)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									EntityUtils.applyStun(mPlugin, 20 * 3, le);
								}
							}
							player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
							Location loc = player.getLocation();
							Vector dir = loc.getDirection();
							double z = dir.getZ() * 1.65;
							double x = dir.getX() * 1.65;
							player.setVelocity(new Vector(-x, 0.65, -z));
							mWorld.spawnParticle(Particle.CLOUD, loc, 15, 0.1f, 0, 0.1f, 0.125f);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0.1f, 0, 0.1f, 0.15f);
							if (disengage > 1) {
								new BukkitRunnable() {

									@Override
									public void run() {
										if (player.isOnGround() || player.getLocation().getBlock().isLiquid()) {
											this.cancel();
											mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 20 * 7, 1, true, false));
											player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.5f);
											mWorld.spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0, 0, 0, 0.175f);
										}
									}

								}.runTaskTimer(mPlugin, 5, 1);
							}
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DISENGAGE, 20 * 15);
						}
					}
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata(ARROW_QUICKSHOT_METAKEY)) {
			event.setCancelled(true);
			double damage = arrow.getMetadata(ARROW_QUICKSHOT_METAKEY).get(0).asDouble();
			EntityUtils.damageEntity(mPlugin, damagee, damage, player);
			damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 2, false, true));
		}
	}

	@Override
	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		if (player.hasMetadata(PLAYER_DEADEYE_METAKEY)) {
			LivingEntity entity = EntityUtils.getNearestHostile(player, 15);
			if (entity != null && arrow.isCritical()) {
				int deadeye = ScoreboardUtils.getScoreboardValue(player, "Deadeye");
				Vector dir = LocationUtils.getDirectionTo(entity.getEyeLocation(), player.getEyeLocation());
				Location loc = player.getEyeLocation();
				int extraDam = ScoutClass.getBowMasteryDamage(player);
				int dam = deadeye == 1 ? 8 : 12;

				player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.5f);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(dir), 10, 0, 0, 0, 0.15f);
				boolean hit = false;
				for (int i = 0; i < 20; i++) {
					loc.add(dir);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 15, 0.3f, 0.3f, 0.3f, 0.075f);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.2f, 0.2f, 0.2f, 0);
					for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
						if (EntityUtils.isHostileMob(e)) {
							LivingEntity le = (LivingEntity) e;
							EntityUtils.damageEntity(mPlugin, le, dam + extraDam, player);
							le.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 3, 3, false, true));
							le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 0, false, true));
							hit = true;
						}
					}
					if (hit) {
						mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.175f);
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 150, 0, 0, 0, 0.3f);
						loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.65f);
						break;
					}
				}

				return false;
			} else {
				return true;
			}
		}
		return true;
	}
}
