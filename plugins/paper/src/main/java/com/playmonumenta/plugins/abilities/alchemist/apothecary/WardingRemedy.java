package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * WARDING REMEDY:
 * Players within a 12 block radius gain 15% / 25% extra melee and ranged damage when
 * at 3 or more absorption health. Shift-RClick with an Alchemist Potion to give
 * players (including yourself) in a 6 block radius 1 absorption health per 0.5
 * seconds for 3 seconds, up to 12 absorption health (Cooldown: 30 / 25 seconds).
 */

public class WardingRemedy extends Ability {

	public static final int WARDING_REMEDY_1_COOLDOWN = 20 * 30;
	public static final int WARDING_REMEDY_2_COOLDOWN = 20 * 25;
	public static final int WARDING_REMEDY_PULSES = 12;
	public static final int WARDING_REMEDY_PULSE_DELAY = 10;
	public static final int WARDING_REMEDY_MAX_ABSORPTION = 12;
	public static final double WARDING_REMEDY_ACTIVE_RADIUS = 6;

	public WardingRemedy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Warding Remedy");
		mInfo.scoreboardId = "WardingRemedy";
		mInfo.linkedSpell = Spells.WARDING_REMEDY;
		mInfo.cooldown = getAbilityScore() == 1 ? WARDING_REMEDY_1_COOLDOWN : WARDING_REMEDY_2_COOLDOWN;
		mInfo.mShorthandName = "WR";
		mInfo.mDescriptions.add("You and allies in a 12 block radius passively gain an additional 15% damage on melee and ranged attacks when at 3 or more absorption health. Shift and right click with an Alchemist Potion to give players (including yourself) within a 6 block radius 1 absorption health per 0.5 seconds for 6 seconds, up to 6 absorption health. Cooldown: 30s.");
		mInfo.mDescriptions.add("The damage bonus is increased to 25%, and cooldown decreased to 25s.");
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.testForItemWithName(mPlayer.getInventory().getItemInMainHand(), "Alchemist's Potion");
	}

	@Override
	public boolean playerThrewSplashPotionEvent(SplashPotion potion) {
		// This is sufficient because we are already checking conditions in runCheck()
		// potion.remove() automatically returns the potion to the player
		potion.remove();
		putOnCooldown();

		new BukkitRunnable() {
			int mPulses = 0;

			@Override
			public void run() {
				mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, (float) Math.pow(1.25, mPulses - 1));
				for (Player p : PlayerUtils.playersInRange(mPlayer, WARDING_REMEDY_ACTIVE_RADIUS, true)) {
					AbsorptionUtils.addAbsorption(p, 1, WARDING_REMEDY_MAX_ABSORPTION);
					mWorld.spawnParticle(Particle.END_ROD, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
				}

				mPulses++;
				if (mPulses >= WARDING_REMEDY_PULSES) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, WARDING_REMEDY_PULSE_DELAY);

		return true;
	}

}
