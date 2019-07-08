package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Alchemical Amalgam: Shift - left clicking with a bow in hand
 * causes the apothecary to launch an orb of positive and negative
 * concoctions, this slow moving projectile gives allies within 5
 * blocks of it 1/2 HP per second and Resistance I and giving
 * enemies slowness I/II and Weakness I for 4 seconds. If the
 * projectile passes through an enemy they will get stunned
 * for 2 / 4 seconds.The projectile on collision with terrain
 * heals allies within 5 blocks of it for 2 / 4 hp and,
 * at level 2, deals 10 damage to mobs. CD: 30s
 */
public class AlchemicalAmalgam extends Ability {

	private static final int ALCHEMICAL_1_SLOWNESS_AMPLIFIER = 1;
	private static final int ALCHEMICAL_2_SLOWNESS_AMPLIFIER = 2;
	private static final int ALCHEMICAL_1_WEAKNESS_AMPLIFIER = 1;
	private static final int ALCHEMICAL_2_WEAKNESS_AMPLIFIER = 2;
	private static final int STUN_DURATION_1 = 2 * 20;
	private static final int STUN_DURATION_2 = 4 * 20;
	private static final int HEAL_PER_SECOND_1 = 1;
	private static final int HEAL_PER_SECOND_2 = 2;
	private static final int ALCHEMICAL_DAMAGE = 10;
	private static final double ALCHEMICAL_EXPLOSION_RADIUS = 5;
	private static final Particle.DustOptions ALCHEMICAL_LIGHT_COLOR = new Particle.DustOptions(
	    Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions ALCHEMICAL_DARK_COLOR = new Particle.DustOptions(
	    Color.fromRGB(83, 0, 135), 1.0f);

	public AlchemicalAmalgam(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Alchemical";
		mInfo.cooldown = 20 * 30;
		mInfo.linkedSpell = Spells.ALCHEMICAL_AMALGAM;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast() {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 1.25f);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);
		mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);
		new BukkitRunnable() {
			Location loc = mPlayer.getEyeLocation();
			Vector dir = loc.getDirection();
			int t = 0;
			int amp = getAbilityScore() == 1 ? 1 : 2;
			int slownessAmplifier = getAbilityScore() == 1 ? ALCHEMICAL_1_SLOWNESS_AMPLIFIER : ALCHEMICAL_2_SLOWNESS_AMPLIFIER;
			int weaknessAmplifier = getAbilityScore() == 1 ? ALCHEMICAL_1_WEAKNESS_AMPLIFIER : ALCHEMICAL_2_WEAKNESS_AMPLIFIER;
			int stunDuration = getAbilityScore() == 1 ? STUN_DURATION_1 : STUN_DURATION_2;
			int healPerSecond = getAbilityScore() == 1 ? HEAL_PER_SECOND_1 : HEAL_PER_SECOND_2;
			double heal = getAbilityScore() == 1 ? 2 : 4;

			double degree = 0;
			@Override
			public void run() {
				loc.add(dir.clone().multiply(0.2));
				degree += 12;
				Vector vec;
				for (int i = 0; i < 2; i++) {
					double radian1 = Math.toRadians(degree + (i * 180));
					vec = new Vector(Math.cos(radian1) * 0.325, 0, Math.sin(radian1) * 0.325);
					vec = VectorUtils.rotateXAxis(vec, -loc.getPitch() + 90);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, ALCHEMICAL_LIGHT_COLOR);
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, ALCHEMICAL_DARK_COLOR);
				}

				mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.35, 0.35, 0.35, 1);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0.35, 0.35, 0.35, 1);

				for (Player p : PlayerUtils.getNearbyPlayers(loc, 5)) {
					if (t % 20 == 0) {
						PlayerUtils.healPlayer(p, healPerSecond);
					}
					mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
					                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3,
					                                                  0, true, true));
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 0.9, mPlayer)) {
					EntityUtils.applyStun(mPlugin, stunDuration, mob);
				}
				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 5, mPlayer)) {
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, 20 * 4, slownessAmplifier));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, 20 * 4, weaknessAmplifier));
				}
				if (t >= 20 * 6 || LocationUtils.collidesWithSolid(loc, loc.getBlock())) {
					this.cancel();
					mWorld.spawnParticle(Particle.SQUID_INK, loc, 20, 0.1, 0.1, 0.1, 0.35);
					mWorld.spawnParticle(Particle.SPIT, loc, 40, 0.1, 0.1, 0.1, 0.3);
					mPlayer.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1, 1.25f);
					for (Player p : PlayerUtils.getNearbyPlayers(loc, ALCHEMICAL_EXPLOSION_RADIUS)) {
						PlayerUtils.healPlayer(p, heal);
					}
					if (getAbilityScore() > 1) {
						for (LivingEntity le : EntityUtils.getNearbyMobs(loc, ALCHEMICAL_EXPLOSION_RADIUS)) {
							EntityUtils.damageEntity(mPlugin, le, ALCHEMICAL_DAMAGE, mPlayer);;
						}
					}
				}
				t++;
			}

		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && InventoryUtils.isBowItem(inMainHand);
	}

}
