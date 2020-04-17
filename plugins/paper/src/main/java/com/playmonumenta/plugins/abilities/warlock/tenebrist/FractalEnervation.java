package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class FractalEnervation extends Ability {

	public static final String FRACTAL_CAP_REMOVED_METAKEY = "FractalAmplifyingHexAmplifierCapRemoved";
	public static final int FRACTAL_AMPLIFYING_HEX_CAP = 4;

	private static final int FRACTAL_INITIAL_RANGE = 9;
	private static final int FRACTAL_DAMAGE = 1;
	private static final int FRACTAL_REFRESH_DURATION = 20 * 6;
	private static final int FRACTAL_FATIGUE_DURATION = 20 * 12;
	private static final int FRACTAL_1_CHAIN_RANGE = 3 + 1; // The +1 accounts for the mob's nonzero hitbox so that the distance between 2 mobs is approx 3 still
	private static final int FRACTAL_2_CHAIN_RANGE = 4 + 1;
	private static final int FRACTAL_1_COOLDOWN = 20 * 12;
	private static final int FRACTAL_2_COOLDOWN = 20 * 10;

	public FractalEnervation(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Fractal Enervation");
		mInfo.scoreboardId = "Fractal";
		mInfo.mShorthandName = "FE";
		mInfo.mDescriptions.add("Right-clicking while not looking down fires a dark magic beam that travels up to 9 blocks. The first enemy hit is afflicted with Mining Fatigue for 12s and takes 1 damage. In addition, all debuffs on the enemy increase by 1 effect level, and have their durations increased to 6 seconds if below 6 seconds. The beam then instantly spreads to all enemies in a 3 block radius, applying the same effects. It will continue spreading until it doesn't find any new targets. Cooldown: 12s.");
		mInfo.mDescriptions.add("The spread radius is increased to 4 blocks. Additionally, Amplifying Hex's extra level cap is increased from 2 to 4 on affected enemies. Cooldown: 10s.");
		mInfo.linkedSpell = Spells.FRACTAL_ENERVATION;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.cooldown = getAbilityScore() == 1 ? FRACTAL_1_COOLDOWN : FRACTAL_2_COOLDOWN;
	}

	private List<LivingEntity> hit = new ArrayList<LivingEntity>();

	@Override
	public void cast(Action action) {
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.9f);
		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 0.7, 0.7, 0.7);
		Vector dir = mPlayer.getEyeLocation().getDirection();
		List<LivingEntity> mobsInInitialRange = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), FRACTAL_INITIAL_RANGE, mPlayer);
		List<LivingEntity> justHit = new ArrayList<LivingEntity>();
		List<LivingEntity> justHitReplacement = new ArrayList<LivingEntity>();
		int chainRange = getAbilityScore() == 1 ? FRACTAL_1_CHAIN_RANGE : FRACTAL_2_CHAIN_RANGE;
		boolean cancel = false;
		for (int i = 0; i < FRACTAL_INITIAL_RANGE; i++) {
			box.shift(dir);
			Location loc = box.getCenter().toLocation(mWorld);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 5, 0.15, 0.15,
			                     0.15, 0.15);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.15, 0.15,
			                     0.15, 0.075);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.1, 0.1, 0.1,
			                     0.1);
			// Find the first hit mob and add it to justHit
			for (LivingEntity mob : mobsInInitialRange) {
				if (mob.getBoundingBox().overlaps(box)) {
					cancel = true;
					hit.add(mob);
					justHit.add(mob);
					// Do 10 chain hit iterations, no risk of infinite loop
					for (int j = 0; j < 10; j++) {
						for (LivingEntity justHitMob : justHit) {
							for (LivingEntity chainMob : EntityUtils.getNearbyMobs(justHitMob.getLocation(), chainRange)) {
								if (!hit.contains(chainMob) && justHitMob.getLocation().distance(chainMob.getLocation()) < chainRange) {
									hit.add(chainMob);
									justHitReplacement.add(chainMob);
								}
							}
						}
						justHit.clear();
						justHit.addAll(justHitReplacement);
						justHitReplacement.clear();
						// Break the loop early if no new mobs hit in new chain iteration
						if (justHit.size() == 0) {
							break;
						}
					}
					break;
				}
			}

			if (loc.getBlock().getType().isSolid() || cancel) {
				break;
			}

		}

		// Apply everything in one go
		for (LivingEntity mob : hit) {
			for (PotionEffectType types : PotionUtils.getNegativeEffects(mob)) {
				PotionEffect effect = mob.getPotionEffect(types);
				mob.removePotionEffect(types);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				mob.addPotionEffect(new PotionEffect(types, Math.max(FRACTAL_REFRESH_DURATION, effect.getDuration()), effect.getAmplifier() + 1));
			}
			mob.setMetadata(FRACTAL_CAP_REMOVED_METAKEY, new FixedMetadataValue(mPlugin, null));
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_DIGGING, FRACTAL_FATIGUE_DURATION, 0));
			EntityUtils.damageEntity(mPlugin, mob, FRACTAL_DAMAGE, mPlayer, MagicType.DARK_MAGIC, true, mInfo.linkedSpell);
			mWorld.spawnParticle(Particle.SPELL_WITCH, mob.getLocation(), 20, 0.25, 0.45, 0.25, 0.15);
			mWorld.spawnParticle(Particle.SPELL_MOB, mob.getLocation(), 10, 0.25, 0.45, 0.25, 0);
		}

		hit.clear();
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.getLocation().getPitch() < 50
				&& InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
