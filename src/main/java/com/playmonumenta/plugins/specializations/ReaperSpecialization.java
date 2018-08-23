package com.playmonumenta.plugins.specializations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect.BlockData;;

public class ReaperSpecialization extends BaseSpecialization {

	public ReaperSpecialization(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!player.isSprinting() && !player.isSneaking() && InventoryUtils.isScytheItem(itemInHand)) {
				int darkEruption = ScoreboardUtils.getScoreboardValue(player, "DarkEruption");
				/*
				 * Dark Eruption: Right Clicking without sprinting or sneaking
				 * you slams the scythe into the ground, causing a massive
				 * eruption to surge forward in the players direction
				 * in 10 blocks. Any enemy it passes by is dealt 12/15
				 * damage and applies Withering II/III for 5 seconds.
				 * At level 2, when the ability is triggered, all enemies
				 * within 3 blocks of the player is dealt 10 damage and
				 * knocked back.
				 */
				if (darkEruption > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.DARK_ERUPTION)) {

						if (darkEruption > 1) {
							ParticleEffect.SPELL_WITCH.display(3, 1, 3, 1, 250, player.getLocation(), 40);
							ParticleEffect.SMOKE_LARGE.display(3, 1, 3, 0, 100, player.getLocation(), 40);
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1.3f);
							for (Entity e : player.getNearbyEntities(3, 2, 3)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									EntityUtils.damageEntity(mPlugin, le, 10, player);
									MovementUtils.KnockAway(player, le, 0.8f);
								}
							}
						}

						double dmg = darkEruption == 1 ? 12 : 15;
						int amp = darkEruption == 1 ? 1 : 2;
						new BukkitRunnable() {
							Location loc = player.getLocation().add(0, 0.5, 0);
							Vector direction = loc.getDirection().normalize().setY(0);

							int t = 0;
							@Override
							public void run() {
								t++;
								loc.add(direction);
								if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
									if (!loc.subtract(0, 1, 0).clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
										this.cancel();
									}
								}
								if (loc.getBlock().getType().isSolid()) {
									if (loc.add(0, 1, 0).getBlock().getType().isSolid()) {
										this.cancel();
									}
								}
								Material mat = loc.clone().subtract(0, 1, 0).getBlock().getType();
								byte data = loc.clone().subtract(0, 1, 0).getBlock().getData();
								if (mat == Material.AIR || mat == Material.BARRIER) {
									mat = Material.DIRT;
								}
								ParticleEffect.BLOCK_CRACK.display(new BlockData(mat, data), 0.25f, 0.25f, 0.25f, 1, 15, loc, 40);
								ParticleEffect.SPELL_WITCH.display(0.25f, 0.25f, 0.25f, 1, 25, loc, 40);
								ParticleEffect.SMOKE_NORMAL.display(0.25f, 0.25f, 0.25f, 0.075f, 10, loc, 40);
								loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, 0.85f, 0.85f);

								for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.85, 0.85, 0.85)) {
									if (EntityUtils.isHostileMob(e)) {
										LivingEntity le = (LivingEntity) e;
										EntityUtils.damageEntity(mPlugin, le, dmg, player);
										le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, amp, true, false));
									}
								}
								if (t >= 14) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DARK_ERUPTION, 20 * 14);
					}
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSprinting() && InventoryUtils.isScytheItem(itemInHand)) {
				int petrifyingGlare = ScoreboardUtils.getScoreboardValue(player, "PetrifyingGlare");
				/*
				 * Petrifying Glare: Sprinting and left clicking unleashes a cone
				 * of magic in the direction the player faces that freezes all enemies
				 * in it��s path (bosses are given slowness 3) and gives vulnerability
				 * (15/20%) for 5/10 seconds. 40/30 second cooldown.
				 */
				if (petrifyingGlare > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PETRIFYING_GLARE)) {

						int duration = petrifyingGlare == 1 ? 20 * 5 : 20 * 10;
						int vulnAmp = petrifyingGlare == 1 ? 2 : 3;

						new BukkitRunnable() {
							double t = 0;
							float xoffset = 0.00F;
							float zoffset = 0.00F;
							double damagerange = 0.75;
							Location loc = player.getLocation();
							Vector direction = player.getLocation().getDirection().normalize();
							List<Entity> affected = new ArrayList<Entity>();
							public void run() {

								t = t + 0.5;
								xoffset += 0.15F;
								zoffset += 0.15F;
								damagerange += 0.25;
								double x = direction.getX() * t;
								double y = direction.getY() + 0.5;
								double z = direction.getZ() * t;
								player.getLocation().getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.15f);
								loc.add(x, y, z);
								ParticleEffect.SPELL.display(xoffset, 0.25F, zoffset, 0, 45, loc, 40);
								ParticleEffect.SPELL_WITCH.display(xoffset, 0.25F, zoffset, 0.05F, 50, loc, 40);

								for (Entity e : loc.getWorld().getNearbyEntities(loc, damagerange, 1.25, damagerange)) {
									if (EntityUtils.isHostileMob(e) && !affected.contains(e)) {
										LivingEntity le = (LivingEntity) e;
										EntityUtils.applyFreeze(mPlugin, 20 * 5, le);
										le.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, duration, vulnAmp, true, false));
										affected.add(le);
									}
								}
								loc.subtract(x, y, z);


								if (t > 7.5) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
						int cooldown = petrifyingGlare == 1 ? 20 * 40 : 20 * 30;

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PETRIFYING_GLARE, cooldown);
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerDamagedByPlayerEvent(Player player, Player damagee) {
		if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
			int soulreaping = ScoreboardUtils.getScoreboardValue(player, "Soulreaping");
			/*
			 * Soulreaping: Critically Striking an enemy will give it
			 * slowness 4 for 4 seconds, and deal 6/10 damage after 2
			 * seconds. Alternatively, critically striking a player
			 * will heal them for 40/50% of their missing health.
			 * (Cooldown: 10 seconds)
			 */
			if (soulreaping > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SOULREAPING)) {
					if (PlayerUtils.isCritical(player)) {
						double hp = damagee.getMaxHealth() - damagee.getHealth();
						double percent = soulreaping == 1 ? 0.4 : 0.5;
						double healed = hp * percent;
						PlayerUtils.healPlayer(damagee, healed);
						damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1, 0.75f);
						ParticleEffect.SPELL_WITCH.display(0.25f, 0.35f, 0.25f, 0, 20, damagee.getLocation().add(0, 1.15, 0), 40);
						ParticleEffect.SPELL_MOB.display(0.25f, 0.35f, 0.25f, 0, 15, damagee.getLocation().add(0, 1.15, 0), 40);
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SOULREAPING, 20 * 10);
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		LivingEntity e = (LivingEntity) event.getEntity();
		if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
			int soulreaping = ScoreboardUtils.getScoreboardValue(player, "Soulreaping");
			/*
			 * Soulreaping: Critically Striking an enemy will give it
			 * slowness 4 for 4 seconds, and deal 6/10 damage after 2
			 * seconds. Alternatively, critically striking a player
			 * will heal them for 20/30% of their missing health.
			 * (Cooldown: 10 seconds)
			 */
			if (soulreaping > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SOULREAPING)) {
					if (EntityUtils.isHostileMob(e)) {
						if (PlayerUtils.isCritical(player)) {
							e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 4, 3, true, false));
							double dmg = soulreaping == 1 ? 6 : 10;
							new BukkitRunnable() {
								int t = 0;
								@Override
								public void run() {
									t++;
									ParticleEffect.SPELL_WITCH.display(0.25f, 0.35f, 0.25f, 0, 1, e.getLocation().add(0, 1.15, 0), 40);
									ParticleEffect.SPELL_MOB.display(0.25f, 0.35f, 0.25f, 0, 1, e.getLocation().add(0, 1.15, 0), 40);
									if (t >= 20 * 2 || e.isDead()) {
										this.cancel();
										List<Entity> entities = e.getNearbyEntities(2, 2, 2);
										entities.add(e);
										ParticleEffect.SMOKE_LARGE.display(0, 0, 0, 0.15f, 15, e.getLocation().add(0, 1.15, 0), 40);
										ParticleEffect.SPELL_WITCH.display(2f, 0.35f, 2f, 1, 35, e.getLocation().add(0, 1.15, 0), 40);
										ParticleEffect.SPELL_MOB.display(2f, 0.35f, 2f, 0, 30, e.getLocation().add(0, 1.15, 0), 40);
										ParticleEffect.SMOKE_LARGE.display(2f, 0.35f, 2f, 0, 8, e.getLocation().add(0, 1.15, 0), 40);
										e.getWorld().playSound(e.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.5f);
										for (Entity en : entities) {
											if (EntityUtils.isHostileMob(en)) {
												LivingEntity le = (LivingEntity) en;
												EntityUtils.damageEntity(mPlugin, le, dmg, player);
											}
										}
									}

								}

							}.runTaskTimer(mPlugin, 0, 1);
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SOULREAPING, 20 * 10);
						}
					}
				}
			}
		}
		return true;
	}

	//Leaving this here in case we ever need it again - Fire

//	@Override
//	public void EntityDeathEvent(Player player, EntityDeathEvent event) {
//		LivingEntity e = event.getEntity();
//		if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
//			int soulreaping = ScoreboardUtils.getScoreboardValue(player, "Soulreaping");
//			/*
//			 * Soulreaping: Whenever the user kills an enemy, they receive 15%/30%
//			 * of the final damage dealt as health. If the ability is at level 2 and is
//			 * an elite enemy, all nearby enemies within 5 blocks also take 6 damage.
//			 */
//
//			if (soulreaping > 0) {
//				double lifesteal = soulreaping == 1 ? 0.15 : 0.3;
//				EntityDamageEvent ev = e.getLastDamageCause();
//				if (ev.getCause() == DamageCause.ENTITY_ATTACK) {
//					PlayerUtils.healPlayer(player, ev.getFinalDamage() * lifesteal);
//					ParticleEffect.SPELL_WITCH.display(0.2f, 0.35f, 0.2f, 1, 4, player.getLocation().add(0, 1.15, 0), 40);
//					if (soulreaping > 1) {
//						if (EntityUtils.isElite(e)) {
//							ParticleEffect.SPELL_WITCH.display(5, 5, 5, 1, 500, e.getLocation(), 40);
//							ParticleEffect.SPELL_MOB.display(5, 5, 5, 0, 500, e.getLocation(), 40);
//							e.getWorld().playSound(e.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.25f, 0.15f);
//							for (Entity ne : e.getNearbyEntities(5, 5, 5)) {
//								if (EntityUtils.isHostileMob(ne)) {
//									LivingEntity le = (LivingEntity) ne;
//									EntityUtils.damageEntity(mPlugin, le, 6, player);
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//	}

}
