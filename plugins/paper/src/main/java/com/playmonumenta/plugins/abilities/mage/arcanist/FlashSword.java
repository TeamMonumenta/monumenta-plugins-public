package com.playmonumenta.plugins.abilities.mage.arcanist;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class FlashSword extends Ability {

	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 8;
	private static final int SWINGS = 3;
	private static final int RADIUS = 5;
	private static final int COOLDOWN = 20 * 10;
	private static final float KNOCKBACK_SPEED_1 = 0.2f;
	private static final float KNOCKBACK_SPEED_2 = 0.4f;
	private static final double DOT_ANGLE = 0.33;
	private static final Particle.DustOptions FSWORD_COLOR1 = new Particle.DustOptions(Color.fromRGB(106, 203, 255), 1.0f);
	private static final Particle.DustOptions FSWORD_COLOR2 = new Particle.DustOptions(Color.fromRGB(168, 226, 255), 1.0f);

	private final int mDamage;
	private final float mKnockbackSpeed;

	public FlashSword(Plugin plugin, Player player) {
		super(plugin, player, "Flash Sword");
		mInfo.mScoreboardId = "FlashSword";
		mInfo.mShorthandName = "FS";
		mInfo.mDescriptions.add("Sprint left-clicking with a wand causes a wave of Arcane blades to hit every enemy within a 5 block cone 3 times (4 damage per hit) in rapid succession. The last hit causes knockback. Only the first hit can apply or trigger spellshock. Cooldown: 10s.");
		mInfo.mDescriptions.add("You instead do 8 damage 3 times. Knockback on the last hit is increased.");
		mInfo.mLinkedSpell = Spells.FSWORD;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mKnockbackSpeed = getAbilityScore() == 1 ? KNOCKBACK_SPEED_1 : KNOCKBACK_SPEED_2;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();
		new BukkitRunnable() {
			int mT = 0;
			float mPitch = 1.2f;
			int mSw = 0;

			@Override
			public void run() {
				mT++;
				mSw++;
				Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
				Location origin = mPlayer.getLocation();
				if (mPlayer.getVelocity().length() > 0.1) {
					// If the player is moving, shift the flash sword in the direction they are moving
					origin.add(mPlayer.getVelocity().normalize().multiply(1.2));
				}
				for (LivingEntity mob : EntityUtils.getNearbyMobs(origin, RADIUS)) {
					Vector toMobVector = mob.getLocation().toVector().subtract(origin.toVector()).setY(0)
					                     .normalize();
					if (playerDir.dot(toMobVector) > DOT_ANGLE) {
						Vector velocity = mob.getVelocity();
						mob.setNoDamageTicks(0);

						// Only interact with spellshock on the first swing
						if (mT == 1) {
							EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, true, true);
						} else {
							EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, false, false);
						}

						if (mT >= SWINGS) {
							MovementUtils.knockAway(mPlayer, mob, mKnockbackSpeed, mKnockbackSpeed);
						} else {
							mob.setVelocity(velocity);
						}
					}
				}

				if (mT >= SWINGS) {
					mPitch = 1.45f;
				}
				World world = mPlayer.getWorld();
				world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.8f);
				world.playSound(origin, Sound.ENTITY_WITHER_SHOOT, 0.75f, mPitch);
				new BukkitRunnable() {
					final int mI = mSw;
					double mRoll;
					double mD = 45;
					boolean mInit = false;

					@Override
					public void run() {
						if (!mInit) {
							if (mI % 2 == 0) {
								mRoll = -8;
								mD = 45;
							} else {
								mRoll = 8;
								mD = 135;
							}
							mInit = true;
						}
						if (mI % 2 == 0) {
							Vector vec;
							for (double r = 1; r < 5; r += 0.5) {
								for (double degree = mD; degree < mD + 30; degree += 5) {
									double radian1 = Math.toRadians(degree);
									vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
									vec = VectorUtils.rotateZAxis(vec, mRoll);
									vec = VectorUtils.rotateXAxis(vec, -origin.getPitch());
									vec = VectorUtils.rotateYAxis(vec, origin.getYaw());

									Location l = origin.clone().add(0, 1.25, 0).add(vec);
									world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
									world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
								}
							}

							mD += 30;
						} else {
							Vector vec;
							for (double r = 1; r < 5; r += 0.5) {
								for (double degree = mD; degree > mD - 30; degree -= 5) {
									double radian1 = Math.toRadians(degree);
									vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
									vec = VectorUtils.rotateZAxis(vec, mRoll);
									vec = VectorUtils.rotateXAxis(vec, -origin.getPitch());
									vec = VectorUtils.rotateYAxis(vec, origin.getYaw());

									Location l = origin.clone().add(0, 1.25, 0).add(vec);
									world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
									world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
								}
							}
							mD -= 30;
						}

						if ((mD >= 135 && mI % 2 == 0) || (mD <= 45 && mI % 2 > 0)) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
				if (mT >= SWINGS) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 7);
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && InventoryUtils.isWandItem(mainHand);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}
}
