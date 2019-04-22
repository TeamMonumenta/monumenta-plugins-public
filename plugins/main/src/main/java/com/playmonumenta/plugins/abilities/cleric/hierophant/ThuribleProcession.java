package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Thurible Procession:
 * Level 1 - Blocking with a shield in either hand 2 times in a row,
 * holding the block on the second time, procedurally builds up
 * passive buffs, which are applied to all players (except the user)
 * within 20 blocks for as long as the block is held. The Hierophant
 * moves at ~80% normal walking speed when blocking to use this ability.
 * Progression of buffs Speed 1 (2 Seconds of Blocking), Strength 1 (4
 * seconds of blocking), Resistance 1 (6 seconds of blocking). After
 * blocking for 15 seconds, all players (including the Hiero) are given
 * 15 seconds of Speed 1, Resistance 1, and Strength 1.
 * At level 2, the radius is increased to 30 blocks and the Hiero moves
 * at normal walking speed when blocking for the ability. Buffs applied
 * at the end of the procession are increased to 20 seconds, and it only
 * takes 10 seconds of blocking to trigger. No cooldown.
 */
public class ThuribleProcession extends Ability {

	private static final Particle.DustOptions THURIBLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 175), 1.0f);
	private static final float DEFAULT_WALK_SPEED = 0.2f;
	private static final float THURIBLE_1_WALK_SPEED = 0.8f;
	private static final float THURIBLE_2_WALK_SPEED = 1.0f;
	private static final int THURIBLE_THRESHOLD_1 = 20 * 2;
	private static final int THURIBLE_THRESHOLD_2 = 20 * 4;
	private static final int THURIBLE_THRESHOLD_3 = 20 * 6;
	private static final int THURIBLE_1_THRESHOLD = 20 * 15;
	private static final int THURIBLE_2_THRESHOLD = 20 * 10;
	private static final int THURIBLE_1_BUFF_DURATION = 20 * 15;
	private static final int THURIBLE_2_BUFF_DURATION = 20 * 20;
	private static final int THURIBLE_1_RADIUS = 20;
	private static final int THURIBLE_2_RADIUS = 30;

	private Location mLastLocation;
	private int rightClicks = 0;

	public ThuribleProcession(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Thurible";
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.cooldown = 20 * 5; // Just to prevent spamming
		mInfo.linkedSpell = Spells.THURIBLE_PROCESSION;
	}

	@Override
	public boolean cast() {
		rightClicks++;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (rightClicks > 0) {
					rightClicks = 0;
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 20);

		if (rightClicks >= 2) {
			rightClicks = 0;
			float speed = getAbilityScore() == 1 ? THURIBLE_1_WALK_SPEED : THURIBLE_2_WALK_SPEED;
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1);
			new BukkitRunnable() {
				int t = 0;
				double rot = 0;
				int strands = 3;
				double radius = 3;
				@Override
				public void run() {
					if (t == 1) {
						mPlayer.setWalkSpeed(speed);
					}
					runParticles(rot, strands, radius, t);
					if (t % 20 == 0) {
						radius = 3;
					}
					rot += 10;
					radius -= 0.1428;

					int duration = getAbilityScore() == 1 ? THURIBLE_1_BUFF_DURATION : THURIBLE_2_BUFF_DURATION;
					int radius = getAbilityScore() == 1 ? THURIBLE_1_RADIUS : THURIBLE_2_RADIUS;
					int threshold = getAbilityScore() == 1 ? THURIBLE_1_THRESHOLD : THURIBLE_2_THRESHOLD;

					// Apply periodic buffs
					List<Player> players = PlayerUtils.getNearbyPlayers(mPlayer, radius, false);
					for (Player pl : players) {
						if (t > THURIBLE_THRESHOLD_1) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, 40, 0, true, true));
						}
						if (t > THURIBLE_THRESHOLD_2) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0, true, true));
						}
						if (t > THURIBLE_THRESHOLD_3) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 0, true, true));
						}
					}

					// Apply end buffs - we set walk speed slow one tick earlier
					// so the player isn't launched forward
					if (t == threshold - 1) {
						mPlayer.setWalkSpeed(DEFAULT_WALK_SPEED);
					}
					if (t >= threshold) {
						for (Player pl : PlayerUtils.getNearbyPlayers(mPlayer, radius, true)) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true));
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, true, true));
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0, true, true));
						}
						this.cancel();
						putOnCooldown();
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
						mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 60, 0, 0, 0, 0.35);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 60, 0.4, 0.4, 0.4, 1);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 200, 5, 3, 5, 1);
					}

					// Cancel if player stops blocking
					// Teleport player back to where they were 1 tick ago
					// to prevent them from walking off a cliff
					if (t > 0) {
						if (!mPlayer.isHandRaised() && !mPlayer.isBlocking() || mPlayer.isDead()) {
							this.cancel();
							putOnCooldown();
							mPlayer.setWalkSpeed(DEFAULT_WALK_SPEED);
							mPlayer.teleport(mLastLocation);
						}
					}

					mLastLocation = mPlayer.getLocation();
					t++;
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return ((mHand != null && mHand.getType() == Material.SHIELD && oHand.getType() != Material.BOW) ||
		        (oHand != null && oHand.getType() == Material.SHIELD && mHand.getType() != Material.BOW))
		       && !mPlayer.isSneaking();
	}

	public void runParticles(double rot, int strands, double radius, int t) {
		double rotAdd = 360 / strands;
		Location ploc = mPlayer.getLocation();
		for (int i = 0; i < strands; i++) {
			double radian = Math.toRadians(rot + (rotAdd * i));
			ploc.add(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
			mWorld.spawnParticle(Particle.REDSTONE, ploc, 15, 0.1, 0.1, 0.1, THURIBLE_COLOR);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, ploc, 5, 0.1f, 0.1f, 0.1f, 0);
			ploc.subtract(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
		}
		if (t % 20 == 0) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.85f, 1.15f);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 40), 15, 2, 0.4f, 2, 0);
		}
	}

}
