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
import com.playmonumenta.plugins.classes.magic.MagicType;
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
	private static final int BODKINBLITZ_1_STEP = 25;
	private static final int BODKINBLITZ_2_STEP = 35;

	private LivingEntity mark;

	public BodkinBlitz(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Bodkin Blitz");
		mInfo.linkedSpell = Spells.BODKIN_BLITZ;
		mInfo.scoreboardId = "BodkinBlitz";
		mInfo.mShorthandName = "BB";
		mInfo.mDescriptions.add("Left-click while sneaking and holding two swords to teleport 10 blocks forwards. Upon teleporting, strike the nearest enemy within 3 blocks, dealing them 25 damage. This ability cannot be used in safe zones. Cooldown: 25 seconds.");
		mInfo.mDescriptions.add("Range increased to 14 blocks. Bodkin Blitz now deals 30 damage and marks the target for up to 5 seconds. Killing a marked enemy refreshes the cooldown of Bodkin Blitz by 10 seconds.");
		mInfo.cooldown = BODKINBLITZ_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			return;
		}

		putOnCooldown();

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 2f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

		new BukkitRunnable() {
			Location mTpLoc = mPlayer.getLocation();
			Vector mShiftVec = mPlayer.getLocation().getDirection().normalize().multiply(0.1);
			BoundingBox mPlayerBox = mPlayer.getBoundingBox();
			int mTick = 0;
			int mStep = getAbilityScore() == 1 ? BODKINBLITZ_1_STEP : BODKINBLITZ_2_STEP;

			@Override
			public void run() {
				// Fire projectile.
				for (int i = 0; i < mStep; i++) {
					Location boxLoc = mPlayerBox.getCenter().toLocation(mWorld);
					
					boolean isBlocked = true;
					BoundingBox testBox = mPlayerBox.clone();
					
					// Preliminary check on the spot the player is standing on, before shifting locations.
					if (testLocation(testBox)) {
						mTpLoc = testBox.getCenter().toLocation(mWorld).add(0, -testBox.getHeight() / 2, 0);
						isBlocked = false;
					}
					
					if (isBlocked) {
						testBox.shift(0, -1, 0);
						for (int dy = 0; dy < 20; dy++) {
							// Start by scanning along the y-axis, from -1 to +1, to find the lowest available space.
							if (testLocation(testBox)) {
								mTpLoc = testBox.getCenter().toLocation(mWorld).add(0, -testBox.getHeight() / 2, 0);
								isBlocked = false;
								break;
							}
							
							testBox.shift(0, 0.1, 0);
						}
					}

					if (isBlocked) {
						// If no spot was found, then you've literally hit a wall. Stop iterating.
						mTick = 4;
						break;
					}

					mWorld.spawnParticle(Particle.FALLING_DUST, boxLoc, 5, 0.15, 0.45, 0.1,
							Bukkit.createBlockData("gray_concrete"));
					mWorld.spawnParticle(Particle.CRIT, boxLoc, 4, 0.25, 0.5, 0.25, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, boxLoc, 5, 0.15, 0.45, 0.15, 0.01);

					mPlayerBox.shift(mShiftVec);

				}
				mTick++;
				// Each incrementation of j checks for 1.5 blocks, for a max of 4 (6 blocks).
				// This is so that we can have a small projectile animation.

				// Teleport player
				if (mTick >= 4) {
					mTpLoc.setDirection(mPlayer.getLocation().getDirection());
					mTpLoc.add(0, 0.1, 0);
					mPlayer.teleport(mTpLoc);

					mWorld.playSound(mTpLoc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 2f);
					mWorld.playSound(mTpLoc, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
					mWorld.playSound(mTpLoc, Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
					mWorld.playSound(mTpLoc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
					mWorld.playSound(mTpLoc, Sound.ENTITY_PHANTOM_HURT, 1f, 0.75f);
					mWorld.playSound(mTpLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

					mWorld.spawnParticle(Particle.SMOKE_LARGE, mTpLoc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.18);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, mTpLoc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.04);
					mWorld.spawnParticle(Particle.SPELL_WITCH, mTpLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, mTpLoc.clone().add(0, 1, 0), 50, 0.75, 0.5, 0.75, 0.05);
					mWorld.spawnParticle(Particle.CRIT, mTpLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.3);

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
						EntityUtils.damageEntity(mPlugin, target, damage, mPlayer, MagicType.PHYSICAL, true, mInfo.linkedSpell);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
	
	private boolean testLocation(BoundingBox box) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y <= 2; y++) {
					// Checking the blocks around the hitbox.
					Block block = box.getCenter().toLocation(mWorld).clone().add(x * 0.4, y * 0.975 - box.getHeight() / 2, z * 0.4).getBlock();
					// A player's hitbox is 0.625 * 0.625 * 1.8125 blocks. Rounding up to 0.8 * 0.8 * 1.95 to be safe.
					
					if (block.getType().isSolid() && block.getBoundingBox().overlaps(box)) {
						// If a bad spot has already been found, then there's no need to check the rest-- this spot is invalid.
						return false;
					}
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
			return mPlayer.isSneaking() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);
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
