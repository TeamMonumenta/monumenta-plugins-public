package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BladeDance extends Ability {

	private static final int DANCE_1_DAMAGE = 12;
	private static final int DANCE_2_DAMAGE = 18;
	private static final int DANCE_RADIUS = 4;
	private static final float DANCE_KNOCKBACK_SPEED = 0.2f;
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	public BladeDance(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Blade Dance");
		mInfo.scoreboardId = "BladeDance";
		mInfo.mShorthandName = "BD";
		mInfo.mDescriptions.add("When holding two swords, right-click while looking down to enter a defensive stance, parrying all attacks and becoming invulnerable for 0.75 seconds. Afterwards, unleash a powerful attack that deals 12 damage to and afflicts Weakness III to all enemies in a 4 block radius for 5 seconds. Cooldown: 20 seconds.");
		mInfo.mDescriptions.add("The area attack now deals 18 damage and afflicts Weakness IV.");
		mInfo.linkedSpell = Spells.BLADE_DANCE;
		mInfo.cooldown = 20 * 15;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;

		/*
		 * NOTE! Because BladeDance has two events (cast and damage), we need both
		 * events to trigger even when it is on cooldown. Therefor it needs to bypass
		 * the automatic cooldown check and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.BLADE_DANCE)
				|| mPlayer.getLocation().getPitch() < 50 || mPlayer.isSneaking()
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInMainHand())
				|| !InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInOffHand())) {
			return;
		}

		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
		mWorld.spawnParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation().clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.15);
		mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0, SWORDSAGE_COLOR);
		mPlayer.setInvulnerable(true);
		if (getAbilityScore() >= 2) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 20 * 2, 1, true, false));
		}
		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				mTicks += 1;
				Location loc = mPlayer.getLocation();
				double r = DANCE_RADIUS - (3 * mPitch);
				mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, r, 2, r, 0);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 4, r, 2, r, 0, SWORDSAGE_COLOR);
				mWorld.spawnParticle(Particle.CLOUD, loc, 4, r, 2, r, 0);
				if (mTicks % 2 == 0) {
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
					mPitch += 0.1f;
				}

				if (mTicks >= 15) {
					mPlayer.setInvulnerable(false);
					mWorld.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
					mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2f);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

					mWorld.spawnParticle(Particle.VILLAGER_ANGRY, mPlayer.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0);

					int damage = getAbilityScore() == 1 ? DANCE_1_DAMAGE : DANCE_2_DAMAGE;

					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), DANCE_RADIUS)) {
						mob.setNoDamageTicks(0);
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.PHYSICAL, true, mInfo.linkedSpell);
						MovementUtils.knockAway(mPlayer, mob, DANCE_KNOCKBACK_SPEED);

						int amplifier = getAbilityScore() == 1 ? 2 : 3;
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, 100, amplifier, true, true));

						Location mobLoc = mob.getLocation().add(0, 1, 0);
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0);
						mWorld.spawnParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3);
						mWorld.spawnParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR);
					}

					new BukkitRunnable() {
						int mTicks = 0;
						Vector mVec;
						double mRadians = 0;

						@Override
						public void run() {
							mVec = new Vector(Math.cos(mRadians) * DANCE_RADIUS / 1.5, 0, Math.sin(mRadians) * DANCE_RADIUS / 1.5);

							Location loc2 = mPlayer.getEyeLocation().add(mVec);
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc2, 5, 1, 0.25, 1, 0);
							mWorld.spawnParticle(Particle.CRIT, loc2, 10, 1, 0.25, 1, 0.3);
							mWorld.spawnParticle(Particle.REDSTONE, loc2, 10, 1, 0.25, 1, 0, SWORDSAGE_COLOR);
							mWorld.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);

							if (mTicks >= 5) {
								this.cancel();
							}

							mTicks++;
							mRadians += Math.toRadians(72);
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}
}
