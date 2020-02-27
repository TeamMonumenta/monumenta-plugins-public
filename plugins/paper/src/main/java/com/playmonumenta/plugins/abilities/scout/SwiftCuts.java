package com.playmonumenta.plugins.abilities.scout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * [Swift Cuts Level 1] : On melee hit the target is marked.
 * When the target is hit again they take an additional 3 damage,
 * remove the mark, and get 10% Vulnerability for 2 seconds.
 * Targets can only be marked each again after 3s
 *
 * [Swift Cuts Level 2] : The damage is increased to 5 and the
 * Vulnerability is increased to 20%
 */
public class SwiftCuts extends Ability {

	public static class Counter {
		public LivingEntity mMob;
		public int mTicksLeftRefractory = SWIFT_CUTS_REFRACTORY;
		public boolean mIsMarked = true;

		public Counter(LivingEntity mob) {
			mMob = mob;
		}
	}

	private static final int SWIFT_CUTS_1_VULNERABILITY_AMPLIFIER = 1;
	private static final int SWIFT_CUTS_2_VULNERABILITY_AMPLIFIER = 3;
	private static final int SWIFT_CUTS_VULNERABILITY_DURATION = 20 * 2;
	private static final int SWIFT_CUTS_REFRACTORY = 20 * 3;
	private static final int SWIFT_CUTS_1_DAMAGE = 3;
	private static final int SWIFT_CUTS_2_DAMAGE = 5;

	private Map<UUID, Counter> mMarkedMobs = new HashMap<UUID, Counter>();
	private int mVulnerabilityAmplifier;
	private int mDamageBonus;

	public SwiftCuts(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Swift Cuts");
		mInfo.scoreboardId = "SwiftCuts";
		mInfo.mShorthandName = "SC";
		mInfo.mDescriptions.add("On a melee hit you mark your target. When that target is hit again the mark is removed, they take 3 extra damage, and they get 10% Vulnerability for 2 seconds. You can not remark a target within 3 seconds of it having your mark removed.");
		mInfo.mDescriptions.add("Effects increased to 5 damage and 20% Vulnerability.");
		mVulnerabilityAmplifier = getAbilityScore() == 1 ? SWIFT_CUTS_1_VULNERABILITY_AMPLIFIER : SWIFT_CUTS_2_VULNERABILITY_AMPLIFIER;
		mDamageBonus = getAbilityScore() == 1 ? SWIFT_CUTS_1_DAMAGE : SWIFT_CUTS_2_DAMAGE;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			Iterator<Map.Entry<UUID, Counter>> iter = mMarkedMobs.entrySet().iterator();
			while (iter.hasNext()) {
				Counter counter = iter.next().getValue();
				if (!counter.mIsMarked) {
					counter.mTicksLeftRefractory -= 5;
				}

				mWorld.spawnParticle(Particle.FALLING_DUST, counter.mMob.getLocation().add(0, 1, 0), 1, 0.35, 0.45, 0.35, Material.GRAVEL.createBlockData());

				if (counter.mTicksLeftRefractory <= 0 || counter.mMob.isDead() || !counter.mMob.isValid()) {
					iter.remove();
				}
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity() instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) event.getEntity();
			UUID uuid = mob.getUniqueId();

			/*
			 * If the mob is not in the map, mark it.
			 *
			 * If the mob is in the map and marked, apply effects and unmark it.
			 *
			 * If the mob is in the map and unmarked, do nothing, as this indicates the mob is "on cooldown".
			 * The timer will remove the mob from the map once it is allowed to be marked again.
			 */
			if (!mMarkedMobs.containsKey(uuid)) {
				mMarkedMobs.put(uuid, new Counter(mob));
			} else {
				Counter counter = mMarkedMobs.get(uuid);
				if (counter.mIsMarked) {
					Location loc = mob.getLocation().add(0, 1, 0);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.35, 0.45, 0.35, 0.001);
					counter.mIsMarked = false;
					event.setDamage(event.getDamage() + mDamageBonus);
					PotionUtils.applyPotion(mPlayer, mob,
											new PotionEffect(PotionEffectType.UNLUCK, SWIFT_CUTS_VULNERABILITY_DURATION, mVulnerabilityAmplifier));
				}
			}
		}
		return true;
	}

}
