package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.DefensiveLineCS;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DefensiveLine extends Ability {
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "DefensiveLinePercentDamageReceivedEffect";
	private static final String NEGATE_DAMAGE_EFFECT_NAME = "DefensiveLineNegateDamageEffect";
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_1 = 0.20;
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_2 = 0.30;
	private static final int DURATION = 20 * 10;
	private static final int COOLDOWN = 20 * 30;
	private static final int RANGE = 8;
	private static final int KNOCK_AWAY_RADIUS = 3;
	private static final float KNOCK_AWAY_SPEED = 0.25f;

	public static final String CHARM_REDUCTION = "Defensive Line Resistance";
	public static final String CHARM_DURATION = "Defensive Line Duration";
	public static final String CHARM_COOLDOWN = "Defensive Line Cooldown";
	public static final String CHARM_RANGE = "Defensive Line Range";
	public static final String CHARM_RADIUS = "Defensive Line Radius";
	public static final String CHARM_KNOCKBACK = "Defensive Line Knockback";
	public static final String CHARM_NEGATIONS = "Defensive Line Damage Negation";

	public static final AbilityInfo<DefensiveLine> INFO =
		new AbilityInfo<>(DefensiveLine.class, "Defensive Line", DefensiveLine::new)
			.linkedSpell(ClassAbility.DEFENSIVE_LINE)
			.scoreboardId("DefensiveLine")
			.shorthandName("DL")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Increase resistance for you and surrounding players.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CHAIN);

	private final int mDuration;
	private final double mRange;
	private final double mPercentDamageReceived;
	private final double mKnockAwayRadius;

	private final DefensiveLineCS mCosmetic;

	public DefensiveLine(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mPercentDamageReceived = (isLevelOne() ? PERCENT_DAMAGE_RECEIVED_EFFECT_1 : PERCENT_DAMAGE_RECEIVED_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);
		mKnockAwayRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, KNOCK_AWAY_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DefensiveLineCS());
	}

	@Override
	public void blockWithShieldEvent() {
		if (isOnCooldown() || !mPlayer.isSneaking()) {
			return;
		}
		putOnCooldown();
		World world = mPlayer.getWorld();
		Location location = mPlayer.getLocation();

		List<Player> players = PlayerUtils.playersInRange(location, mRange, true);
		players.removeIf(player -> player.getScoreboardTags().contains("disable_class"));

		mCosmetic.onCast(mPlugin, mPlayer, world, location, players);

		for (Player player : players) {
			Location loc = player.getLocation();

			mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME,
				new PercentDamageReceived(mDuration, -mPercentDamageReceived).deleteOnAbilityUpdate(true));
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(player, NEGATE_DAMAGE_EFFECT_NAME,
					new NegateDamage(mDuration, (int) (1 + CharmManager.getLevel(mPlayer, CHARM_NEGATIONS)),
						EnumSet.of(DamageEvent.DamageType.MELEE),
						new PartialParticle(Particle.FIREWORKS_SPARK, loc).count(5).delta(0).extra(0.25))
						.deleteOnAbilityUpdate(true));
			}

			for (LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(player), mKnockAwayRadius).getHitMobs()) {
				MovementUtils.knockAway(player, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCK_AWAY_SPEED), true);
			}
		}
	}

	private static Description<DefensiveLine> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Block while sneaking to grant yourself and other players within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks ")
			.addPercent(a -> a.mPercentDamageReceived, PERCENT_DAMAGE_RECEIVED_EFFECT_1, false, Ability::isLevelOne)
			.add(" resistance for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. Additionally, mobs within ")
			.add(a -> a.mKnockAwayRadius, KNOCK_AWAY_RADIUS)
			.add(" blocks of an affected player are knocked back.")
			.addCooldown(COOLDOWN);
	}

	private static Description<DefensiveLine> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The effect is increased to ")
			.addPercent(a -> a.mPercentDamageReceived, PERCENT_DAMAGE_RECEIVED_EFFECT_2, false, Ability::isLevelTwo)
			.add(" resistance.");
	}

	private static Description<DefensiveLine> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Additionally, all affected players negate the next melee attack dealt to them within the duration.");
	}

}
