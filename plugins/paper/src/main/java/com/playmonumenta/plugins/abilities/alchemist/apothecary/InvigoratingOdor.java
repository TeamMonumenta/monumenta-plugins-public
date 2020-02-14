package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * INVIGORATING ODOR:
 * Alchemist Potions do +2 / +4 damage, and leave behind a
 * 3 second aura that gives players Speed I and Haste I for
 * 10 seconds. At level 2, Resistance I is added to the aura.
 */
public class InvigoratingOdor extends Ability {

	private static final int INVIGORATING_1_DAMAGE = 2;
	private static final int INVIGORATING_2_DAMAGE = 4;
	private static final int INVIGORATING_DURATION = 20 * 10;
	private static final int INVIGORATING_AURA_DURATION = 20 * 3;
	private static final int INVIGORATING_RADIUS = 3;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.5f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.5f);

	private int mDamage;

	public InvigoratingOdor(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Invigorating Odor");
		mInfo.scoreboardId = "InvigoratingOdor";
		mDamage = getAbilityScore() == 1 ? INVIGORATING_1_DAMAGE : INVIGORATING_2_DAMAGE;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity le : affectedEntities) {
					if (EntityUtils.isHostileMob(le)) {
						apply(le);
					}
				}
			}

			createAura(potion.getLocation(), INVIGORATING_RADIUS);
		}

		return true;
	}

	public void apply(LivingEntity le) {
		EntityUtils.damageEntity(mPlugin, le, mDamage, mPlayer);
	}

	public void createAura(Location loc, double radius) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= INVIGORATING_AURA_DURATION) {
					this.cancel();
				}

				if (mTicks == 0) {
					mWorld.spawnParticle(Particle.END_ROD, loc, 35, 0.3, 0.3, 0.3, 0.1);
					mWorld.spawnParticle(Particle.SPELL, loc, 35, radius / 2, 0.15, radius / 2);
				}
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.3, 0.3, 0.3, APOTHECARY_DARK_COLOR);
				mWorld.spawnParticle(Particle.END_ROD, loc, 1, radius / 2, 0.15, radius / 2, 0.05);
				mWorld.spawnParticle(Particle.REDSTONE, loc, (int) Math.pow(radius, 2) * 2, radius / 2, 0.15, radius / 2, APOTHECARY_LIGHT_COLOR);

				for (Player player : PlayerUtils.playersInRange(loc, radius)) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, INVIGORATING_DURATION, 0, true, true));
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.FAST_DIGGING, INVIGORATING_DURATION, 0, true, true));
					if (getAbilityScore() > 1) {
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 0, true, true));
					}
				}

				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

}
