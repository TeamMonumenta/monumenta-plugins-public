package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ViciousCombosCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ViciousCombos extends Ability {

	private static final int VICIOUS_COMBOS_RANGE = 5;
	private static final int VICIOUS_COMBOS_COOL_1 = 20;
	private static final int VICIOUS_COMBOS_COOL_2 = 2 * 20;
	private static final int VICIOUS_COMBOS_CRIPPLE_DURATION = 5 * 20;
	private static final double VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL = 0.15;
	private static final double VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL = 0.15;
	private static final int ENHANCEMENT_COOLDOWN_REDUCTION = 20;
	private static final int ENHANCEMENT_CHARGE_LIFETIME = 3 * 20;
	private static final double ENHANCEMENT_DAMAGE_INCREASE = 0.2;

	public static final String CHARM_CDR = "Vicious Combos Cooldown Reduction";
	public static final String CHARM_RADIUS = "Vicious Combos Radius";
	public static final String CHARM_VULN = "Vicious Combos Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Vicious Combos Weakness Amplifier";
	public static final String CHARM_DURATION = "Vicious Combos Duration";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Vicious Combos Enhancement Damage Amplifier";

	public static final AbilityInfo<ViciousCombos> INFO =
		new AbilityInfo<>(ViciousCombos.class, "Vicious Combos", ViciousCombos::new)
			.linkedSpell(ClassAbility.VICIOUS_COMBOS)
			.scoreboardId("ViciousCombos")
			.shorthandName("VC")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Killing mobs reduces cooldowns, and killing Elite mobs completely refreshes them.")
			.quest216Message("-------n-------u-------")
			.displayItem(Material.ZOMBIE_HEAD);

	private final int mCDR;
	private final double mRadius;
	private final int mDuration;
	private final double mVuln;
	private final double mWeaken;
	private final double mEnhancementDamage;

	private @Nullable ClassAbility mLastAbility = null;
	private int mAbilityCastTime = 0;

	private final ViciousCombosCS mCosmetic;

	public ViciousCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCDR = CharmManager.getDuration(mPlayer, CHARM_CDR, isLevelOne() ? VICIOUS_COMBOS_COOL_1 : VICIOUS_COMBOS_COOL_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, VICIOUS_COMBOS_RANGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, VICIOUS_COMBOS_CRIPPLE_DURATION);
		mVuln = VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mWeaken = VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mEnhancementDamage = ENHANCEMENT_DAMAGE_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ViciousCombosCS());
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Location loc = killedEntity.getLocation();
			loc = loc.add(0, 0.5, 0);
			World world = mPlayer.getWorld();

			if (EntityUtils.isElite(killedEntity) || EntityUtils.isBoss(killedEntity)) {
				mPlugin.mTimers.removeAllCooldowns(mPlayer);
				MessagingUtils.sendActionBarMessage(mPlayer, "All your cooldowns have been reset");

				if (isLevelTwo()) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius, mPlayer)) {
						new PartialParticle(Particle.SPELL_MOB, mob.getLocation().clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
						EntityUtils.applyVulnerability(mPlugin, mDuration, mVuln, mob);
						EntityUtils.applyWeaken(mPlugin, mDuration, mWeaken, mob);
					}
				}
				mCosmetic.comboOnElite(world, loc, mPlayer, mRadius, killedEntity);

			} else if (EntityUtils.isHostileMob(killedEntity)) {
				mPlugin.mTimers.updateCooldowns(mPlayer, mCDR);
				mCosmetic.comboOnKill(world, loc, mPlayer, mRadius, killedEntity);
			}
		}, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// If:
		// Is Enhanced,
		// Is a Melee Attack,
		// LastAbility does exist
		// The LastAbility cast time is within charge's lifetime.
		if (isEnhanced()
			&& event.getType() == DamageEvent.DamageType.MELEE
			&& mLastAbility != null
			&& Bukkit.getServer().getCurrentTick() < mAbilityCastTime + ENHANCEMENT_CHARGE_LIFETIME) {
			event.updateDamageWithMultiplier(1 + mEnhancementDamage);
			mPlugin.mTimers.updateCooldown(mPlayer, mLastAbility, ENHANCEMENT_COOLDOWN_REDUCTION);
			mCosmetic.enhancedCombo(enemy.getWorld(), mPlayer, enemy);

			clearState();
		}
		return false;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (isEnhanced()) {
			// Run this 1 tick late to prevent ByMyBlade triggering it immediately.
			new BukkitRunnable() {
				@Override
				public void run() {
					// Get the index of the ability in mAbilities, add to the order.
					mLastAbility = event.getSpell();
					mAbilityCastTime = Bukkit.getServer().getCurrentTick();
				}
			}.runTaskLater(mPlugin, 1);
		}

		return true;
	}

	public void clearState() {
		mLastAbility = null;
		mAbilityCastTime = 0;
	}

	private static Description<ViciousCombos> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Passively, killing a mob refreshes the cooldown of your abilities by ")
			.addDuration(a -> a.mCDR, VICIOUS_COMBOS_COOL_1, false, Ability::isLevelOne)
			.add(" second. Killing an Elite or Boss mob instead resets the cooldown of your abilities.");
	}

	private static Description<ViciousCombos> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Killing a mob now refreshes your ability cooldowns by ")
			.addDuration(a -> a.mCDR, VICIOUS_COMBOS_COOL_2, false, Ability::isLevelTwo)
			.add(" seconds. Killing an Elite or Boss mob inflicts nearby mobs within ")
			.add(a -> a.mRadius, VICIOUS_COMBOS_RANGE)
			.add(" blocks with ")
			.addPercent(a -> a.mWeaken, VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL)
			.add(" weaken and ")
			.addPercent(a -> a.mVuln, VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDuration, VICIOUS_COMBOS_CRIPPLE_DURATION)
			.add(" seconds.");
	}

	private static Description<ViciousCombos> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When an ability goes on cooldown, your next melee attack within ")
			.addDuration(ENHANCEMENT_CHARGE_LIFETIME)
			.add(" seconds deals ")
			.addPercent(a -> a.mEnhancementDamage, ENHANCEMENT_DAMAGE_INCREASE)
			.add(" more melee damage and that ability's cooldown is refreshed by ")
			.addDuration(ENHANCEMENT_COOLDOWN_REDUCTION)
			.add(" second, prioritizing the last ability.");
	}
}
