package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

public class DeathsTouch extends Ability {

	/*
	 * Death’s Touch: Sprint + right-click marks the enemy
	 * you are looking at as the reaper’s next victim. If
	 * you kill that enemy with a scythe, you reap its soul,
	 * granting you 15 / 20 s of lvl 1 buffs contrary to the
	 * debuffs affecting it (Weakness -> Strength, Slowness ->
	 * Speed, On Fire -> Fire Resistance, Wither / Poison ->
	 * Regeneration, Mining Fatigue -> Haste, Blindness ->
	 * Night Vision). Cooldown: 30 / 20 s
	 */

	private LivingEntity target = null;
	public DeathsTouch(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DEATHS_TOUCH;
		mInfo.scoreboardId = "DeathsTouch";
		mInfo.cooldown = getAbilityScore() == 1 ? 30 * 20 : 20 * 20;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	public List<PotionEffectType> getOppositeEffects(LivingEntity e) {
		List<PotionEffectType> types = new ArrayList<PotionEffectType>();
		for (PotionEffect effect : e.getActivePotionEffects()) {
			if (effect.getType() == PotionEffectType.WEAKNESS) {
				types.add(PotionEffectType.INCREASE_DAMAGE);
			} else if (effect.getType() == PotionEffectType.SLOW) {
				types.add(PotionEffectType.SPEED);
			} else if (effect.getType() == PotionEffectType.WITHER || effect.getType() == PotionEffectType.POISON) {
				types.add(PotionEffectType.REGENERATION);
			} else if (effect.getType() == PotionEffectType.SLOW_DIGGING) {
				types.add(PotionEffectType.FAST_DIGGING);
			} else if (effect.getType() == PotionEffectType.BLINDNESS) {
				types.add(PotionEffectType.NIGHT_VISION);
			}
		}
		if (e.getFireTicks() > 0) {
			types.add(PotionEffectType.FIRE_RESISTANCE);
		}
		return types;
	}

	@Override
	public boolean cast() {
		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		boolean cancel = false;
		loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1, 0.25f);
		for (int i = 0; i < 20; i++) {
			loc.add(dir);
			mWorld.spawnParticle(Particle.SPELL_MOB, loc, 3, 0.1, 0.1, 0.1);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 0.5)) {
				if (target == null) {
					target = mob;
					cancel = true;
					loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1, 1f);
					new BukkitRunnable() {
						double width = mob.getWidth() / 2;
						int t = 0;
						@Override
						public void run() {
							t++;
							mPlayer.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width);
							mPlayer.spawnParticle(Particle.SPELL_WITCH, mob.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width);
							if (t >= 20 * 15) {
								this.cancel();
								target = null;
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
					break;
				}
			}
			if (cancel) {
				break;
			}
		}
		putOnCooldown();
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (target != null) {
			if (event.getEntity().equals(target)) {
				LivingEntity e = event.getEntity();
				List<PotionEffectType> effects = getOppositeEffects(e);
				int duration = getAbilityScore() == 1 ? 20 * 15 : 20 * 20;
				for (PotionEffectType effect : effects) {
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(effect, duration, 0, true, true));
				}
				target = null;
			}
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSprinting();
	}

}
