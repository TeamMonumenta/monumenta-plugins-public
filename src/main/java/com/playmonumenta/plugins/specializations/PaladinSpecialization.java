package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
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

public class PaladinSpecialization extends BaseSpecialization {
	private static final Particle.DustOptions HOLY_JAVELIN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.0f);
	
	private World mWorld;

	public PaladinSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	public static final String PLAYER_LUMINOUS_METAKEY = "PlayerLuminousInfusionMetakey";

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSprinting()) {
				int holyJavelin = ScoreboardUtils.getScoreboardValue(player, "HolyJavelin");
				/*
				 * Level 1 - Attacking while sprinting throws a piercing spear
				 * of light, dealing 10 damage to undead and 5 damage to all
				 * others, also lights all targets on fire for 5s. (10 cooldown)
				 * Level 2 - Damage to undead is increased to 20, non-undead damage
				 * is increased to 10, cooldown is reduced to 8s.
				 */
				if (holyJavelin > 0) {
					int damage = holyJavelin == 1 ? 10 : 15;
					int damage2 = holyJavelin == 1 ? 5 : 10;
							
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.HOLY_JAVELIN)) {

						Location loc = player.getEyeLocation();
						Vector dir = loc.getDirection();
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 0.9f);

						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(dir), 10, 0, 0, 0, 0.125f);
						for (int i = 0; i < 12; i++) {
							loc.add(dir);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 22, 0.35, 0.35, 0.35, HOLY_JAVELIN_COLOR);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);

							for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.65, 0.65, 0.65)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									if (EntityUtils.isUndead(le)) {
										EntityUtils.damageEntity(mPlugin, le, damage, player);
										if (holyJavelin > 1) {
											MovementUtils.PullTowards(player, le, 0.1f);
									} else {
										EntityUtils.damageEntity(mPlugin, le, damage2, player);
									}
									le.setFireTicks(20 * 5);
								}
							}

							if (loc.getBlock().getType().isSolid()) {
								loc.subtract(dir.multiply(0.5));
								mWorld.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f);
								loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
								loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
								break;
							}
						}

						int cooldown = holyJavelin == 1 ? 20 * 10 : 20 * 8;
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.HOLY_JAVELIN, cooldown);
					}
				}
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				if (player.getLocation().getPitch() > 75) {
					int luminousInfusion = ScoreboardUtils.getScoreboardValue(player, "LuminousInfusion");
					/*
					 * Luminous Infusion: Level 1 - Sneak and right-click while looking at the
					 * ground to infuse your weapon with light. Attacks done with the weapon do
					 * +10 damage to undead, and immobilize non-undead (Slowness V) for 2s. After 5
					 * swings, the infusion ends. Cooldown: 30s.
					 * Level 2 - The infusion lasts for 10 hits.
					 */
					if (luminousInfusion > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.LUMINOUS_INFUSION)) {
							int swings = luminousInfusion == 1 ? 5 : 10;
							player.setMetadata(PLAYER_LUMINOUS_METAKEY, new FixedMetadataValue(mPlugin, swings));
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
							mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1);
							new BukkitRunnable() {
								int t = 0;
								@Override
								public void run() {
									t++;
									Location rightHand = PlayerUtils.getRightSide(player.getEyeLocation(), 0.45).subtract(0, .8, 0);
									Location leftHand = PlayerUtils.getRightSide(player.getEyeLocation(), -0.45).subtract(0, .8, 0);
									mWorld.spawnParticle(Particle.SPELL_INSTANT, leftHand, 2, 0.05f, 0.05f, 0.05f, 0);
									mWorld.spawnParticle(Particle.SPELL_INSTANT, rightHand, 2, 0.05f, 0.05f, 0.05f, 0);
									if (t >= 20 * 25 || player.getMetadata(PLAYER_LUMINOUS_METAKEY).get(0).asInt() <= 0) {
										MessagingUtils.sendActionBarMessage(mPlugin, player, "Luminous Infusion has expired");
										player.removeMetadata(PLAYER_LUMINOUS_METAKEY, mPlugin);
										this.cancel();
									}
								}

							}.runTaskTimer(mPlugin, 0, 1);

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.LUMINOUS_INFUSION, 20 * 30);
						}
					}
				}
			}
			else if (player.isSprinting()) {
				int choirBells = ScoreboardUtils.getScoreboardValue(player, "ChoirBells");
				/*
				 * Choir Bells: Right-Clicking while sprinting causes the Cleric to
				 * become the target of any undead within a cone area in front of them.
				 * Affected Undead are stricken with slowness II (level 1) and 30%
				 * Vulnerability (level 2). Cooldown 30/20s
				 */

				if (choirBells > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CHOIR_BELLS)) {
						int cooldown = choirBells == 1 ? 30 * 20 : 20 * 20;
						ParticleUtils.explodingConeEffect(mPlugin, player, 8, Particle.VILLAGER_HAPPY, 0.5f, Particle.SPELL_INSTANT, 0.5f, 0.33);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.4f);
						Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
						for (Entity e : player.getNearbyEntities(8, 8, 8)) {
							if (e instanceof LivingEntity) {
								LivingEntity le = (LivingEntity) e;
								if (EntityUtils.isUndead(le)) {
									Vector toMobVector = le.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
									if (playerDir.dot(toMobVector) > 0.33) {
										((Monster) le).setTarget(player);
										if (choirBells > 0) {
											le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1, true, false));
										}
										if (choirBells > 1) {
											le.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 2, 5, true, false));
										}
									}
								}
							}
						}

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CHOIR_BELLS, cooldown);
					}
				}
			}
		}
	}
}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(PLAYER_LUMINOUS_METAKEY)) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (!InventoryUtils.isBowItem(item)) {
				int swings = player.getMetadata(PLAYER_LUMINOUS_METAKEY).get(0).asInt();
				LivingEntity e = (LivingEntity) event.getEntity();

				if (EntityUtils.isHostileMob(e)) {
					if (EntityUtils.isUndead(e)) {
						EntityUtils.damageEntity(mPlugin, e, 10, player);
					} else {
						e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4, true, false));
					}
				}

				player.setMetadata(PLAYER_LUMINOUS_METAKEY, new FixedMetadataValue(mPlugin, swings - 1));
			}
		}
		return true;
	}

	

}
