package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Fractal Enervation: Sprint right-click fires a dark magic beam
 * (max range: 9), afflicting the first enemy it hits with an
 * unnatural weakness that lasts 12s. The beam then instantly
 * spreads to all enemies in a 3-block radius, and then from them,
 * and so on. Affected enemies suffer from Vulnerability I / II
 * and all debuffs on the enemies increase by 1 effect level.
 * Cooldown: 16s / 13s
 */
public class FractalEnervation extends Ability {

	public FractalEnervation(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Fractal";
		mInfo.linkedSpell = Spells.FRACTAL_ENERVATION;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.cooldown = getAbilityScore() == 1 ? 20 * 16 : 20 * 13;
	}

	private List<Mob> hit = new ArrayList<Mob>();

	@Override
	public boolean cast() {
		hit.clear();
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.9f);
		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 0.7, 0.7, 0.7);
		Vector dir = mPlayer.getEyeLocation().getDirection();
		List<Mob> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), 9);
		int level = getAbilityScore() == 1 ? 1 : 3;
		int range = 9;
		boolean cancel = false;
		for (int i = 0; i < range; i++) {
			box.shift(dir);
			Location loc = box.getCenter().toLocation(mWorld);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0.15, 0.15,
			                     0.15, 0.15);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.15, 0.15,
			                     0.15, 0.075);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.1, 0.1, 0.1,
			                     0.1);
			for (Mob mob : mobs) {
				if (mob.getBoundingBox().overlaps(box)) {
					if (!hit.contains(mob)) {
						hit.add(mob);
						for (PotionEffectType types : PotionUtils.getNegativeEffects(mob)) {
							PotionEffect effect = mob.getPotionEffect(types);
							mob.removePotionEffect(types);
							mob.addPotionEffect(
							    new PotionEffect(types, effect.getDuration(), effect.getAmplifier() + 1));
						}
						mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 5, level));
						mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 40, 0.25, 0.45, 0.25, 0.15);
						mWorld.spawnParticle(Particle.SPELL_MOB, loc, 20, 0.25, 0.45, 0.25, 0);
						i = 0;
						Mob nextMob = null;
						double dist = 100;
						for (Mob next : EntityUtils.getNearbyMobs(mob.getLocation(), 4)) {
							if (next.getLocation().distance(loc) < dist && !next.getUniqueId().equals(mob.getUniqueId())
							    && !hit.contains(next)) {
								nextMob = next;
								dist = next.getLocation().distance(loc);
							}
						}
						if (nextMob != null) {
							Vector to = LocationUtils
							            .getDirectionTo(nextMob.getLocation().add(0, nextMob.getHeight() / 2, 0), loc);
							dir = to;
							range = 4;
						} else {
							cancel = true;
						}
						break;
					}
				}
			}

			if (loc.getBlock().getType().isSolid() || cancel) {
				break;
			}

		}
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && InventoryUtils.isScytheItem(mHand);
	}

}
