package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class BodkinBlitz extends Ability {

	private static final int BODKINBLITZ_COOLDOWN = 25 * 20;
	private static final int BODKINBLITZ_1_DAMAGE = 25;
	private static final int BODKINBLITZ_2_DAMAGE = 30;

	private int mLeftClicks = 0;
	private LivingEntity mark;

	public BodkinBlitz(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Bodkin Blitz");
		mInfo.linkedSpell = Spells.BODKIN_BLITZ;
		mInfo.scoreboardId = "BodkinBlitz";
		mInfo.mShorthandName = "BB";
		mInfo.mDescriptions.add("Left-click quickly three times while holding two swords to teleport 6 blocks forwards. Upon teleporting, strike the nearest enemy within 3 blocks, dealing them 25 damage. This ability cannot be used in safe zones. Cooldown: 25 seconds.");
		mInfo.mDescriptions.add("Bodkin Blitz now deals 30 damage and marks the target for up to 5 seconds. Killing a marked enemy refreshes the cooldown of Bodkin Blitz by 10 seconds.");
		mInfo.cooldown = BODKINBLITZ_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			return;
		}

		mLeftClicks++;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mLeftClicks > 0) {
					mLeftClicks--;
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 20);
		if (mLeftClicks < 3) {
			return;
		}

		mLeftClicks = 0;

		putOnCooldown();

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 2f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

		new BukkitRunnable() {
			Location boxLoc;
			Vector shiftVec = mPlayer.getLocation().getDirection().normalize().multiply(0.1);
			BoundingBox loc = mPlayer.getBoundingBox();
			int j = 0;

			@Override
			public void run() {
				// Since the player's bounding box is a bit too close to the ground. This should give players more distance more consistently
				// (due to how the teleport location is currently determined)
				loc.shift(0, 0.15, 0);

				// Fire projectile.
				for (int i = 0; i < 15; i++) {
					boxLoc = loc.getCenter().toLocation(mWorld);
					boxLoc.setDirection(mPlayer.getLocation().getDirection());

					/**
					 * Going to continue to work on this for next week. What I want is for it to
					 * teleport smarter: if the bounding box overlaps with a ceiling or floor,
					 * it will try to reposition to find a suitable location within y +- 0.5.
					 */
//					boolean isBlocked = true;
//					for (int x = -1; x <= 1; x++) {
//						for (int z = -1; z <= 1; z++) {
//							for (int y = -1; y <= 1; y++) {
//								for (int dy = 0; dy < 8; dy++) {
//									// This condition here just saves us a few checks.
//									if (isBlocked) {
//										Block block = boxLoc.clone().add(x, y + dy / 10.0, z).getBlock();
//										if (!block.getBoundingBox().overlaps(loc) || !block.getType().isSolid()) {
//											isBlocked = false;
//											break;
//										}
//									}
//								}
//							}
//						}
//					}

					boolean isBlocked = false;
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							for (int y = -1; y <= 1; y++) {
								// This condition here just saves us a few checks.
								if (!isBlocked) {
									Block block = boxLoc.clone().add(x, y, z).getBlock();
									if (block.getType().isSolid() && block.getBoundingBox().overlaps(loc)) {
										isBlocked = true;
										break;
									}
								}
							}
						}
					}

					if (isBlocked) {
						j = 4;
						break;
					}

					mWorld.spawnParticle(Particle.FALLING_DUST, boxLoc, 5, 0.15, 0.45, 0.1,
							Bukkit.createBlockData("gray_concrete"));
					mWorld.spawnParticle(Particle.CRIT, boxLoc, 4, 0.25, 0.5, 0.25, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, boxLoc, 5, 0.15, 0.45, 0.15, 0.01);

					loc.shift(shiftVec);

				}
				j++;
				// Each incrementation of j checks for 1.5 blocks, for a max of 4 (6 blocks).
				// This is so that we can have a small projectile animation.

				// Teleport player
				if (j >= 4) {
					Location tpLoc = boxLoc.clone().add(0, -0.85, 0);

					for (int i = 0; i < 10; i++) {
						if (!tpLoc.getBlock().getType().isSolid()) {
							tpLoc.subtract(0, 0.1, 0);
						} else {
							tpLoc.add(0, 0.3, 0);
							break;
						}
					}

					mPlayer.teleport(tpLoc);

					mWorld.playSound(boxLoc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 2f);
					mWorld.playSound(boxLoc, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
					mWorld.playSound(boxLoc, Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
					mWorld.playSound(boxLoc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
					mWorld.playSound(boxLoc, Sound.ENTITY_PHANTOM_HURT, 1f, 0.75f);
					mWorld.playSound(boxLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

					mWorld.spawnParticle(Particle.SMOKE_LARGE, boxLoc, 30, 0.25, 0.5, 0.25, 0.18);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, boxLoc, 15, 0.25, 0.5, 0.25, 0.04);
					mWorld.spawnParticle(Particle.SPELL_WITCH, boxLoc, 15, 0.5, 0.5, 0.5, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, boxLoc, 50, 0.75, 0.5, 0.75, 0.05);
					mWorld.spawnParticle(Particle.CRIT, boxLoc, 25, 1, 1, 1, 0.3);

					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 19, true, false));

					LivingEntity target = EntityUtils.getNearestHostile(mPlayer, 3);

					// Damage enemy, if applicable
					if (target != null) {
						Location beamLoc = mPlayer.getLocation().add(0, 1.5, 0);
						Location enemyLoc = target.getLocation().add(0, 1.5, 0);
						Vector beam = LocationUtils.getDirectionTo(enemyLoc, beamLoc).multiply(0.2);

						for (int i = 0; i < 15; i++) {
							beamLoc.add(beam);

							mWorld.spawnParticle(Particle.SMOKE_LARGE, beamLoc, 3, 0.2, 0.2, 0.2, 0.01);
							mWorld.spawnParticle(Particle.CRIT, beamLoc, 1, 0.2, 0.2, 0.2, 0.2);
							mWorld.spawnParticle(Particle.SPELL_MOB, beamLoc, 2, 0.2, 0.2, 0.2, 0);

							if (beamLoc.distance(enemyLoc) < 0.4) {
								break;
							}
						}

						mWorld.spawnParticle(Particle.SMOKE_LARGE, enemyLoc, 15, 0.25, 0.5, 0.25, 0.1);
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, enemyLoc, 20, 0.25, 0.5, 0.25, 0.07);
						mWorld.spawnParticle(Particle.SPELL_WITCH, enemyLoc, 8, 0.45, 0.5, 0.45, 0);
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, enemyLoc, 5, 0.5, 0.6, 0.5, 0);
						mWorld.spawnParticle(Particle.CRIT, enemyLoc, 40, 0.25, 0.5, 0.25, 0.4);

						mWorld.playSound(enemyLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 2f);
						mWorld.playSound(enemyLoc, Sound.BLOCK_ANVIL_LAND, 0.6f, 2f);

						if (getAbilityScore() > 1) {
							mark = target;

							new BukkitRunnable() {
								int i = 0;

								@Override
								public void run() {
									if (i >= 100) {
										mark = null;
										this.cancel();
									} else {
										i++;
										if (mark == null) {
											i = 100;
										} else {
											mWorld.spawnParticle(Particle.SMOKE_LARGE,
											                     mark.getLocation().clone().add(0, 1, 0),
																 1, 0.25, 0.5, 0.25, 0.07f);

										}
									}
								}
							}.runTaskTimer(mPlugin, 0, 1);
						}
						int damage = getAbilityScore() == 1 ? BODKINBLITZ_1_DAMAGE : BODKINBLITZ_2_DAMAGE;
						EntityUtils.damageEntity(mPlugin, target, damage, mPlayer);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
			return !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);
		}
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		EntityDamageEvent e = event.getEntity().getLastDamageCause();
		if (e.getCause() == DamageCause.ENTITY_ATTACK || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK
				|| e.getCause() == DamageCause.CUSTOM) {
			if (event.getEntity() == mark) {
				LivingEntity mob = event.getEntity();
				mWorld.playSound(mob.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 2f);
				mWorld.playSound(mob.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 0.75f);
				mWorld.playSound(mob.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 2f);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, mob.getLocation(), 30, 0.25, 0.5, 0.25, 0.2f);
				mWorld.spawnParticle(Particle.SPELL_WITCH, mob.getLocation(), 20, 0.35, 0.5, 0.35, 0f);

				mPlugin.mTimers.updateCooldown(mPlayer, mInfo.linkedSpell, 200);
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Cooldown refreshed!");

				mark = null;
			}
		}
	}
}
