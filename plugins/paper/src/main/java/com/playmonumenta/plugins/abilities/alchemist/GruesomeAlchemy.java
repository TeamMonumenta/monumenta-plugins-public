package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class GruesomeAlchemy extends Ability implements PotionAbility {
	public static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	public static final double GRUESOME_ALCHEMY_0_SLOWNESS_AMPLIFIER = 0.05;
	public static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.15;
	public static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.25;
	public static final double GRUESOME_ALCHEMY_3_SLOWNESS_AMPLIFIER = 0.35;
	public static final double GRUESOME_ALCHEMY_0_VULNERABILITY_AMPLIFIER = 0.05;
	public static final double GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 0.15;
	public static final double GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 0.25;
	public static final double GRUESOME_ALCHEMY_3_VULNERABILITY_AMPLIFIER = 0.35;
	public static final double GRUESOME_ALCHEMY_0_WEAKEN_AMPLIFIER = 0.05;
	public static final double GRUESOME_ALCHEMY_1_WEAKEN_AMPLIFIER = 0.1;
	public static final double GRUESOME_ALCHEMY_3_WEAKEN_AMPLIFIER = 0.2;
	public static final double GRUESOME_POTION_DAMAGE_MULTIPLIER = 0.8;
	public static final int GRUESOME_ALCHEMY_ENHANCEMENT_STUN_DURATION = 10;

	public static final String CHARM_DAMAGE_MULTIPLIER = "Gruesome Alchemy Damage Multiplier";
	public static final String CHARM_SLOWNESS = "Gruesome Alchemy Slowness Amplifier";
	public static final String CHARM_VULNERABILITY = "Gruesome Alchemy Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Gruesome Alchemy Weakness Amplifier";
	public static final String CHARM_DURATION = "Gruesome Alchemy Duration";
	public static final String CHARM_STUN_DURATION = "Gruesome Alchemy Stun Duration";

	public static final AbilityInfo<GruesomeAlchemy> INFO =
		new AbilityInfo<>(GruesomeAlchemy.class, "Gruesome Alchemy", GruesomeAlchemy::new)
			.linkedSpell(ClassAbility.GRUESOME_ALCHEMY)
			.scoreboardId("GruesomeAlchemy")
			.shorthandName("GA")
			.canUse(player -> AbilityUtils.getClassNum(player) == Alchemist.CLASS_ID)
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw potions that deal less damage, but apply slowness, weakness, and vulnerability to enemies.")
			.displayItem(Material.SKELETON_SKULL);

	private final int mDuration;
	private final int mStunDuration;
	private final ConcurrentHashMap<UUID, Integer> mAfflictedMobs = new ConcurrentHashMap<>();

	private @Nullable AlchemistPotions mAlchemistPotions;

	public GruesomeAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, GRUESOME_ALCHEMY_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, GRUESOME_ALCHEMY_ENHANCEMENT_STUN_DURATION);

		Bukkit.getScheduler().runTask(
			plugin,
			() -> mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class)
		);
	}

	public int getLevel() {
		if (isLevelOne()) {
			return 1;
		}
		if (isLevelTwo()) {
			return 2;
		}
		return 0;
	}

	public double getWeaknessAmplifier(int level) {
		double base = 0;
		switch (level) {
			case 0 -> base = GRUESOME_ALCHEMY_0_WEAKEN_AMPLIFIER;
			case 1, 2 -> base = GRUESOME_ALCHEMY_1_WEAKEN_AMPLIFIER;
			case 3 -> base = GRUESOME_ALCHEMY_3_WEAKEN_AMPLIFIER;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
	}

	public double getSlownessAmplifier(int level) {
		double base = 0;
		switch (level) {
			case 0 -> base = GRUESOME_ALCHEMY_0_SLOWNESS_AMPLIFIER;
			case 1 -> base = GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER;
			case 2 -> base = GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER;
			case 3 -> base = GRUESOME_ALCHEMY_3_SLOWNESS_AMPLIFIER;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
	}

	public double getVulnerabilityAmplifier(int level) {
		double base = 0;
		switch (level) {
			case 0 -> base = GRUESOME_ALCHEMY_0_VULNERABILITY_AMPLIFIER;
			case 1 -> base = GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER;
			case 2 -> base = GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER;
			case 3 -> base = GRUESOME_ALCHEMY_3_VULNERABILITY_AMPLIFIER;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY);
	}

	private void applyEffects(LivingEntity mob, int level) {
		if (mAlchemistPotions == null) {
			return;
		}
		EntityUtils.applySlow(mPlugin, mDuration, getSlownessAmplifier(level), mob);
		EntityUtils.applyVulnerability(mPlugin, mDuration, getVulnerabilityAmplifier(level), mob);
		EntityUtils.applyWeaken(mPlugin, mDuration, getWeaknessAmplifier(level), mob);
		mAfflictedMobs.put(mob.getUniqueId(), Bukkit.getCurrentTick());
		cleanAfflictedMap();
	}

	private void cleanAfflictedMap() {
		int currentTick = Bukkit.getCurrentTick();
		mAfflictedMobs.values().removeIf(applicationTicks -> currentTick - applicationTicks > mDuration);
	}

	public boolean isAfflicted(LivingEntity mob) {
		int currentTick = Bukkit.getCurrentTick();
		return mAfflictedMobs.containsKey(mob.getUniqueId()) &&
			currentTick - mAfflictedMobs.get(mob.getUniqueId()) <= mDuration;
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level, boolean refreshBrutalDot) {
		if (isGruesome) {
			applyEffects(mob, level);
		}
	}

	public static void tryDoEnhancementEffect(@Nullable GruesomeAlchemy gruesomeAlchemy, LivingEntity mob) {
		if (gruesomeAlchemy == null) {
			return;
		}

		gruesomeAlchemy.internalTryDoEnhancementEffect(mob);
	}

	private void internalTryDoEnhancementEffect(LivingEntity mob) {
		if (!isEnhanced()) {
			return;
		}

		if (!mAfflictedMobs.containsKey(mob.getUniqueId())) {
			return;
		}

		if (EntityUtils.isElite(mob)) {
			EntityUtils.applyStagger(mPlugin, mStunDuration, mob);
			return;
		}
		EntityUtils.applyStun(mPlugin, mStunDuration, mob);
		if (mAlchemistPotions != null) {
			mAlchemistPotions.mCosmetic.stunEffects(mob);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mAlchemistPotions == null) {
			return;
		}

		cleanAfflictedMap();
		if (!isEnhanced()) {
			return;
		}

		mAfflictedMobs.keySet().forEach(mobId -> {
			Entity e = Bukkit.getEntity(mobId);
			if (e == null) {
				return;
			}

			if (e instanceof LivingEntity mob && mAlchemistPotions != null) {
				mAlchemistPotions.mCosmetic.dizzyEffects(mPlayer, mob);
			}
		});
	}

	public void applyHigherLevel(LivingEntity mob, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions != null) {
			mAlchemistPotions.applyEffects(mob, true, playerItemStats, getLevel() + 1);
		}
	}

	private static Description<GruesomeAlchemy> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Increase the slowness, vulnerability, and").styles(UNDERLINED)
			.addLine("weakness applied by *Gruesome* potions.").styles(Alchemist.GRUESOME_COLOR)
			.addLine()
			.addStatComparison("Effect: %p0 -> %p1 Slowness")
				.statValues(stat(GRUESOME_ALCHEMY_0_SLOWNESS_AMPLIFIER), stat(a -> a.getSlownessAmplifier(1), GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER))
			.addStatComparison("Effect: %p0 -> %p1 Vulnerability")
				.statValues(stat(GRUESOME_ALCHEMY_0_VULNERABILITY_AMPLIFIER), stat(a -> a.getVulnerabilityAmplifier(1), GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER))
			.addStatComparison("Effect: %p0 -> %p1 Weakness")
				.statValues(stat(GRUESOME_ALCHEMY_0_WEAKEN_AMPLIFIER), stat(a -> a.getWeaknessAmplifier(1), GRUESOME_ALCHEMY_1_WEAKEN_AMPLIFIER))
			.addDashedLine();
	}

	private static Description<GruesomeAlchemy> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase the slowness and vulnerability").styles(UNDERLINED)
			.addLine("applied by *Gruesome* potions even further.").styles(Alchemist.GRUESOME_COLOR)
			.addLine()
			.addStatComparison("Effect: %p1 -> %p2 Slowness")
				.statValues(stat(GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER), stat(a -> a.getSlownessAmplifier(2), GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER))
			.addStatComparison("Effect: %p1 -> %p2 Vulnerability")
				.statValues(stat(GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER), stat(a -> a.getVulnerabilityAmplifier(2), GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER))
			.addLine()
			.addLine("When boosted by *Volatile Reaction* to Level 3:").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Effect: %p2 -> %p3 Slowness")
				.statValues(stat(GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER), stat(a -> a.getSlownessAmplifier(3), GRUESOME_ALCHEMY_3_SLOWNESS_AMPLIFIER))
			.addStatComparison("Effect: %p2 -> %p3 Vulnerability")
				.statValues(stat(GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER), stat(a -> a.getVulnerabilityAmplifier(3), GRUESOME_ALCHEMY_3_VULNERABILITY_AMPLIFIER))
			.addStatComparison("Effect: %p2 -> %p3 Weakness")
				.statValues(stat(GRUESOME_ALCHEMY_1_WEAKEN_AMPLIFIER), stat(a -> a.getWeaknessAmplifier(3), GRUESOME_ALCHEMY_3_WEAKEN_AMPLIFIER))
			.addDashedLine();
	}

	private static Description<GruesomeAlchemy> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Hitting *Gruesome*-afflicted mobs with").styles(Alchemist.GRUESOME_COLOR)
			.addLine("powerful abilities stuns them.")
			.addLine("Elites are staggered instead.")
			.addLine()
			.addStat("Powerful Abilities:")
				.addListItem("Alchemical Artillery")
				.addListItem("Unstable Amalgam")
				.addListItem("Volatile Reaction")
				.addListItem("Esoteric Enhancements")
				.addListItem("Panacea")
				.addListItem("Transmutation Ring")
			.addLine()
			.addStat("Effect: Stun/Stagger for %t")
			.statValues(stat(a -> a.mStunDuration, GRUESOME_ALCHEMY_ENHANCEMENT_STUN_DURATION))
			.addDashedLine();
	}
}
