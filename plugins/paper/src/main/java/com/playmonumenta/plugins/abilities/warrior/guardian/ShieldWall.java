package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ShieldWall extends Ability {

	private static final int SHIELD_WALL_1_DURATION = 8 * 30;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 6;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 20;

	public ShieldWall(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Shield Wall");
		mInfo.mScoreboardId = "ShieldWall";
		mInfo.mShorthandName = "SW";
		mInfo.mDescriptions.add("Press the swap key while holding a shield in either hand to create a 180 degree arc of particles 5 blocks high and 4 blocks wide in front of the user. This blocks all enemy projectiles (Ghast fireballs explode on the wall) and deals 6 magic damage to enemies that pass through the wall. The shield lasts 8 seconds. Cooldown: 30s.");
		mInfo.mDescriptions.add("The shield lasts 10 seconds instead. Additionally, the shield knocks back enemies that try to go through it. Cooldown: 20s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? SHIELD_WALL_1_COOLDOWN : SHIELD_WALL_2_COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.SHIELD_WALL;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.STONE_BRICK_WALL, 1);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		event.setCancelled(true);
		if (!isTimerActive()) {
			int time = getAbilityScore() == 1 ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION;
			boolean knockback = getAbilityScore() != 1;
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1.5f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.8f);
			new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 70, 0, 0, 0, 0.3f).spawnAsPlayerActive(mPlayer);
			putOnCooldown();

			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

			new BukkitRunnable() {
				int mT = 0;
				final Location mLoc = mPlayer.getLocation();
				final List<BoundingBox> mBoxes = new ArrayList<>();
				List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
				final List<LivingEntity> mMobsHitThisTick = new ArrayList<>();
				boolean mHitboxes = false;

				@Override
				public void run() {
					mT++;
					Vector vec;
					for (int y = 0; y < 5; y++) {
						for (double degree = 0; degree < 180; degree += 10) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(FastUtils.cos(radian1) * 4, y, FastUtils.sin(radian1) * 4);
							vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

							Location l = mLoc.clone().add(vec);
							if (mT % 4 == 0) {
								new PartialParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
							}
							if (!mHitboxes) {
								mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								                         l.clone().add(0.6, 5, 0.6)));
							}
						}
						mHitboxes = true;
					}

					for (BoundingBox box : mBoxes) {
						for (Entity e :world.getNearbyEntities(box)) {
							Location eLoc = e.getLocation();
							if (e instanceof Projectile proj) {
								if (proj.getShooter() instanceof LivingEntity shooter
										&& (!(proj.getShooter() instanceof Player) || AbilityManager.getManager().isPvPEnabled((Player) shooter))) {
									proj.remove();
									new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 5, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
									world.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.5f);
								}
							} else if (e instanceof LivingEntity le && EntityUtils.isHostileMob(e)) {
								// Stores mobs hit this tick
								mMobsHitThisTick.add(le);
								// This list does not update to the mobs hit this tick until after everything runs
								if (!mMobsAlreadyHit.contains(le)) {
									mMobsAlreadyHit.add(le);
									Vector v = le.getVelocity();

									DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), SHIELD_WALL_DAMAGE, false, true, false);

									//Bosses should not be affected by slowness or knockback.
									if (knockback && !e.getScoreboardTags().contains("Boss")) {
										MovementUtils.knockAway(mLoc, le, 0.3f, true);
										new PartialParticle(Particle.EXPLOSION_NORMAL, eLoc, 50, 0, 0, 0, 0.35f).spawnAsPlayerActive(mPlayer);
										world.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
									} else {
										le.setVelocity(v);
									}
								} else if (le.getNoDamageTicks() + 5 < le.getMaximumNoDamageTicks()) {
									if (knockback && !e.getScoreboardTags().contains("Boss")) {
										/*
										 * This is a temporary fix while we decide how to handle KBR mobs with Shield Wall level 2.
										 *
										 * If a mob collides with shield wall halfway through its invulnerability period, assume it
										 * resists knockback and give it Slowness V for 5 seconds to simulate the old effect of
										 * halting mobs with stunlock damage, minus the insane damage part.
										 *
										 * This effect is reapplied each tick, so the mob is slowed drastically until 2 seconds
										 * after they leave shield wall hitbox.
										 */
										PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4, true, false));
									}
								}
							}
						}
					}
					/*
					 * Compare the two lists of mobs and only remove from the
					 * actual hit tracker if the mob isn't detected as hit this
					 * tick, meaning it is no longer in the shield wall hitbox
					 * and is thus eligible for another hit.
					 */
					List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
					for (LivingEntity mob : mMobsAlreadyHit) {
						if (mMobsHitThisTick.contains(mob)) {
							mobsAlreadyHitAdjusted.add(mob);
						}
					}
					mMobsAlreadyHit = mobsAlreadyHitAdjusted;
					mMobsHitThisTick.clear();
					if (mT >= time) {
						this.cancel();
						mBoxes.clear();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mHand.getType() == Material.SHIELD || oHand.getType() == Material.SHIELD;
	}
}
