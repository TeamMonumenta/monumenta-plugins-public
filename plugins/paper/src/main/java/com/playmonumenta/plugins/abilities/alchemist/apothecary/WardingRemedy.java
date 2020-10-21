package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class WardingRemedy extends Ability {

	public static final int WARDING_REMEDY_1_COOLDOWN = 20 * 30;
	public static final int WARDING_REMEDY_2_COOLDOWN = 20 * 25;
	public static final int WARDING_REMEDY_PULSES = 12;
	public static final int WARDING_REMEDY_PULSE_DELAY = 10;
	public static final int WARDING_REMEDY_MAX_ABSORPTION = 6;
	public static final int WARDING_REMEDY_ABSORPTION_DURATION = 20 * 30;
	public static final double WARDING_REMEDY_ACTIVE_RADIUS = 6;
	private static final Color APOTHECARY_LIGHT_COLOR = Color.fromRGB(255, 255, 100);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.5f);

	public WardingRemedy(Plugin plugin, Player player) {
		super(plugin, player, "Warding Remedy");
		mInfo.mScoreboardId = "WardingRemedy";
		mInfo.mLinkedSpell = Spells.WARDING_REMEDY;
		mInfo.mCooldown = getAbilityScore() == 1 ? WARDING_REMEDY_1_COOLDOWN : WARDING_REMEDY_2_COOLDOWN;
		mInfo.mShorthandName = "WR";
		mInfo.mDescriptions.add("You and allies in a 12 block radius passively gain an additional 15% damage on melee and ranged attacks when at 3 or more absorption health. Shift and right click with an Alchemist Potion to give players (including yourself) within a 6 block radius 1 absorption health per 0.5 seconds for 6 seconds, lasting 30 seconds, up to 6 absorption health. Cooldown: 30s.");
		mInfo.mDescriptions.add("The damage bonus is increased to 25%, and cooldown decreased to 25s.");
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.testForItemWithName(mPlayer.getInventory().getItemInMainHand(), "Alchemist's Potion");
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		// This is sufficient because we are already checking conditions in runCheck()
		// potion.remove() automatically returns the potion to the player
		potion.remove();
		putOnCooldown();

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1f, 1.5f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1.5f);

		world.spawnParticle(Particle.END_ROD, mPlayer.getLocation().clone().add(0, 1, 0), 50, 0.25, 0.25, 0.25, 0.2);
		world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation(), 80, 2.8, 2.8, 2.8, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 3.0f));
		world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 1, 0), 40, 0.35, 0.5, 0.35, APOTHECARY_DARK_COLOR);
		world.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 60, 0.25, 0.25, 0.25, 0.2);
		world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(0, 0.15, 0), 100, 2.8, 0, 2.8, APOTHECARY_DARK_COLOR);

		new BukkitRunnable() {
			int mPulses = 0;
			int mTick = 10;
			@Override
			public void run() {
				world.spawnParticle(Particle.END_ROD, mPlayer.getLocation().add(0, 0.5, 0), 1, 0.35, 0.15, 0.35, 0.05);

				if (mTick >= WARDING_REMEDY_PULSE_DELAY) {
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.7f, 2f);

					for (int i = 0; i < 50; i++) {
						world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().clone().add(6 * FastUtils.sin(i / 25.0 * Math.PI), 0.15, 6 * FastUtils.cos(i / 25.0 * Math.PI)), 1, 0, 0, 0, APOTHECARY_DARK_COLOR);
					}
					world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().clone().add(0, 0.15, 0), 15, 2.8, 0, 2.8, 0);
					world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation(), 40, 2.8, 2.8, 2.8, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 1.5f));
					world.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 2.8, 2.8, 2.8, 0);

					for (Player p : PlayerUtils.playersInRange(mPlayer, WARDING_REMEDY_ACTIVE_RADIUS, true)) {
						AbsorptionUtils.addAbsorption(p, 1, WARDING_REMEDY_MAX_ABSORPTION, WARDING_REMEDY_ABSORPTION_DURATION);

						world.spawnParticle(Particle.REDSTONE, p.getLocation().clone().add(0, 0.5, 0), 10, 0.35, 0.15, 0.35, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 1.0f));
						world.spawnParticle(Particle.SPELL, p.getLocation().clone().add(0, 0.5, 0), 5, 0.35, 0.15, 0.35, 0);
					}
					mTick = 0;
					mPulses++;
					if (mPulses >= WARDING_REMEDY_PULSES) {
						this.cancel();
					}
				}

				mTick++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

}
