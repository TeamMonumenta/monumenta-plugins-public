package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.effects.PercentDamageReceived;

public class InvigoratingOdor extends PotionAbility {

	private static final double INVIGORATING_POTION_CHANCE = 0.25;
	private static final int INVIGORATING_1_DAMAGE = 1;
	private static final int INVIGORATING_2_DAMAGE = 2;
	private static final int INVIGORATING_DURATION = 20 * 5;
	private static final int INVIGORATING_AURA_DURATION = 20 * 3;
	private static final double PERCENT_DAMAGE_RESIST = 0.1;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.5f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.5f);

	public InvigoratingOdor(Plugin plugin, Player player) {
		super(plugin, player, "Invigorating Odor", INVIGORATING_1_DAMAGE, INVIGORATING_2_DAMAGE);
		mInfo.mLinkedSpell = Spells.INVIGORATING_ODOR;
		mInfo.mScoreboardId = "InvigoratingOdor";
		mInfo.mShorthandName = "IO";
		mInfo.mDescriptions.add("Alchemist Potions deal +1 damage and leave an aura for 3 seconds where they hit. The aura provides Speed I and Haste I to players within it for 5 seconds. The 50% chance for a potion on ally kills is increased to 75%.");
		mInfo.mDescriptions.add("Alchemist Potions deal +2 damage and the aura additionally grants 10% damage reduction for 5 seconds. The chance for a potion on ally kills is increased to 100%.");
	}

	@Override
	public void createAura(Location loc, double radius) {
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.END_ROD, loc, 35, 0.3, 0.3, 0.3, 0.1);
		world.spawnParticle(Particle.SPELL, loc, 35, radius / 2, 0.15, radius / 2);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				world.spawnParticle(Particle.REDSTONE, loc, 3, 0.3, 0.3, 0.3, APOTHECARY_DARK_COLOR);
				world.spawnParticle(Particle.END_ROD, loc, 1, radius / 2, 0.15, radius / 2, 0.05);
				world.spawnParticle(Particle.REDSTONE, loc, (int) Math.pow(radius, 2) * 2, radius / 2, 0.15, radius / 2, APOTHECARY_LIGHT_COLOR);

				for (Player player : PlayerUtils.playersInRange(loc, radius)) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, INVIGORATING_DURATION, 0, true, true));
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.FAST_DIGGING, INVIGORATING_DURATION, 0, true, true));
					if (getAbilityScore() > 1) {
						mPlugin.mEffectManager.addEffect(mPlayer, "InvigoratingResistance", new PercentDamageReceived(INVIGORATING_DURATION, PERCENT_DAMAGE_RESIST));
					}
				}

				if (mTicks > INVIGORATING_AURA_DURATION) {
					this.cancel();
				}

				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	// Used by NonAlchemistPotionPassive
	public double getPotionChanceBonus() {
		return INVIGORATING_POTION_CHANCE * getAbilityScore();
	}

}
