package com.playmonumenta.plugins.abilities.other;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.enchantments.evasions.SecondWind;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;

public class EvasionEnchant extends Ability {

	private static final int SECOND_WIND_IFRAMES = 20 * 2;
	private static final int THRESHOLD_INTERVAL = 5;
	private static final double DAMAGE_REDUCTION_INTERVAL = 0.2;
	// Threshold -> Damage Reduction pairs
	private static final Map<Integer, Double> EVASION_DAMAGE_REDUCTION = new LinkedHashMap<Integer, Double>();

	static {
		// Reverse order checking evasion thresholds
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 4, DAMAGE_REDUCTION_INTERVAL * 4);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 3, DAMAGE_REDUCTION_INTERVAL * 3);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 2, DAMAGE_REDUCTION_INTERVAL * 2);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 1, DAMAGE_REDUCTION_INTERVAL * 1);
	}

	public int mCounter = 0;

	public EvasionEnchant(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		evade(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		evade(event);
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		double damage = event.getDamage();
		Location loc = mPlayer.getLocation().add(0, 1, 0);

		for (Map.Entry<Integer, Double> entry : EVASION_DAMAGE_REDUCTION.entrySet()) {
			int threshold = entry.getKey();
			double damageReduction = entry.getValue();

			if (mCounter >= threshold) {
				mCounter -= threshold;
				event.setDamage(damage * (1 - damageReduction));

				if (PlayerTracking.getInstance().getPlayerCustomEnchantLevel(mPlayer, SecondWind.class) > 0
						&& mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) < EntityUtils.getRealFinalDamage(event)) {
					mCounter -= THRESHOLD_INTERVAL;
					event.setDamage(damage * (1 - damageReduction - DAMAGE_REDUCTION_INTERVAL));
					mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, threshold * 0.1f, 1.5f);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.1f, 2f);

					new BukkitRunnable() {
						@Override
						public void run() {
							mPlayer.setNoDamageTicks(SECOND_WIND_IFRAMES);
						}
					}.runTaskLater(mPlugin, 1);
				}

				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				Bukkit.getPluginManager().callEvent(new EvasionEvent(mPlayer, event.getDamage()));
			}
		}
	}

	// The code is exactly the same, but BossAbilityDamageEvent doesn't extend EntityDamageByEntityEvent, so can't call them together
	private void evade(EntityDamageByEntityEvent event) {
		double damage = event.getDamage();
		Location loc = mPlayer.getLocation().add(0, 1, 0);

		for (Map.Entry<Integer, Double> entry : EVASION_DAMAGE_REDUCTION.entrySet()) {
			int threshold = entry.getKey();
			double damageReduction = entry.getValue();

			if (mCounter >= threshold) {
				mCounter -= threshold;
				event.setDamage(damage * (1 - damageReduction));

				if (PlayerTracking.getInstance().getPlayerCustomEnchantLevel(mPlayer, SecondWind.class) > 0
						&& mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) < EntityUtils.getRealFinalDamage(event)) {
					mCounter -= THRESHOLD_INTERVAL;
					event.setDamage(damage * (1 - damageReduction - DAMAGE_REDUCTION_INTERVAL));
					mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, threshold * 0.1f, 1.5f);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.1f, 2f);

					new BukkitRunnable() {
						@Override
						public void run() {
							mPlayer.setNoDamageTicks(SECOND_WIND_IFRAMES);
						}
					}.runTaskLater(mPlugin, 1);
				}

				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				Bukkit.getPluginManager().callEvent(new EvasionEvent(mPlayer, event.getDamage()));
			}
		}
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

}
