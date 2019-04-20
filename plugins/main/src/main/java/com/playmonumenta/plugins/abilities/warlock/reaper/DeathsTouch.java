package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DeathsTouch extends Ability {
	private static final int DEATHS_TOUCH_1_COOLDOWN = 30 * 20;
	private static final int DEATHS_TOUCH_2_COOLDOWN = 20 * 20;
	private static final int DEATHS_TOUCH_1_BUFF_DURATION = 15 * 20;
	private static final int DEATHS_TOUCH_2_BUFF_DURATION = 20 * 20;
	private static final int DEATHS_TOUCH_RANGE = 20;

	/*
	 * Death’s Touch: Sprint + right-click marks the enemy
	 * you are looking at as the reaper’s next victim.
	 * The player that kills the mob reaps its soul,
	 * granting them 15 / 20 s of lvl 1 buffs contrary to the
	 * debuffs affecting it (Weakness -> Strength, Slowness ->
	 * Speed, On Fire -> Fire Resistance, Wither / Poison ->
	 * Regeneration, Mining Fatigue -> Haste, Blindness ->
	 * Night Vision). Cooldown: 30 / 20 s
	 */

	// Although we now track the mob buffs on kill with metadata,
	// We still need this variable to easily apply particle effects
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

	@Override
	public boolean cast() {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell) || !mPlayer.isSprinting() || !InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			return false;
		}

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.25f);

		// Get a list of mobs that can possibly be hit - so we don't have to ask the game for nearby mobs every time
		List<LivingEntity> mobsInRange = EntityUtils.getNearbyMobs(loc, DEATHS_TOUCH_RANGE, mPlayer);
		BoundingBox box = BoundingBox.of(loc, 1, 1, 1);
		for (int i = 0; i < DEATHS_TOUCH_RANGE; i++) {
			box.shift(dir);
			Location bloc = box.getCenter().toLocation(mWorld);
			mWorld.spawnParticle(Particle.SPELL_MOB, bloc, 5, 0.15, 0.15, 0.15, 0);
			for (LivingEntity mob : mobsInRange) {
				if (mob.getBoundingBox().overlaps(box)) {
					target = mob;
					int duration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_BUFF_DURATION : DEATHS_TOUCH_2_BUFF_DURATION;
					mob.setMetadata("DeathsTouchBuffDuration", new FixedMetadataValue(mPlugin, duration));
					loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);

					new BukkitRunnable() {
						int runnableDuration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_COOLDOWN : DEATHS_TOUCH_2_COOLDOWN;
						double width = mob.getWidth() / 2;
						int t = 0;

						@Override
						public void run() {
							t++;
							mPlayer.spawnParticle(Particle.SPELL_MOB, target.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width, 0);
							mPlayer.spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, mob.getHeight() / 2, 0), 1, width, width, width, 0);
							if (t >= runnableDuration || target.isDead()) {
								this.cancel();
								target = null;
								target.removeMetadata("DeathsTouchBuffDuration", mPlugin);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);

					// This loop only runs at most once!
					putOnCooldown();
					return true;
				}
			}
		}
		if (mInfo.linkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
				mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell, 20 * 5);
				PlayerUtils.callAbilityCastEvent(mPlayer, mInfo.linkedSpell);
			}
		}
		return false;
	}

	// The buffs will be applied in the DeathsTouchNonReaper ability for all players

}
