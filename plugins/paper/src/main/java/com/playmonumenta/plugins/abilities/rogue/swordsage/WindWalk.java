package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class WindWalk extends Ability {

	/*
	 * Wind Walk: Level 1 - Sprint + LClick without hitting a mob 3 times
	 * in quick succession to rush forward on a gale of air.
	 * Regular mobs within 4 blocks of you during the rush are knocked into
	 * the air and levitated for 3 seconds. Elites remain on the ground and
	 * are stunned. Cooldown: 25 seconds
	 * Level 2 – Nonelite mobs are given 30% Vulnerability for 8 seconds and
	 * mobs are affected by their respective debuffs for 5 seconds.
	 */

	private static final int WIND_WALK_COOLDOWN = 20 * 20;
	private static final int WIND_WALK_1_DURATION = 20 * 2;
	private static final int WIND_WALK_2_DURATION = 20 * 4;
	private static final int WIND_WALK_VULNERABILITY_DURATION_INCREASE = 20 * 3;
	private static final int WIND_WALK_VULNERABILITY_AMPLIFIER = 5;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.5;

	private int mLeftClicks = 0;

	public WindWalk(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.WIND_WALK;
		mInfo.scoreboardId = "WindWalk";
		mInfo.cooldown = WIND_WALK_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
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
		if (mLeftClicks < 2) {
			return;
		}
		int duration = getAbilityScore() == 1 ? WIND_WALK_1_DURATION : WIND_WALK_2_DURATION;
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 90, 0.25, 0.45, 0.25, 0.1);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.45, 0.25, 0.15);
		Vector direction = mPlayer.getLocation().getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));
		new BukkitRunnable() {
			List<LivingEntity> mobsAlreadyHit = new ArrayList<LivingEntity>();
			@Override
			public void run() {
				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 7, 0.25, 0.45, 0.25, 0);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation().add(mPlayer.getVelocity().normalize()), WIND_WALK_RADIUS)) {
					if (!mobsAlreadyHit.contains(mob)) {
						if (!EntityUtils.isBoss(mob)) {
							mWorld.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 1.25f);
							EntityUtils.applyStun(mPlugin, duration, mob);
						}
						if (EntityUtils.isElite(mob)) {
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);
							mWorld.playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 0.75f);
						} else if (!EntityUtils.isBoss(mob)) {
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0);
							mWorld.spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1);
							mob.setVelocity(mob.getVelocity().setY(0.5));
							PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, duration, 0, true, false));
							if (getAbilityScore() > 1) {
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, duration + WIND_WALK_VULNERABILITY_DURATION_INCREASE, WIND_WALK_VULNERABILITY_AMPLIFIER, true, false));
							}
						}
						mobsAlreadyHit.add(mob);
					}
				}
				if (mPlayer.isOnGround() || mPlayer.getLocation().getBlock().getType() == Material.WATER || mPlayer.getLocation().getBlock().getType() == Material.LAVA) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSprinting() && mPlayer.getLocation().getPitch() < 50) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				LocationType locType = mPlugin.mSafeZoneManager.getLocationType(mPlayer.getLocation());
				return locType != LocationType.Capital && locType != LocationType.SafeZone;
			}
		}
		return false;
	}

}
