package com.playmonumenta.plugins.specializations;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;

public class SniperSpecialization extends BaseSpecialization {

	public SniperSpecialization(Plugin plugin, Random random) {
		super(plugin, random);
	}

	/*
	 * Notes:
	 * Time to fully charge a bow: ~1.1 Seconds
	 */

	public static final String PLAYER_OVERCHARGED_METAKEY = "PlayerOverchargedMetakey";
	public static final String PLAYER_ENCHANTED_ARROW_METAKEY = "PlayerEnchantedArrowMetakey";
	public static final String PLAYER_ENCHANTED_ARROW_CHARGED_METAKEY = "PlayerEnchantedArrowPrimedMetakey";
	public static final String PLAYER_SHARPSHOOTER_STACKS_METAKEY = "PlayerSharpshooterStacksMetakey";

	public static final String ARROW_BLOODHUNTER_ARROW_METAKEY = "ArrowBloodhunterArrowMetakey";

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (itemInHand.getType() == Material.BOW) {
				int overcharge = ScoreboardUtils.getScoreboardValue(player, "Overcharge");
				/*
				 * Overcharge: Drawing a bow to full charge and holding the arrow
				 * for 3 seconds will Overcharge the arrow. An Overcharged arrow
				 * travels 33% faster (and thus does more damage, how much im not sure)
				 * Level 2 reduces the time for overcharge to 2 seconds.
				 */

				int chargeTime = overcharge == 1 ? 20 * 2 : 20 * 3;
				if (overcharge > 0) {
					new BukkitRunnable() {
						int i = 0;
						@Override
						public void run() {
							i++;
							if (i >= 16) {

								ParticleEffect.CRIT.display(0, 0, 0, 0.25f, 10, player.getLocation(), 40);
								if (i == 16 + chargeTime) {
									MessagingUtils.sendActionBarMessage(mPlugin, player, "Your shot is now " + ChatColor.BOLD + "Overcharged!");
									player.setMetadata(PLAYER_OVERCHARGED_METAKEY, new FixedMetadataValue(mPlugin, null));
								}

								if (MetadataUtils.happenedThisTick(mPlugin, player, Constants.PLAYER_BOW_SHOT_METAKEY, -1) ||
								    !itemInHand.equals(player.getInventory().getItemInMainHand())) {
									this.cancel();
								}
							}
						}

					}.runTaskTimer(mPlugin, 5, 1);
				}
				int enchantedArrow = ScoreboardUtils.getScoreboardValue(player, "EnchantedArrow");

				if (enchantedArrow > 0) {
					new BukkitRunnable() {
						int i = 0;
						@Override
						public void run() {
							i++;
							if (i >= 16) {
								player.setMetadata(PLAYER_ENCHANTED_ARROW_CHARGED_METAKEY, new FixedMetadataValue(mPlugin, null));
								this.cancel();

								if (MetadataUtils.happenedThisTick(mPlugin, player, Constants.PLAYER_BOW_SHOT_METAKEY, -1) ||
								    !itemInHand.equals(player.getInventory().getItemInMainHand())) {
									this.cancel();
								}
							}
						}

					}.runTaskTimer(mPlugin, 5, 1);
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (itemInHand.getType() == Material.BOW && player.isSneaking()) {
				int enchantedArrow = ScoreboardUtils.getScoreboardValue(player, "EnchantedArrow");
				/*
				 * Enchanted Arrow: Sneak Left Clicking will prime an enchanted arrow.
				 * When fired, this arrow will instantaneously travel in a straight
				 * line for 30 blocks until hitting a block, piercing through all
				 * targets, dealing 20 / 40 damage. (Cooldown: 20s)
				 */

				if (enchantedArrow > 0) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.45f);
					player.setMetadata(PLAYER_ENCHANTED_ARROW_METAKEY, new FixedMetadataValue(mPlugin, enchantedArrow));
					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							ParticleEffect.SPELL_INSTANT.display(0.25f, 0, 0.25f, 0, 4, player.getLocation(), 40);
							if (!player.hasMetadata(PLAYER_ENCHANTED_ARROW_METAKEY) || t >= 20 * 10) {
								player.removeMetadata(PLAYER_ENCHANTED_ARROW_METAKEY, mPlugin);
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int sharpshooter = ScoreboardUtils.getScoreboardValue(player, "Sharpshooter");
		/*
		 * Sharpshooter: Hitting an enemy with an arrow grants you levels of ��Sharpshot��
		 * for 7 seconds. Each level you have of Sharpshot increases your arrow damage by
		 * 1. Every time you land an arrow shot, your level of Sharpshot is increased
		 * (max of 5 Levels) and its duration is refreshed. At level 2, you can have a max
		 * of 7 Sharpshot Levels, and its duration is increased to 9 seconds.
		 */
		if (sharpshooter > 0) {
			int max = sharpshooter == 1 ? 5 : 7;
			int duration = sharpshooter == 1 ? 20 * 5 : 20 * 7;
			if (!player.hasMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY)) {
				player.setMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY, new FixedMetadataValue(mPlugin, 1));
				MessagingUtils.sendActionBarMessage(mPlugin, player, "You have begun to stack Sharpshooter!");
				new BukkitRunnable() {
					int i = 0;
					int stacks = player.getMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY).get(0).asInt();
					@Override
					public void run() {
						i++;
						if (stacks != player.getMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY).get(0).asInt()) {
							i = 0;
							stacks = player.getMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY).get(0).asInt();
						}

						if (i >= duration) {
							player.removeMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY, mPlugin);
							this.cancel();
							MessagingUtils.sendActionBarMessage(mPlugin, player, "Sharpshooter has worn off and been reset to 0 stacks");
						}

					}

				}.runTaskTimer(mPlugin, 0, 1);
			} else {
				int stacks = player.getMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY).get(0).asInt();
				event.setDamage(event.getDamage() + stacks);
				if (stacks >= max) {
					player.setMetadata(PLAYER_SHARPSHOOTER_STACKS_METAKEY, new FixedMetadataValue(mPlugin, stacks + 1));
				}
			}
		}

		if (arrow.hasMetadata(ARROW_BLOODHUNTER_ARROW_METAKEY)) {
			int bounces = arrow.getMetadata(ARROW_BLOODHUNTER_ARROW_METAKEY).get(0).asInt();
			bounces -= 1;
			if (bounces >= 0) {
				LivingEntity target = null;
				double maxDist = 18;
				for (Entity e : damagee.getNearbyEntities(maxDist, maxDist, maxDist)) {
					if (EntityUtils.isHostileMob(e)) {
						LivingEntity le = (LivingEntity) e;
						if (le.getLocation().distance(damagee.getLocation()) < maxDist) {
							target = le;
							maxDist = le.getLocation().distance(damagee.getLocation());
						}
					}
				}
				if (target != null) {
					Location loc = damagee.getLocation().add(0, damagee.getHeight(), 0);
					Arrow a = (Arrow) damagee.getWorld().spawnEntity(loc, EntityType.ARROW);
					a.setShooter(player);
					a.setCritical(true);
					a.setVelocity(LocationUtils.getDirectionTo(target.getEyeLocation(), loc).multiply(2));
					a.setMetadata(ARROW_BLOODHUNTER_ARROW_METAKEY, new FixedMetadataValue(mPlugin, bounces));
				}
			}
		}
	}

	@Override
	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		int bloodhunterArrows = ScoreboardUtils.getScoreboardValue(player, "BloodhuntArrows");
		/*
		 * Bloodhunter Arrows: All arrows you fire travel 10% / 20% faster.
		 * Critically charged arrows, upon hitting an enemy, will deal damage
		 * and bounce off, aiming at the nearest target. At level 2, the
		 * arrow will bounce off of its second victim and aim at a third
		 * target. (All bounces do the same damage.) [Make Critically Charged
		 * Arrows have a Red Trail for a GoTG reference :P]
		 */
		if (bloodhunterArrows > 0) {
			double velInc = bloodhunterArrows == 1 ? 1.1 : 1.2;
			ParticleEffect.CRIT.display(0, 0, 0, 0.5f, 8, player.getEyeLocation().add(player.getLocation().getDirection()), 40);
			arrow.setVelocity(arrow.getVelocity().multiply(velInc));
			if (arrow.isCritical()) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.REDSTONE);
				arrow.setMetadata(ARROW_BLOODHUNTER_ARROW_METAKEY, new FixedMetadataValue(mPlugin, bloodhunterArrows));
			}
		}

		if (player.hasMetadata(PLAYER_ENCHANTED_ARROW_METAKEY) && player.hasMetadata(PLAYER_ENCHANTED_ARROW_CHARGED_METAKEY)) {
			int level = player.getMetadata(PLAYER_ENCHANTED_ARROW_METAKEY).get(0).asInt();
			double dmg = level == 1 ? 20 : 40;
			new BukkitRunnable() {
				int i = 0;
				LivingEntity tar = null;
				Location loc = player.getEyeLocation();
				Vector dir = loc.getDirection().normalize();
				@Override
				public void run() {
					i++;
					player.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.85f);
					player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 0.65f);
					player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					ParticleEffect.FIREWORKS_SPARK.display(0.1f, 0.1f, 0.1f, 0.2f, 10, loc.clone().add(dir), 40);
					for (int i = 0; i < 60; i++) {
						loc.add(dir.clone().multiply(0.5));
						ParticleEffect.SPELL_INSTANT.display(0.1f, 0.1f, 0.1f, 0, 3, loc, 40);
						ParticleEffect.FIREWORKS_SPARK.display(0.1f, 0.1f, 0.1f, 0.1f, 1, loc, 40);

						boolean gotTar = false;
						for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
							if (EntityUtils.isHostileMob(e) && !e.isDead()) {
								LivingEntity damagee = (LivingEntity) e;
								EntityUtils.damageEntity(mPlugin, damagee, dmg, player);

								LivingEntity target = null;
								tar = null;
								double maxDist = 18;
								for (Entity e2 : damagee.getNearbyEntities(maxDist, maxDist, maxDist)) {
									if (EntityUtils.isHostileMob(e2) && !e2.isDead()) {
										LivingEntity le = (LivingEntity) e2;
										if (le.getLocation().distance(damagee.getLocation()) < maxDist) {
											target = le;
											maxDist = le.getLocation().distance(damagee.getLocation());
										}
									}
								}
								if (target != null) {
									tar = target;
									loc = damagee.getEyeLocation();
									dir = LocationUtils.getDirectionTo(target.getLocation().add(0, target.getHeight(), 0), damagee.getEyeLocation());
								} else {
									this.cancel();
								}
								gotTar = true;
								break;
							}
						}
						if (gotTar) {
							break;
						}
						if (loc.getBlock().getType().isSolid()) {
							ParticleEffect.FIREWORKS_SPARK.display(0.1f, 0.1f, 0.1f, 0.2f, 150, loc, 40);
							player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
							break;
						}
					}

					if (tar == null) {
						this.cancel();
					}
					if (i >= 20 || loc.getBlock().getType().isSolid()) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 5);
			player.removeMetadata(PLAYER_ENCHANTED_ARROW_METAKEY, mPlugin);
			player.removeMetadata(PLAYER_ENCHANTED_ARROW_CHARGED_METAKEY, mPlugin);
			return false;
		}
		return true;
	}
}
