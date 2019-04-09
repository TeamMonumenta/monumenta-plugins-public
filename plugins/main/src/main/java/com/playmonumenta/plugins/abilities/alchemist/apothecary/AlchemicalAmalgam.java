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
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Alchemical Amalgam: Shift - left clicking with a bow in hand
 * causes the apothecary to launch an orb of positive and negative
 * concoctions, this slow moving projectile gives allies within 5
 * blocks of it regeneration II/III and Resistance I and giving
 * enemies slowness I/II and Weakness I for 4 seconds. The projectile
 * on collision with terrain heals allies within 5 blocks of it
 * for 2 / 4 hp and deals 10 damage to mobs. CD: 30s
 */
public class AlchemicalAmalgam extends Ability {

	private static final int ALCHEMICAL_1_SLOWNESS_AMPLIFIER = 1;
	private static final int ALCHEMICAL_2_SLOWNESS_AMPLIFIER = 2;
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
	public boolean cast() {
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
			double heal = getAbilityScore() == 1 ? 2 : 4;
			@Override
			public void run() {
				loc.add(dir.clone().multiply(0.3));
				t++;
				mWorld.spawnParticle(Particle.REDSTONE, loc, 25, 0.25, 0.25, 0.25, ALCHEMICAL_LIGHT_COLOR);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 25, 0.25, 0.25, 0.25, ALCHEMICAL_DARK_COLOR);
				for (Player p : PlayerUtils.getNearbyPlayers(loc, 5)) {
					mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
					                                 new PotionEffect(PotionEffectType.REGENERATION, 20 * 3,
					                                                  amp, true, true));
					mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
					                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3,
					                                                  0, true, true));
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 5, mPlayer)) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 4, slownessAmplifier));
					mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 4, 0));
				}
				if (t >= 20 * 6 || loc.getBlock().getType().isSolid()) {
					this.cancel();
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 150, 0.1, 0.1, 0.1, 0.2);
					mPlayer.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
					for (Player p : PlayerUtils.getNearbyPlayers(loc, ALCHEMICAL_EXPLOSION_RADIUS)) {
						PlayerUtils.healPlayer(p, heal);
					}
					for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), ALCHEMICAL_EXPLOSION_RADIUS)) {
						EntityUtils.damageEntity(mPlugin, le, ALCHEMICAL_DAMAGE, mPlayer);;
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && InventoryUtils.isBowItem(inMainHand);
	}

}
