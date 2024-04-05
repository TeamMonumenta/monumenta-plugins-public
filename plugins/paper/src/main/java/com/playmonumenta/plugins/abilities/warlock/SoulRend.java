package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
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
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.NavigableSet;
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
			.descriptions(
				"Attacking an enemy with a critical scythe attack heals you for %s health. Cooldown: %ss."
					.formatted(HEAL, StringUtils.ticksToSeconds(COOLDOWN)),
				("The attacked enemy is marked for %s seconds, allowing your next %s critical scythe attacks against them to heal you for %s%% of the damage dealt, capped at %s health per hit. " +
					"Killing the enemy heals you for %s health for each remaining mark on the mob. " +
					"Healing from this ability now applies to all players within %s blocks of you.")
					.formatted(StringUtils.ticksToSeconds(MARK_DURATION), MARK_COUNT, StringUtils.multiplierToPercentage(MARK_HEAL_PERCENT), MARK_HEAL_CAP, REMAINING_MARK_HEAL, RADIUS),
				"Healing above max health, as well as any healing from this skill that remains negated by Dark Pact, is converted into Absorption, up to %s absorption health, for %ss."
					.formatted(ABSORPTION_CAP, StringUtils.ticksToSeconds(ABSORPTION_DURATION)))
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

	private final double mDarkPactHeal;
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
		mAbsorptionCap = ABSORPTION_CAP;
		mAbsorptionDuration = ABSORPTION_DURATION;

		mDarkPactHeal = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, isLevelOne() ? DARK_PACT_HEAL_1 : DARK_PACT_HEAL_2);
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

			NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
			if (darkPactEffects != null) {
				if (mDarkPact != null && mDarkPact.isLevelTwo()) {
					int currPactDuration = darkPactEffects.last().getDuration();
					mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
					mCosmetic.rendHealEffect(mPlayer, mPlayer, enemy);
					double healed = PlayerUtils.healPlayer(mPlugin, mPlayer, mDarkPactHeal);
					mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(currPactDuration, -1));

					if (isEnhanced()) {
						// All healing, minus the 2/4 healed through dark pact, converted to absorption
						double absorption = mDarkPactHeal - healed;
						absorptionPlayer(mPlayer, absorption, enemy);
					}
				} else if (isEnhanced()) {
					// All healing converted to absorption
					absorptionPlayer(mPlayer, mDarkPactHeal, enemy);
				}
			} else {
				healPlayer(mPlayer, mHeal, enemy);

				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(enemy, "SoulRendLifeSteal",
						new SoulRendLifeSteal(mPlugin, mPlayer, mMarkDuration, mMarks, mHealPercent, mHealCap, mRemainingHeal, mRadius, isEnhanced(), mAbsorptionCap, mAbsorptionDuration, mCosmetic));

					double allyHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, mHeal);
					for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
						healPlayer(p, allyHeal, enemy);
					}
				}
			}

			putOnCooldown();
		}
		return false;
	}

	private void healPlayer(Player player, double heal, LivingEntity enemy) {
		mCosmetic.rendHealEffect(mPlayer, player, enemy);
		double healed = PlayerUtils.healPlayer(mPlugin, player, heal, mPlayer);
		if (isEnhanced()) {
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

}
