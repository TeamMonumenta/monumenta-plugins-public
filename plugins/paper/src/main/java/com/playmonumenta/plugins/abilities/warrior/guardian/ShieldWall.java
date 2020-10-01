package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;

/*
 * Shield Wall: Blocking and then blocking again within 0.25s
 * Creates a 180 degree arc of particles with a height of 5 blocks
 * and width of 4 blocks in front of the user, blocking all enemy
 * projectiles and dealing 6 damage to enemies who pass through the
 * wall. The shield lasts 8/10 seconds. At level 2, this shield knocks
 * back enemies as well. (Ghast fireballs explode on the wall)
 * Cooldown: 30/20 seconds
 */
public class ShieldWall extends Ability {

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "ShieldWallTickRightClicked";

	private static final int SHIELD_WALL_1_DURATION = 8 * 30;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 6;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 20;

	public ShieldWall(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Shield Wall");
		mInfo.mScoreboardId = "ShieldWall";
		mInfo.mShorthandName = "SW";
		mInfo.mDescriptions.add("Blocking and then blocking again within .25s creates a 180 degree arc of particles 5 blocks high and 4 blocks wide in front of the user. This blocks all enemy projectiles (Ghast fireballs explode on the wall) and deals 6 damage to enemies that pass through the wall. The shield lasts 8 seconds. Cooldown: 30s.");
		mInfo.mDescriptions.add("The shield lasts 10 seconds instead. Additionally, the shield knocks back enemies that try to go through it. Cooldown is reduced to 20s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? SHIELD_WALL_1_COOLDOWN : SHIELD_WALL_2_COOLDOWN;
		mInfo.mLinkedSpell = Spells.SHIELD_WALL;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	private int mRightClicks = 0;

	@Override
	public void cast(Action action) {
		// Prevent two right clicks being registered from one action (e.g. blocking)
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;

			// This timer makes sure that the player actually blocked instead of some other right click interaction
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!mPlayer.isHandRaised() && mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 1);

			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 5);
		}

		if (mRightClicks >= 2) {
			int time = getAbilityScore() == 1 ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION;
			boolean knockback = getAbilityScore() != 1;
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1.5f);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.8f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 70, 0, 0, 0, 0.3f);
			putOnCooldown();
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
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

							Location l = mLoc.clone().add(vec);
							if (mT % 4 == 0) {
								mWorld.spawnParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0);
							}
							if (!mHitboxes) {
								mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								                         l.clone().add(0.6, 5, 0.6)));
							}
						}
						mHitboxes = true;
					}

					for (BoundingBox box : mBoxes) {
						for (Entity e : mWorld.getNearbyEntities(box)) {
							Location eLoc = e.getLocation();
							if (e instanceof Projectile) {
								Projectile proj = (Projectile) e;
								if (proj.getShooter() instanceof LivingEntity) {
									LivingEntity shooter = (LivingEntity) proj.getShooter();
									if (!(shooter instanceof Player) || AbilityManager.getManager().isPvPEnabled((Player)shooter)) {
										proj.remove();
										mWorld.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 5, 0, 0, 0, 0.25f);
										mWorld.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.5f);
									}
								}
							} else if (EntityUtils.isHostileMob(e)) {
								LivingEntity le = (LivingEntity) e;
								// Stores mobs hit this tick
								mMobsHitThisTick.add(le);
								// This list does not update to the mobs hit this tick until after everything runs
								if (!mMobsAlreadyHit.contains(le)) {
									mMobsAlreadyHit.add(le);
									Vector v = le.getVelocity();
									EntityUtils.damageEntity(mPlugin, le, SHIELD_WALL_DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
									//Bosses should not be affected by slowness or knockback.
									if (knockback && !e.getScoreboardTags().contains("Boss")) {
										MovementUtils.knockAway(mLoc, le, 0.3f);
										mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, eLoc, 50, 0, 0, 0, 0.35f);
										mWorld.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
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
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mHand.getType() != Material.BOW && (mHand.getType() == Material.SHIELD || oHand.getType() == Material.SHIELD);
	}

}
