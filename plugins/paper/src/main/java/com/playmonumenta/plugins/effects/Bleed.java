package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ImmortalMountBoss;
import com.playmonumenta.plugins.bosses.bosses.ImmortalPassengerBoss;
import com.playmonumenta.plugins.bosses.bosses.WormSegmentBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Bleed extends Effect {
	public static final String effectID = "BleedingStacks";
	public static final String BLEED_EFFECT_NAME = "BleedEffect";
	private static final double STACK_WEAKEN = 0.03;
	private static final double BOSS_STACK_WEAKEN = 0.01;
	public static final int HEMORRHAGE_STACKS = 8;
	private static final int BOSS_HEMORRHAGE_STACKS = 20;
	private static final float HEMORRHAGE_DAMAGE_PERCENT = 0.25f;
	private static final float HEMORRHAGE_DAMAGE_CAP_PER_REGION = 35f;
	private static final float HEMORRHAGE_DAMAGE_BOSS_PER_REGION = 100f;
	private static final double HEMORRHAGE_WEAKEN = 0.3;
	// bleed duration is the duration of the debuff as a whole that enables stack gain/loss
	private static final int BLEED_DURATION = 50 * Constants.TICKS_PER_SECOND;
	// stack duration is the duration of the weaken as well as the interval stacks decrement at
	private static final int STACK_DURATION = 5 * Constants.TICKS_PER_SECOND;
	private static final int HEMORRHAGE_DURATION = 5 * Constants.TICKS_PER_SECOND;

	private static final Set<String> NEVER_HEMORRHAGE_BOSSTAGS = Set.of(
		ImmortalPassengerBoss.identityTag,
		ImmortalMountBoss.identityTag,
		WormSegmentBoss.identityTag
		// This is FAILSAFE CODE. Damage transferring immortal mobs / worms should never even receive the bleeding (unless being forced to Quiet Hemorrhage).
		// Non-damage transferring immortal mobs shouldn't take damage in the first place.
	);

	private static final ItemStack REDSTONE_ITEM_STACK = new ItemStack(Material.REDSTONE_BLOCK);
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	private int mStacks;
	private boolean mHasHemorrhaged;
	private int mStacksGainedThisTick;
	private int mTicksToStackDecay = STACK_DURATION;


	public Bleed() {
		super(BLEED_DURATION, effectID);
		mHasHemorrhaged = false;
		mStacksGainedThisTick = 0;
	}
	// Don't call this constructor from anywhere except the EntityUtils helper methods, those set the stacks correctly and the constructor itself doesn't

	public void incrementStacks(Player player, LivingEntity mob, int stacks) {
		if (mHasHemorrhaged) {
			return;
		}
		double hemorrhageStacks = EntityUtils.isBoss(mob) ? BOSS_HEMORRHAGE_STACKS : HEMORRHAGE_STACKS;
		mTicksToStackDecay = STACK_DURATION;
		setDuration(BLEED_DURATION);
		// Apply the highest number of stacks used at once
		mStacks += Math.max(stacks - mStacksGainedThisTick, 0);
		if (mStacksGainedThisTick == 0) {
			// Run this before setting mStacksGainedThisTick, so that only one runnable is created
			new BukkitRunnable() {
				@Override
				public void run() {
					mStacksGainedThisTick = 0;
				}
			}.runTaskLater(Plugin.getInstance(), 0);
			// Runs at the start of the next tick
		}
		mStacksGainedThisTick = Math.max(mStacksGainedThisTick, stacks);

		double weakness = Math.min(hemorrhageStacks, mStacks)
			* (EntityUtils.isBoss(mob) ? BOSS_STACK_WEAKEN : STACK_WEAKEN);
		EntityUtils.applyWeaken(Plugin.getInstance(), STACK_DURATION, weakness, mob);

		if (mStacks >= hemorrhageStacks) {
			PlayerUtils.callHemorrhageEvent(player, mob);
			mHasHemorrhaged = true;
			Set<String> bosstags = mob.getScoreboardTags();
			if (NEVER_HEMORRHAGE_BOSSTAGS.stream().noneMatch(bosstags::contains)) {
				double damage = EntityUtils.isBoss(mob)
					? (Region.getRegionNumber(ServerProperties.getRegion(player)) * HEMORRHAGE_DAMAGE_BOSS_PER_REGION)
					: Math.min(EntityUtils.getMaxHealth(mob) * HEMORRHAGE_DAMAGE_PERCENT,
					Region.getRegionNumber(ServerProperties.getRegion(player)) * HEMORRHAGE_DAMAGE_CAP_PER_REGION);
				DamageUtils.damage(player, mob, DamageEvent.DamageType.TRUE, damage, ClassAbility.BLEEDING, true, false);
			}
			EntityUtils.applyWeaken(Plugin.getInstance(), HEMORRHAGE_DURATION, HEMORRHAGE_WEAKEN, mob);
			setDuration(HEMORRHAGE_DURATION);


			new PartialParticle(Particle.ITEM_CRACK, LocationUtils.getHalfHeightLocation(mob), 75, 0.25, 0.25, 0.25, 1, REDSTONE_ITEM_STACK).spawnAsEnemyBuff();
			mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.75F, 0.5F);
			mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_SNIFFER_EGG_CRACK, 1.25F, 0.7F);
			mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 1.0F, 0.5F);
			mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SPIDER_STEP, SoundCategory.PLAYERS, 0.85F, 0.5F);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz && entity instanceof LivingEntity mob && !mHasHemorrhaged) {
			if (mStacks > 0) {
				mTicksToStackDecay -= Constants.QUARTER_TICKS_PER_SECOND;

				if (mTicksToStackDecay <= 0) {
					mTicksToStackDecay = STACK_DURATION;
					mStacks--;
					double weakness = Math.min(EntityUtils.isBoss(mob) ? BOSS_HEMORRHAGE_STACKS - 1 : HEMORRHAGE_STACKS - 1, mStacks)
						* (EntityUtils.isBoss(mob) ? BOSS_STACK_WEAKEN : STACK_WEAKEN);
					EntityUtils.applyWeaken(Plugin.getInstance(), STACK_DURATION, weakness, mob);

					if (mStacks <= 0) {
						setDuration(0);
					}
				}

				if (!mHasHemorrhaged && twoHertz) {
					new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mob), 5 * mStacks, 0.4, 0.4, 0.4, 0.05).data(COLOR).spawnAsEnemyBuff();
				}
			}
		}
	}

	/**
	 * Apply the Hemorrhage effect to clear all bleed stacks and block further application of them, but do not deal damage or call a HemorrhageEvent.
	 * Useful for Worms.
	 *
	 * @param mob         Mob to force the quiet hemorrhage on
	 * @param applyWeaken Whether to also apply the 30% Weaken
	 */
	public void actuallyApplyHemorrhageCooldown(LivingEntity mob, boolean applyWeaken) {
		mHasHemorrhaged = true;
		mStacks = 1;
		if (applyWeaken) {
			EntityUtils.applyWeaken(Plugin.getInstance(), HEMORRHAGE_DURATION, HEMORRHAGE_WEAKEN, mob);
		}
		setDuration(HEMORRHAGE_DURATION);
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s stacks:%s",
			this.getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}

	@Override
	public double getMagnitude() {
		return mHasHemorrhaged ? 1000 : mStacks;
		// A Bleed effect that has already hemorrhaged should have the highest priority and obfuscate all lower levels of Bleed effect
	}

	public boolean hasHemorrhaged() {
		return mHasHemorrhaged;
	}
}
