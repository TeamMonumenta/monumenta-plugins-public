package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
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
import com.playmonumenta.plugins.utils.InventoryUtils;

public class DeathsTouch extends Ability {
	private static final int DEATHS_TOUCH_1_COOLDOWN = 30 * 20;
	private static final int DEATHS_TOUCH_2_COOLDOWN = 20 * 20;
	private static final int DEATHS_TOUCH_1_BUFF_DURATION = 15 * 20;
	private static final int DEATHS_TOUCH_2_BUFF_DURATION = 20 * 20;
	private static final int DEATHS_TOUCH_RANGE = 20;

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
		mInfo.cooldown = getAbilityScore() == 1 ? DEATHS_TOUCH_1_COOLDOWN : DEATHS_TOUCH_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;

		/*
		 * NOTE! Because this skill has two events it needs to bypass the automatic cooldown check
		 * and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	private static List<PotionEffectType> getOppositeEffects(LivingEntity e) {
		List<PotionEffectType> types = new ArrayList<PotionEffectType>();
		for (PotionEffect effect : e.getActivePotionEffects()) {
			if (effect.getType().equals(PotionEffectType.WEAKNESS)) {
				types.add(PotionEffectType.INCREASE_DAMAGE);
			} else if (effect.getType().equals(PotionEffectType.SLOW)) {
				types.add(PotionEffectType.SPEED);
			} else if (effect.getType().equals(PotionEffectType.WITHER) || effect.getType().equals(PotionEffectType.POISON)) {
				types.add(PotionEffectType.REGENERATION);
			} else if (effect.getType().equals(PotionEffectType.SLOW_DIGGING)) {
				types.add(PotionEffectType.FAST_DIGGING);
			} else if (effect.getType().equals(PotionEffectType.BLINDNESS)) {
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
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell) || !mPlayer.isSprinting() || !InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			return false;
		}

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1, 0.25f);

		// Get a list of mobs that can possibly be hit - so we don't have to ask the game for nearby mobs every time
		List<Mob> mobsInRange = EntityUtils.getNearbyMobs(loc, DEATHS_TOUCH_RANGE);
		BoundingBox box = BoundingBox.of(loc, 1, 1, 1);
		for (int i = 0; i < DEATHS_TOUCH_RANGE; i++) {
			box.shift(dir);
			Location bloc = box.getCenter().toLocation(mWorld);
			mWorld.spawnParticle(Particle.SPELL_MOB, bloc, 5, 0.15, 0.15, 0.15, 0);
			for (LivingEntity mob : mobsInRange) {
				if (mob.getBoundingBox().overlaps(box)) {
					target = mob;
					loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1, 1f);

					new BukkitRunnable() {
						int runnableDuration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_COOLDOWN : DEATHS_TOUCH_2_COOLDOWN;
						double width = mob.getWidth() / 2;
						int t = 0;

						@Override
						public void run() {
							t++;
							mPlayer.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width, 0);
							mPlayer.spawnParticle(Particle.SPELL_WITCH, mob.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width, 0);
							if (t >= runnableDuration || target == null || target.isDead()) {
								this.cancel();
								target = null;
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);

					// This loop only runs at most once!
					putOnCooldown();
					return true;
				}
			}
		}
		putOnCooldown();
		// Didn't find a mob - Due to lack of skill possessed by the caster, put it on cooldown
		return false;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (target != null && event.getEntity().getUniqueId().equals(target.getUniqueId())) {
			List<PotionEffectType> effects = getOppositeEffects(event.getEntity());
			int duration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_BUFF_DURATION : DEATHS_TOUCH_2_BUFF_DURATION;
			for (PotionEffectType effect : effects) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(effect, duration, 0, true, true));
			}
			target = null;
		}
	}

}
