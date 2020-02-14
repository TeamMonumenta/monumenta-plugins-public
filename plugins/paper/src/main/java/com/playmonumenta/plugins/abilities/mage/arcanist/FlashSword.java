package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;

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
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Flash Sword: Sprint left clicking with a wand causes a wave
 * of Arcane blades to cut down all the foes in your path. Each
 * enemy within a 5 block cone takes 4 damage 3 times in rapid
 * succession, being knocked back with the last swipe. At level
 * 2 this abilities damage increases to 7 damage 3 times.
 * (CD: 10 seconds)
 */
public class FlashSword extends Ability {

	private static final int FSWORD_1_DAMAGE = 4;
	private static final int FSWORD_2_DAMAGE = 7;
	private static final int FSWORD_SWINGS = 3;
	private static final int FSWORD_RADIUS = 5;
	private static final int FSWORD_COOLDOWN = 20 * 10;
	private static final float FSWORD_KNOCKBACK_SPEED = 0.3f;
	private static final double FSWORD_DOT_ANGLE = 0.33;
	private static final Particle.DustOptions FSWORD_COLOR1 = new Particle.DustOptions(Color.fromRGB(106, 203, 255), 1.0f);
	private static final Particle.DustOptions FSWORD_COLOR2 = new Particle.DustOptions(Color.fromRGB(168, 226, 255), 1.0f);

	public FlashSword(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Flash Sword");
		mInfo.scoreboardId = "FlashSword";
		mInfo.linkedSpell = Spells.FSWORD;
		mInfo.cooldown = FSWORD_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
		int flashSword = getAbilityScore();
		Player player = mPlayer;
		putOnCooldown();
		new BukkitRunnable() {
			int t = 0;
			float pitch = 1.2f;
			int sw = 0;

			@Override
			public void run() {
				t++;
				sw++;
				Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
				Location origin = player.getLocation();
				if (player.getVelocity().length() > 0.1) {
					// If the player is moving, shift the flash sword in the direction they are moving
					origin.add(player.getVelocity().normalize().multiply(1.2));
				}
				for (LivingEntity mob : EntityUtils.getNearbyMobs(origin, FSWORD_RADIUS)) {
					Vector toMobVector = mob.getLocation().toVector().subtract(origin.toVector()).setY(0)
					                     .normalize();
					if (playerDir.dot(toMobVector) > FSWORD_DOT_ANGLE) {
						int damageMult = (flashSword == 1) ? FSWORD_1_DAMAGE : FSWORD_2_DAMAGE;
						Vector velocity = mob.getVelocity();
						mob.setNoDamageTicks(0);

						// Only interact with spellshock on the first swing
						if (t == 1) {
							EntityUtils.damageEntity(mPlugin, mob, damageMult, player, MagicType.ARCANE, true, mInfo.linkedSpell, true, true);
						} else {
							EntityUtils.damageEntity(mPlugin, mob, damageMult, player, MagicType.ARCANE, true, mInfo.linkedSpell, false, false);
						}

						if (t >= FSWORD_SWINGS) {
							MovementUtils.knockAway(player, mob, FSWORD_KNOCKBACK_SPEED);
						} else {
							mob.setVelocity(velocity);
						}
					}
				}

				if (t >= FSWORD_SWINGS) {
					pitch = 1.45f;
				}
				player.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.8f);
				player.getWorld().playSound(origin, Sound.ENTITY_WITHER_SHOOT, 0.75f, pitch);
				new BukkitRunnable() {
					final int i = sw;
					double roll;
					double d = 45;
					boolean init = false;

					@Override
					public void run() {
						if (!init) {
							if (i % 2 == 0) {
								roll = -8;
								d = 45;
							} else {
								roll = 8;
								d = 135;
							}
							init = true;
						}
						if (i % 2 == 0) {
							Vector vec;
							for (double r = 1; r < 5; r += 0.5) {
								for (double degree = d; degree < d + 30; degree += 5) {
									double radian1 = Math.toRadians(degree);
									vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
									vec = VectorUtils.rotateZAxis(vec, roll);
									vec = VectorUtils.rotateXAxis(vec, -origin.getPitch());
									vec = VectorUtils.rotateYAxis(vec, origin.getYaw());

									Location l = origin.clone().add(0, 1.25, 0).add(vec);
									mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
									mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
								}
							}

							d += 30;
						} else {
							Vector vec;
							for (double r = 1; r < 5; r += 0.5) {
								for (double degree = d; degree > d - 30; degree -= 5) {
									double radian1 = Math.toRadians(degree);
									vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
									vec = VectorUtils.rotateZAxis(vec, roll);
									vec = VectorUtils.rotateXAxis(vec, -origin.getPitch());
									vec = VectorUtils.rotateYAxis(vec, origin.getYaw());

									Location l = origin.clone().add(0, 1.25, 0).add(vec);
									mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
									mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
								}
							}
							d -= 30;
						}

						if ((d >= 135 && i % 2 == 0) || (d <= 45 && i % 2 > 0)) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
				if (t >= FSWORD_SWINGS) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 7);
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && (mHand != null && (InventoryUtils.isWandItem(mHand)));
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}
}
