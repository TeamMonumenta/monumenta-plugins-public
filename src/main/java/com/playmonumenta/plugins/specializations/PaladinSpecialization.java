package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.Color;
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

public class PaladinSpecialization extends BaseSpecialization {
	private static final Particle.DustOptions HOLY_JAVELIN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.0f);
	private static final Particle.DustOptions HALLOWED_BEAM_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.0f);
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
					int damage = holyJavelin == 1 ? 10 : 20;
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.HOLY_JAVELIN)) {

						Location loc = player.getEyeLocation();
						Vector dir = loc.getDirection();
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 0.9f);

						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(dir), 10, 0, 0, 0, 0.125f);
						for (int i = 0; i < 15; i++) {
							loc.add(dir);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 22, 0.35, 0.35, 0.35, HOLY_JAVELIN_COLOR);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);

							for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.65, 0.65, 0.65)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									if (EntityUtils.isUndead(le)) {
										EntityUtils.damageEntity(mPlugin, le, damage, player);
									} else {
										EntityUtils.damageEntity(mPlugin, le, damage / 2, player);
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

	@Override
	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		if (arrow.isCritical()) {
			int hallowedBeam = ScoreboardUtils.getScoreboardValue(player, "HallowedBeam");
			/*
			 * Hallowed Beam: Level 1 - Firing a fully-drawn bow while sneaking,
			 * if pointed directly at a non-boss undead, will instantly deal 40
			 * damage to the undead instead of consuming the arrow.  Cooldown:
			 * 20s.
			 * Level 2 - The targeted undead explodes, dealing 20 damage
			 * to undead within a 5-block radius, and giving slowness 4 all enemies
			 * for 5s.

			 */
			if (hallowedBeam > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.HALLOWED_BEAM)) {
					LivingEntity e = EntityUtils.getCrosshairTarget(player, 30, false, true, true, false);
					if (e != null && EntityUtils.isUndead(e)) {
						player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.5f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
						Location loc = player.getEyeLocation();
						Vector dir = LocationUtils.getDirectionTo(e.getEyeLocation(), loc);
						for (int i = 0; i < 30; i++) {
							loc.add(dir);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 22, 0.35, 0.35, 0.35, HALLOWED_BEAM_COLOR);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
							if (loc.distance(e.getEyeLocation()) < 1.25) {
								loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
								loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
								break;
							}
						}
						EntityUtils.damageEntity(mPlugin, e, 40, player);
						Location eLoc = e.getLocation().add(0, e.getHeight() / 2, 0);
						mWorld.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
						mWorld.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f);
						if (hallowedBeam > 1) {
							mWorld.spawnParticle(Particle.SPELL_INSTANT, e.getLocation(), 500, 5, 0.15f, 5, 1);
							for (Entity ne : e.getNearbyEntities(5, 5, 5)) {
								if (ne instanceof LivingEntity) {
									LivingEntity le = (LivingEntity) ne;
									if (EntityUtils.isUndead(le)) {
										EntityUtils.damageEntity(mPlugin, le, 20, player);
										le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 3, false, true));
									}
								}
							}
						}
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.HALLOWED_BEAM, 20 * 20);
						return false;
					}
				} else {
					return true;
				}
			}
		}
		return true;
	}

}
