package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class HandOfLight extends Ability {

	private static final int HEALING_RADIUS = 12;
	private static final int HEALING_1_HEAL = 10;
	private static final int HEALING_2_HEAL = 16;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int HEALING_1_COOLDOWN = 20 * 20;
	private static final int HEALING_2_COOLDOWN = 15 * 20;

	public HandOfLight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.HEALING;
		mInfo.scoreboardId = "Healing";
		mInfo.cooldown = getAbilityScore() == 1 ? HEALING_1_COOLDOWN : HEALING_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}
	@Override
	public boolean cast() {
		int healing = getAbilityScore();
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		World world = mPlayer.getWorld();
		int healAmount = healing == 1 ? HEALING_1_HEAL : HEALING_2_HEAL;

		for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, HEALING_RADIUS, false)) {
			Vector toMobVector = p.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			// Only heal players in the correct direction
			// Only heal players that have a class score > 0 (so it doesn't work on arena contenders)
			if (playerDir.dot(toMobVector) > HEALING_DOT_ANGLE && ScoreboardUtils.getScoreboardValue(mPlayer, "Class") > 0) {
				PlayerUtils.healPlayer(p, healAmount);

				Location loc = p.getLocation();

				world.spawnParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				world.spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
				mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);
			}
		}

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);

		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, HEALING_RADIUS, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, HEALING_DOT_ANGLE);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		//TODO Find a way to make HoL and Cleansing Rain not cast at the same tim
		if (mPlayer.isSneaking()) {
			return (offHand != null && offHand.getType() == Material.SHIELD) || (mainHand != null && mainHand.getType() == Material.SHIELD);
		}
		return false;
	}

}
