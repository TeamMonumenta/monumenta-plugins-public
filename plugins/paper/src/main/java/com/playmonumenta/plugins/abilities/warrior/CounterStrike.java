package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CounterStrike extends Ability {

	private static final double COUNTER_STRIKE_1_REFLECT = 0.2;
	private static final double COUNTER_STRIKE_2_REFLECT = 0.4;
	private static final float COUNTER_STRIKE_RADIUS = 3.0f;
	private static final int REDUCTION_DURATION = 10 * 20;
	private static final double DAMAGE_REDUCTION_PER_STACK = 0.05;
	private static final int MAX_STACKS = 3;

	public static final String CHARM_DAMAGE = "Counter Strike Damage";
	public static final String CHARM_RADIUS = "Counter Strike Radius";
	public static final String CHARM_DURATION = "Counter Strike Duration";
	public static final String CHARM_DAMAGE_REDUCTION = "Counter Strike Damage Reduction";
	public static final String CHARM_STACKS = "Counter Strike Stacks";

	private final double mReflect;
	private final HashMap<LivingEntity, Integer> mLastDamageTime = new HashMap<>();
	private final HashMap<LivingEntity, Integer> mStacks = new HashMap<>();

	public CounterStrike(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Counter Strike");
		mInfo.mScoreboardId = "CounterStrike";
		mInfo.mShorthandName = "CS";
		mInfo.mDescriptions.add("When you take melee damage, deal melee damage equal to 20% of pre-mitigation damage taken to all mobs in a 3 block radius.");
		mInfo.mDescriptions.add("The damage is increased to 40% of pre-mitigation damage.");
		mInfo.mDescriptions.add("When this ability activates, gain 5% damage reduction against future melee damage from the mob that activated it for 10 seconds. This effect stacks up to 3 times (15% damage reduction).");
		mInfo.mLinkedSpell = ClassAbility.COUNTER_STRIKE;
		mDisplayItem = new ItemStack(Material.CACTUS, 1);
		mReflect = isLevelOne() ? COUNTER_STRIKE_1_REFLECT : COUNTER_STRIKE_2_REFLECT;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.MELEE
			    && source != null
			    && !event.isBlocked()
			    && mPlayer != null
			    && mPlayer.getNoDamageTicks() <= mPlayer.getMaximumNoDamageTicks() / 2f) {

			Location loc = mPlayer.getLocation().add(0, 1, 0);
			new PartialParticle(Particle.SWEEP_ATTACK, loc, 6, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FIREWORKS_SPARK, loc, 8, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(mPlayer);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 0.7f);
			double eventDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, event.getOriginalDamage() * mReflect);
			List<LivingEntity> entityList = EntityUtils.getNearbyMobs(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, COUNTER_STRIKE_RADIUS));

			if (entityList.remove(source)) {
				DamageUtils.damage(mPlayer, source, DamageType.MELEE_SKILL, eventDamage, mInfo.mLinkedSpell, true, true);
			}

			for (LivingEntity mob : entityList) {
				DamageUtils.damage(mPlayer, mob, DamageType.WARRIOR_AOE, eventDamage, mInfo.mLinkedSpell, true, true);
			}

			if (isEnhanced()) {
				// Remove any old mobs from the list so they don't pile up. There should never be too many at once so this shouldn't be intensive
				mLastDamageTime.forEach(this::clearIfExpired);
				Integer stacks = mStacks.get(source);
				Integer lastDamageTime = mLastDamageTime.get(source);
				if (stacks != null) {
					if (lastDamageTime != null) {
						event.setDamage(event.getDamage() * (1 - stacks * (DAMAGE_REDUCTION_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_REDUCTION))));
						mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.5f + 0.1f * stacks, 2.5f - 0.5f * stacks);
						if (stacks < MAX_STACKS + CharmManager.getLevel(mPlayer, CHARM_STACKS)) {
							mStacks.put(source, stacks + 1);
						}
					}
				} else {
					mStacks.put(source, 1);
				}
				mLastDamageTime.put(source, mPlayer.getTicksLived());
			}
		}
	}

	private void clearIfExpired(LivingEntity mob, Integer time) {
		if (time < mPlayer.getTicksLived() - (REDUCTION_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION))) {
			mLastDamageTime.remove(mob);
			mStacks.remove(mob);
		}
	}
}
