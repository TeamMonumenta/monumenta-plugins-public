package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Bodyguard: Blocking while looking directly at another player
 * makes you charge to him (max range: 15 blocks, immune to
 * knockback and damage). Upon arriving, you knock back all
 * nearby mobs (radius: 4 blocks). Both you and the other player
 * gain + 4 / + 8 armor and regen I/II for 8 s. At lvl 2, mobs
 * are also stunned for 3 s. Cooldown: 45 / 35s
 */
public class Bodyguard extends Ability {

	public Bodyguard(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Bodyguard";
		mInfo.linkedSpell = Spells.BODYGUARD;
		mInfo.cooldown = 20 * 30;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		Vector dir = oLoc.getDirection();
		List<Player> players = PlayerUtils.getNearbyPlayers(mPlayer.getEyeLocation(), 15);
		players.remove(mPlayer);
		for (int i = 0; i < 15; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(mWorld);
			if (bLoc.getBlock().getType().isSolid()) {
				break;
			}
			for (Player player : players) {
				if (player.getBoundingBox().overlaps(box)) {
					Location loc = mPlayer.getEyeLocation();
					for (int j = 0; j < 45; j++) {
						loc.add(dir.clone().multiply(0.33));
						mWorld.spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0f);
						if (loc.distance(bLoc) < 1) {
							break;
						}
					}
					//Flame
					for (int k = 0; k < 120; k++) {
						double x = ThreadLocalRandom.current().nextDouble(-3, 3);
						double z = ThreadLocalRandom.current().nextDouble(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						mWorld.spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), ThreadLocalRandom.current().nextDouble(0.1, 0.4));
					}

					//Explosion_Normal
					for (int k = 0; k < 60; k++) {
						double x = ThreadLocalRandom.current().nextDouble(-3, 3);
						double z = ThreadLocalRandom.current().nextDouble(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), ThreadLocalRandom.current().nextDouble(0.15, 0.5));
					}

					putOnCooldown();
					mWorld.playSound(oLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
					mPlayer.teleport(player.getLocation().clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0));
					Location tloc = player.getLocation().clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
					mWorld.playSound(tloc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
					mWorld.playSound(tloc, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.9f);
					int bodyguard = getAbilityScore();
					double armor = bodyguard == 1 ? 4 : 8;
					int amp = bodyguard == 1 ? 0 : 1;
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					                                 new PotionEffect(PotionEffectType.REGENERATION,
					                                                  20 * 8,
					                                                  amp, false, true));
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
					                                 new PotionEffect(PotionEffectType.REGENERATION,
					                                                  20 * 8,
					                                                  amp, false, true));
					mPlayer.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
					player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
					new BukkitRunnable() {

						@Override
						public void run() {
							mPlayer.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
							player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
						}

					}.runTaskLater(mPlugin, 20 * 8);
					for (Mob mob : EntityUtils.getNearbyMobs(player.getLocation(), 4)) {
						MovementUtils.KnockAway(player, mob, 0.45f);
						if (bodyguard > 1) {
							EntityUtils.applyStun(mPlugin, 20 * 3, mob);
						}
					}
					return true;
				}
			}
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
