package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class ViciousCombos extends Ability {

	private static final int VICIOUS_COMBOS_RANGE = 5;
	private static final int VICIOUS_COMBOS_EFFECT_DURATION = 15 * 20;
	private static final int VICIOUS_COMBOS_EFFECT_LEVEL = 0;
	private static final int VICIOUS_COMBOS_COOL_1 = 20;
	private static final int VICIOUS_COMBOS_COOL_2 = 2 * 20;
	private static final int VICIOUS_COMBOS_CRIPPLE_DURATION = 5 * 20;
	private static final int VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL = 3;
	private static final double VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL = 0.15;

	public ViciousCombos(Plugin plugin, Player player) {
		super(plugin, player, "Vicious Combos");
		mInfo.mScoreboardId = "ViciousCombos";
		mInfo.mShorthandName = "VC";
		mInfo.mDescriptions.add("Passively, killing an enemy with melee or ability damage refreshes the cooldown of your abilities by 1 second. Killing an Elite enemy instead resets the cooldown of your abilities.");
		mInfo.mDescriptions.add("Killing an enemy now refreshes your ability cooldowns by 2 seconds. Killing an Elite enemy inflicts nearby enemies within 5 blocks with 15% weaken and 15% Vulnerability for 5 seconds.");
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		EntityDamageEvent e = event.getEntity().getLastDamageCause();
		if (e != null
		    && (e.getCause() == DamageCause.ENTITY_ATTACK
			    || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK
			    || e.getCause() == DamageCause.CUSTOM)) {
			LivingEntity killedEntity = event.getEntity();
			int viciousCombos = getAbilityScore();

			//Run the task 1 tick later to let everything go on cooldown (ex. BMB)
			new BukkitRunnable() {

				@Override
				public void run() {
					Location loc = killedEntity.getLocation();
					loc = loc.add(0, 0.5, 0);

					World world = mPlayer.getWorld();
					if (EntityUtils.isElite(killedEntity)) {
						mPlugin.mTimers.removeAllCooldowns(mPlayer);
						MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "All your cooldowns have been reset");

						if (viciousCombos > 1) {
							for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, VICIOUS_COMBOS_RANGE, mPlayer)) {
								world.spawnParticle(Particle.SPELL_MOB, mob.getLocation().clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0);
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL, true, false));
								EntityUtils.applyWeaken(mPlugin, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL, mob);
							}
						}

						world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2, 0.5f);
						world.spawnParticle(Particle.CRIT, loc, 500, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 500, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25);
						world.spawnParticle(Particle.SWEEP_ATTACK, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
						world.spawnParticle(Particle.SPELL_MOB, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
					} else if (EntityUtils.isHostileMob(killedEntity)) {
						int timeReduction = (viciousCombos == 1) ? VICIOUS_COMBOS_COOL_1 : VICIOUS_COMBOS_COOL_2;
						if (killedEntity instanceof Player) {
							timeReduction *= 2;
						}

						mPlugin.mTimers.updateCooldowns(mPlayer, timeReduction);

						world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
						world.spawnParticle(Particle.CRIT, loc, 50, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 50, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25);
						world.spawnParticle(Particle.SWEEP_ATTACK, loc, 30, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
						world.spawnParticle(Particle.SPELL_MOB, loc, 30, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
					}

				}

			}.runTaskLater(mPlugin, 1);
		}
	}
}
