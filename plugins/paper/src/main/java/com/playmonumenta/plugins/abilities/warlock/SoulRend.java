package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SoulRendCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.SoulRendLifeSteal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SoulRend extends Ability {

	public static final int HEAL = 5;
	private static final int MARK_DURATION = 8 * 20;
	private static final int MARK_COUNT = 2;
	private static final double MARK_HEAL_PERCENT = 0.1;
	private static final double MARK_HEAL_CAP = 2.5;
	private static final double REMAINING_MARK_HEAL = 1.5;
	private static final int RADIUS = 7;
	private static final int COOLDOWN = 20 * 8;
	private static final int ABSORPTION_CAP = 4;
	private static final int ABSORPTION_DURATION = 50;
	public static final int DARK_PACT_HEAL_1 = 2;
	public static final int DARK_PACT_HEAL_2 = 4;

	public static final String CHARM_COOLDOWN = "Soul Rend Cooldown";
	public static final String CHARM_HEAL = "Soul Rend Healing";
	public static final String CHARM_PACT_HEAL = "Soul Rend Pact Healing";
	public static final String CHARM_MARK_DURATION = "Soul Rend Mark Duration";
	public static final String CHARM_MARK_COUNT = "Soul Rend Mark Count";
	public static final String CHARM_ALLY = "Soul Rend Ally Heal";
	public static final String CHARM_RADIUS = "Soul Rend Radius";
	public static final String CHARM_ABSORPTION_CAP = "Soul Rend Absorption Cap";
	public static final String CHARM_ABSORPTION_DURATION = "Soul Rend Absorption Duration";

	public static final AbilityInfo<SoulRend> INFO =
		new AbilityInfo<>(SoulRend.class, "Soul Rend", SoulRend::new)
			.linkedSpell(ClassAbility.SOUL_REND)
			.scoreboardId("SoulRend")
			.shorthandName("SR")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Critical strikes heal you.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.POTION);

	private final double mHeal;
	private final int mMarkDuration;
	private final int mMarks;
	private final double mHealPercent;
	private final double mHealCap;
	private final double mRemainingHeal;
	private final double mRadius;
	private final double mAbsorptionCap;
	private final int mAbsorptionDuration;
	private final double mAllyHealMultiplier;

	public final double mDarkPactHeal;
	private @Nullable DarkPact mDarkPact;

	private final SoulRendCS mCosmetic;

	public SoulRend(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHeal = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, HEAL);
		mMarkDuration = CharmManager.getDuration(player, CHARM_MARK_DURATION, MARK_DURATION);
		mMarks = MARK_COUNT + (int) CharmManager.getLevel(player, CHARM_MARK_COUNT);
		mHealPercent = MARK_HEAL_PERCENT;
		mHealCap = MARK_HEAL_CAP;
		mRemainingHeal = REMAINING_MARK_HEAL;
		mRadius = CharmManager.getRadius(player, CHARM_RADIUS, RADIUS);
		mAbsorptionCap = CharmManager.calculateFlatAndPercentValue(player, CHARM_ABSORPTION_CAP, ABSORPTION_CAP);
		mAbsorptionDuration = CharmManager.getDuration(player, CHARM_ABSORPTION_DURATION, ABSORPTION_DURATION);
		mAllyHealMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, 1);

		mDarkPactHeal = CharmManager.calculateFlatAndPercentValue(player, CHARM_PACT_HEAL, isLevelOne() ? DARK_PACT_HEAL_1 : DARK_PACT_HEAL_2);
		Bukkit.getScheduler().runTask(plugin, () -> mDarkPact = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, DarkPact.class));

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SoulRendCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!isOnCooldown()
			&& event.getType() == DamageType.MELEE
			&& PlayerUtils.isFallingAttack(mPlayer)
			&& ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {

			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			mCosmetic.rendHitSound(world, loc);
			mCosmetic.rendHitParticle(mPlayer, loc);

			@Nullable
			NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
			if (darkPactEffects != null) {
				if (mDarkPact != null && mDarkPact.isLevelTwo()) {
					NavigableSet<Effect> previousDarkPactEffects = new ConcurrentSkipListSet<>(darkPactEffects);

					mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
					healPlayer(mPlayer, mHeal, enemy, mDarkPactHeal);

					// give back the dark pact effects
					previousDarkPactEffects.forEach(effect ->
						mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(effect.getDuration(), -effect.getMagnitude()).deleteOnAbilityUpdate(true)));

				} else if (isEnhanced()) {
					// All healing converted to absorption
					absorptionPlayer(mPlayer, mHeal, enemy);
				}

			} else {
				healPlayer(mPlayer, mHeal, enemy);
			}

			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(enemy, "SoulRendLifeSteal." + mPlayer.getUniqueId(),
					new SoulRendLifeSteal(mPlayer, mMarkDuration, mMarks, mHealPercent, mHealCap, mRemainingHeal, this, mCosmetic)
						.deleteOnAbilityUpdate(true));

				healOthers(mHeal, enemy);
			}

			putOnCooldown();
		}
		return false;
	}

	public void markHeal(double heal, LivingEntity enemy) {
		healPlayer(mPlayer, heal, enemy, heal, false);
		healOthers(heal, enemy, false);
	}

	private void healOthers(double heal, LivingEntity enemy) {
		healOthers(heal, enemy, isEnhanced());
	}

	private void healOthers(double heal, LivingEntity enemy, boolean grantAbsorption) {
		for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
			healPlayer(p, heal * mAllyHealMultiplier, enemy, heal * mAllyHealMultiplier, grantAbsorption);
		}
	}

	private void healPlayer(Player player, double heal, LivingEntity enemy) {
		healPlayer(player, heal, enemy, heal, isEnhanced());
	}

	private void healPlayer(Player player, double heal, LivingEntity enemy, double healCap) {
		healPlayer(player, heal, enemy, healCap, isEnhanced());
	}

	private void healPlayer(Player player, double heal, LivingEntity enemy, double healCap, boolean grantAbsorption) {
		mCosmetic.rendHealEffect(mPlayer, player, enemy);
		double healed = PlayerUtils.healPlayer(mPlugin, player, healCap, mPlayer);
		if (grantAbsorption) {
			absorptionPlayer(player, heal - healed, enemy);
		}
	}

	//Handles capping the absorption
	private void absorptionPlayer(Player player, double absorption, LivingEntity enemy) {
		if (absorption <= 0) {
			return;
		}
		AbsorptionUtils.addAbsorption(player, absorption, mAbsorptionCap, mAbsorptionDuration);
		mCosmetic.rendAbsorptionEffect(mPlayer, player, enemy);
	}

	private static Description<SoulRend> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When you attack a mob with a critical scythe attack, heal ")
			.add(a -> a.mHeal, HEAL)
			.add(" health.")
			.addCooldown(COOLDOWN);
	}

	private static Description<SoulRend> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The attacked mob is marked for ")
			.addDuration(a -> a.mMarkDuration, MARK_DURATION)
			.add(" seconds, causing your next ")
			.add(a -> a.mMarks, MARK_COUNT)
			.add(" critical scythe attacks against them to heal you for ")
			.addPercent(a -> a.mHealPercent, MARK_HEAL_PERCENT)
			.add(" of the damage dealt, capped at ")
			.add(a -> a.mHealCap, MARK_HEAL_CAP)
			.add(" health per hit. Killing the mob heals you for ")
			.add(a -> a.mRemainingHeal, REMAINING_MARK_HEAL)
			.add(" health for each remaining mark on the mob. Healing from this ability now applies to all players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of you.");
	}

	private static Description<SoulRend> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Healing from the initial attack that is above max health or negated by Dark Pact is converted into up to ")
			.add(a -> a.mAbsorptionCap, ABSORPTION_CAP)
			.add(" absorption health, which lasts ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_DURATION)
			.add(" seconds.");
	}
}
