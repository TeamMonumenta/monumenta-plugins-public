package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.DARK_GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Critical scythe attacks heal you.")
			.addLine()
			.addStat("Healing: %d HP")
				.statValues(stat(a -> a.mHeal, HEAL))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<SoulRend> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Soul Rend* marks the mob you hit for %t.").styles(UNDERLINED)
				.statValues(stat(a -> a.mMarkDuration, MARK_DURATION))
			.addLine()
			.addLine("Your next %d critical attacks against that")
				.statValues(stat(a -> a.mMarks, MARK_COUNT))
			.addLine("mob heal for a portion of the damage dealt.")
			.addLine("*(If that mob dies, heal %d HP for each*").styles(DARK_GREY)
				.statValues(stat(REMAINING_MARK_HEAL))
			.addLine("*remaining mark it had)*").styles(DARK_GREY)
			.addLine()
			.addLine("*Soul Rend* now heals all nearby players.").styles(UNDERLINED)
			.addLine()
			.addStat("Healing: %p of damage dealt (max %d HP)")
				.statValues(stat(a -> a.mHealPercent, MARK_HEAL_PERCENT), stat(a -> a.mHealCap, MARK_HEAL_CAP))
			.addStat("Heal Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addDashedLine();
	}

	private static Description<SoulRend> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Soul Rend* healing that exceeds your max HP").styles(UNDERLINED)
			.addLine("or is negated by *Dark Pact* is converted").styles(UNDERLINED)
			.addLine("into up to %d absorption that lasts for %t.")
				.statValues(stat(a -> a.mAbsorptionCap, ABSORPTION_CAP), stat(a -> a.mAbsorptionDuration, ABSORPTION_DURATION))
			.addDashedLine();
	}
}
