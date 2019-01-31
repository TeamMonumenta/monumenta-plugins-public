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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Thurible Procession: 
 * Level 1 - Blocking with a shield in either hand 
 * procedurally builds up passive buffs, which are applied to all players 
 * (except the user) within 20 blocks for as long as the block is held. 
 * The Hierophant moves at ~80% normal walking speed when blocking to use 
 * this ability. Progression: Speed 1 (0:01) (2 Seconds of Blocking), 
 * Strength 1 (0:01) (4 seconds of blocking), Resistance 1 (0:01) (6 seconds 
 * of blocking) 
 * Level 2 - The radius of the buffs increase to 30, and the 
 * Hiero moves at normal walk speed while blocking. Progression: In addition 
 * to the previous progression, after blocking for 10 seconds, all players 
 * (including the user) within the range are given 10 seconds of Absorption 
 * 1, Speed 1, Strength 1, and Resistance 1.
 */
public class ThuribleProcession extends Ability {

	private static final Particle.DustOptions THURIBLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 175), 1.0f);
	public static final String PLAYER_THURIBLE_METAKEY = "PlayerIncensedThuribleMetakey";
	
	public ThuribleProcession(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Thurible";
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}
	
	@Override
	public boolean cast() {
		int incensedThurible = getAbilityScore();
		Player player = mPlayer;
		if (!player.hasMetadata(PLAYER_THURIBLE_METAKEY)) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1);
			player.setMetadata(PLAYER_THURIBLE_METAKEY, new FixedMetadataValue(mPlugin, 0));
			new BukkitRunnable() {
				int t = 0;
				int seconds = 0;
				double rot = 0;
				int strands = 3;
				double rotAdd = 360 / strands;
				double radius = 3;
				@Override
				public void run() {
					t++;
					rot += 10;
					radius -= 0.1428;
					Location ploc = player.getLocation();
					for (int i = 0; i < strands; i++) {
						double radian = Math.toRadians(rot + (rotAdd * i));
						ploc.add(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
						mWorld.spawnParticle(Particle.REDSTONE, ploc, 15, 0.1, 0.1, 0.1, THURIBLE_COLOR);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, ploc, 5, 0.1f, 0.1f, 0.1f, 0);
						ploc.subtract(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
					}
					if (t > 20) {
						radius = 3;
						t = 0;
						seconds++;
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.85f, 1.15f);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1.15, 40), 15, 2, 0.4f, 2, 0);
					}

					int radius = incensedThurible == 1 ? 20 : 30;
					int duration = 20 * 5;
					List<Player> players = PlayerUtils.getNearbyPlayers(player.getLocation(), radius);
					if (incensedThurible < 2) {
						if (players.contains(player) && seconds < 10) {
							players.remove(player);
						}
					}
					if (seconds >= 2) {
						for (Player pl : players) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true));
						}
					}

					if (seconds >= 4) {
						for (Player pl : players) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, true, true));
						}
					}

					if (seconds >= 6) {
						for (Player pl : players) {
							mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0, true, true));
						}
					}
					if (incensedThurible > 1) {
						if (seconds >= 10) {
							for (Player pl : players) {
								mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, true, true));
							}
						}
					}
					if (t >= 1) {
						if (player.isDead()) {
							this.cancel();
							player.removeMetadata(PLAYER_THURIBLE_METAKEY, mPlugin);
						}
						if ((!player.isHandRaised() && !player.isBlocking())) {
							this.cancel();
							player.removeMetadata(PLAYER_THURIBLE_METAKEY, mPlugin);
						}
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mHand.getType() == Material.SHIELD || oHand.getType() == Material.SHIELD;
	}
	
}
