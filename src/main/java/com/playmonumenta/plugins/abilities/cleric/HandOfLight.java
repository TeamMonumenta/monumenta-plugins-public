package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class HandOfLight extends Ability {

	private static final int HEALING_RADIUS = 12;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int HEALING_1_COOLDOWN = 14 * 20;
	private static final int HEALING_2_COOLDOWN = 10 * 20;

	public HandOfLight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
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
		boolean healCaster = AbilityManager.getManager().isPvPEnabled(mPlayer);
		for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, HEALING_RADIUS, healCaster)) {
			double healAmount = healing == 1 ? 2 + (p.getMaxHealth() * 0.1) : 4 + (p.getMaxHealth() * 0.2);
			Vector toMobVector = p.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();

			// Only heal players in the correct direction
			// Only heal players that have their class disabled (so it doesn't work on arena contenders)
			// Don't heal players with PvP enabled
			// If the source player was included (because PvP is on), heal them
			if ((playerDir.dot(toMobVector) > HEALING_DOT_ANGLE
			     && !p.getScoreboardTags().contains("disable_class")
				 && !AbilityManager.getManager().isPvPEnabled(mPlayer))
			    || p.equals(mPlayer)) {

				PlayerUtils.healPlayer(p, healAmount);

				Location loc = p.getLocation();
				mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
				                                 new PotionEffect(PotionEffectType.REGENERATION, 20 * 4, 1, true, true));
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
		// Must be sneaking
		if (!mPlayer.isSneaking()) {
			return false;
		}

		// Must be holding a shield
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (offHand == null || offHand.getType() != Material.SHIELD
		    && (mainHand == null || mainHand.getType() != Material.SHIELD)) {
			return false;
		}

		// Must not match conditions for cleansing rain
		Ability cleansing = AbilityManager.getManager().getPlayerAbility(mPlayer, CleansingRain.class);
		if (cleansing != null && cleansing.runCheck()) {
			return false;
		}

		return mPlayer.getLocation().getPitch() > -50 && mPlayer.getLocation().getPitch() < 50;
	}

}
