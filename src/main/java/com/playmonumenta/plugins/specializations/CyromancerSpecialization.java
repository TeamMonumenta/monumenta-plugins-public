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
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;

public class CyromancerSpecialization extends BaseSpecialization {

	public CyromancerSpecialization(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (player.isSneaking()) {
			ItemStack offHand = player.getInventory().getItemInOffHand();
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				if ((mainHand.getType() != Material.BOW) ||
				    (offHand.getType() != Material.BOW)
				    && !blockClicked.isInteractable()) {

					int blizzard = ScoreboardUtils.getScoreboardValue(player, "Blizzard");
					/*
					 * Blizzard: Shift Right click with a shield to
					 * cast a powerful AoE which deals 15/22 damage
					 * to enemies and freezes them. (Cooldown: 22 seconds)
					 */
					if (blizzard > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BLIZZARD)) {
							ParticleEffect.EXPLOSION_NORMAL.display(7, 1, 7, 0.2f, 1000, player.getLocation(), 40);
							ParticleEffect.SNOW_SHOVEL.display(7, 4, 7, 0.35f, 1500, player.getLocation(), 40);
							ParticleEffect.CLOUD.display(0, 0, 0, 0.5f, 300, player.getLocation(), 40);
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
							player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.85f, 1.15f);
							double damage = blizzard == 1 ? 15 : 22;
							for (Entity e : player.getNearbyEntities(7, 4, 7)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									EntityUtils.damageEntity(mPlugin, le, damage, player);
									if (!EntityUtils.isBoss(le)) {
										EntityUtils.applyFreeze(mPlugin, 20 * 5, le);
									} else {
										le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 2, false, true));
									}
								}
							}
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BLIZZARD, 20 * 22);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		LivingEntity e = (LivingEntity) event.getEntity();
		if (player.isSprinting()) {
			int coldWave = ScoreboardUtils.getScoreboardValue(player, "ColdWave");
			/*
			 * Cold Wave: Sprint Hitting a monster releases an ice wave
			 * behind them. Enemies hit by the wave are dealt 6/10 damage.
			 * Enemies who are not frozen are debuffed with frozen for 3
			 * seconds. Enemies who are frozen take an additional 5 damage
			 * and are unfrozen. (cooldown 10s)
			 */

			if (coldWave > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.COLD_WAVE)) {
					double dmg = coldWave == 1 ? 6 : 10;
					new BukkitRunnable() {
						double t = 0;
						float xoffset = 0.00F;
						float zoffset = 0.00F;
						double damagerange = 0.75;
						Location loc = e.getLocation();
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
							player.getLocation().getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 1);
							loc.add(x, y, z);
							ParticleEffect.CLOUD.display(xoffset, 0.25F, zoffset, 0.075F, 10, loc, 40);
							ParticleEffect.SNOW_SHOVEL.display(xoffset, 0.25F, zoffset, 0.05F, 50, loc, 40);

							for (Entity e : loc.getWorld().getNearbyEntities(loc, damagerange, 1.25, damagerange)) {
								if (EntityUtils.isHostileMob(e) && !affected.contains(e)) {
									LivingEntity le = (LivingEntity) e;
									double dmgAdd = 0;
									if (EntityUtils.isFrozen(le)) {
										dmgAdd += 5;
										le.setAI(true);
									} else {
										if (!EntityUtils.isBoss(le)) {
											EntityUtils.applyFreeze(mPlugin, 20 * 3, le);
										} else {
											le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2, false, true));
										}
									}
									EntityUtils.damageEntity(mPlugin, le, dmg + dmgAdd, player);
									affected.add(le);
								}
							}
							loc.subtract(x, y, z);


							if (t > 7.5) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.COLD_WAVE, 20 * 10);
				}
			}
		}

		if (InventoryUtils.isWandItem(player.getInventory().getItemInMainHand())) {
			int frozenHeart = ScoreboardUtils.getScoreboardValue(player, "FrozenHeart");
			/*
			 * Frozen Heart: Critically hitting an enemy freezes it for 2
			 * seconds. Deal 4/7 additional damage to frozen enemies on
			 * all attacks (spells, melee, bows). (Cooldown: 12s)
			 */
			if (frozenHeart > 0) {
				if (EntityUtils.isFrozen(e)) {
					double dmg = frozenHeart == 1 ? 4 : 7;
					EntityUtils.damageEntity(mPlugin, e, event.getDamage() + dmg, player);
				} else {
					if (PlayerUtils.isCritical(player)) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.FROZEN_HEART)) {
							e.getWorld().playSound(e.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.75f);
							ParticleEffect.SNOW_SHOVEL.display(0.2f, 0.2f, 0.2f, 0.2f, 50, e.getLocation().add(0, e.getHeight() / 2, 0), 40);
							if (!EntityUtils.isBoss(e)) {
								EntityUtils.applyFreeze(mPlugin, 20 * 3, e);
							} else {
								e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2, false, true));
							}
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.FROZEN_HEART, 20 * 5);
						}
					}
				}
			}
		}
		return true;
	}

}
