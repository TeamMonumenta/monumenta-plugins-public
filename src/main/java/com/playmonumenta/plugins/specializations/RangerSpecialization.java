package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
				 * Quickdraw: Left Clicking with a bow instantly
				 * fires an arrow that deals 9 damage (+ any other
				 * bonuses from skills) and inflicts Slowness 3
				 * for 2 seconds (Cooldown: 12 seconds). Level 2
				 * decreases the cooldown to 10 seconds and increases
				 * the arrow damage to 12 + effects.
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
					 * Disengage: Sneak right click (without a bow) to leap backwards
					 * 6 ish blocks from your position, with a bit of vertical velocity
					 *  as well. Enemies within melee range of you previous position
					 *  are stunned for 2 seconds(does not work on ebm,lites and bosses).
					 *  (Cooldown: 12 seconds) At Level 2, you deal 8 damage
					 */
					if (disengage > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.DISENGAGE)) {
							for (Entity e : player.getNearbyEntities(3, 3, 3)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									EntityUtils.applyStun(mPlugin, 20 * 3, le);
									if (disengage > 1) {
										EntityUtils.damageEntity(mPlugin, le, 8, player);
									}
								}
							}
							player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
							player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.2f);
							Location loc = player.getLocation();
							Vector dir = loc.getDirection();
							double z = dir.getZ() * 1.65;
							double x = dir.getX() * 1.65;
							player.setVelocity(new Vector(-x, 0.65, -z));
							mWorld.spawnParticle(Particle.CLOUD, loc, 15, 0.1f, 0, 0.1f, 0.125f);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0.1f, 0, 0.1f, 0.15f);
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DISENGAGE, 20 * 12);
						}
					}
				}
			} else if (player.isSprinting()) {
				int precisionStrike = ScoreboardUtils.getScoreboardValue(player, "PrecisionStrike");
				/*
				 * Precision Strike: A fast dash that stops at the first
				 * enemy hit, dealing 6 damage and applying 20% vulnerability
				 * for 2 seconds, this also stuns the enemy hit for 1 second.
				 * At level 2 the damage increases to 10 and the vulnerability
				 * increases to 30%. (CD: 6 seconds)
				 */
				if (precisionStrike > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PRECISION_STRIKE)) {
						Location loc = player.getLocation();
						Vector dir = loc.getDirection();
						dir.setY(dir.getY() * 0.5);
						dir.add(new Vector(0, 0.35, 0));
						int level = precisionStrike == 1 ? 3 : 5;
						int dmg = precisionStrike == 1 ? 6 : 10;
						player.setVelocity(dir);
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 63, 0.25, 0.1, 0.25, 0.2);
						mWorld.spawnParticle(Particle.CLOUD, loc, 20, 0.25, 0.1, 0.25, 0.125);
						mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 2);
						mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.7f);
						new BukkitRunnable() {

							@Override
							public void run() {
								for (LivingEntity e : EntityUtils.getNearbyMobs(loc, 0.75)) {
									EntityUtils.damageEntity(mPlugin, e, dmg, player);
									e.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 2, level, false, true));
									EntityUtils.applyStun(mPlugin, 20, e);
									break;
								}
								if (player.isOnGround()) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 1, 1);
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PRECISION_STRIKE, 20 * 6);
					}
				}
			}
		}
	}

}
