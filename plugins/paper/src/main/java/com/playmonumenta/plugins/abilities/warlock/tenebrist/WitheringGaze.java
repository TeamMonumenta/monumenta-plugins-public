package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.WitheringGazeCS;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WitheringGaze extends Ability {

	private static final int WITHERING_GAZE_STUN_DURATION = 3 * 20;
	private static final int WITHERING_GAZE_DOT_DURATION_1 = 6 * 20;
	private static final int WITHERING_GAZE_DOT_DURATION_2 = 8 * 20;
	private static final int WITHERING_GAZE_DOT_PERIOD = 10;
	private static final int WITHERING_GAZE_DOT_DAMAGE = 1;
	private static final int WITHERING_GAZE_1_COOLDOWN = 20 * 30;
	private static final int WITHERING_GAZE_2_COOLDOWN = 20 * 20;
	private static final int WITHERING_GAZE_RANGE = 9;
	private static final double ANGLE = 65;
	private static final String DOT_EFFECT_NAME = "WitheringGazeDamageOverTimeEffect";

	public static final String CHARM_STUN = "Withering Gaze Stun Duration";
	public static final String CHARM_COOLDOWN = "Withering Gaze Cooldown";
	public static final String CHARM_RANGE = "Withering Gaze Range";
	public static final String CHARM_CONE = "Withering Gaze Cone";
	public static final String CHARM_DOT = "Withering Gaze Damage Over Time Duration";
	public static final String CHARM_DAMAGE = "Withering Gaze Damage";

	public static final AbilityInfo<WitheringGaze> INFO =
		new AbilityInfo<>(WitheringGaze.class, "Withering Gaze", WitheringGaze::new)
			.linkedSpell(ClassAbility.WITHERING_GAZE)
			.scoreboardId("WitheringGaze")
			.shorthandName("WG")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Stun and deal damage over time to all mobs in front of you.")
			.cooldown(WITHERING_GAZE_1_COOLDOWN, WITHERING_GAZE_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WitheringGaze::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.WITHER_ROSE);

	private final double mAngle;
	private final double mRange;
	private final int mStunDuration;
	private final int mDOTDuration;
	private final double mDOTDamage;
	private final WitheringGazeCS mCosmetic;

	public WitheringGaze(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAngle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, WITHERING_GAZE_RANGE);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN, WITHERING_GAZE_STUN_DURATION);
		mDOTDuration = CharmManager.getDuration(player, CHARM_DOT, (isLevelOne() ? WITHERING_GAZE_DOT_DURATION_1 : WITHERING_GAZE_DOT_DURATION_2));
		mDOTDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, WITHERING_GAZE_DOT_DAMAGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WitheringGazeCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		mCosmetic.onCast(mPlayer, mRange, mAngle);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		new BukkitRunnable() {
			double mCurrentRadius = 1.15;

			@Override
			public void run() {
				Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mCurrentRadius, 0), 2 * mCurrentRadius, mCurrentRadius, Math.toRadians(ANGLE));
				for (LivingEntity e : hitbox.getHitMobs()) {
					if (mPlayer.hasLineOfSight(e)) {
						if (EntityUtils.isElite(e) || EntityUtils.isBoss(e)) {
							EntityUtils.applySlow(mPlugin, mStunDuration, 1.0, e);
						} else {
							EntityUtils.applyStun(mPlugin, mStunDuration, e);
						}
						mPlugin.mEffectManager.addEffect(e, DOT_EFFECT_NAME, new CustomDamageOverTime(mDOTDuration, mDOTDamage,
							WITHERING_GAZE_DOT_PERIOD, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC));
					}
				}

				if (mCurrentRadius > mRange) {
					this.cancel();
				}

				mCurrentRadius += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private static Description<WitheringGaze> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to unleash a ")
			.add(a -> a.mRange, WITHERING_GAZE_RANGE)
			.add(" block long cone in the direction you are facing. Mobs in the cone are stunned for ")
			.addDuration(a -> a.mStunDuration, WITHERING_GAZE_STUN_DURATION)
			.add(" seconds (elites and bosses are rooted instead) and dealt ")
			.add(a -> a.mDOTDamage, WITHERING_GAZE_DOT_DAMAGE)
			.add(" magic damage every ")
			.addDuration(WITHERING_GAZE_DOT_PERIOD)
			.add(" seconds for ")
			.addDuration(a -> a.mDOTDuration, WITHERING_GAZE_DOT_DURATION_1, false, Ability::isLevelOne)
			.add(" seconds.")
			.addCooldown(WITHERING_GAZE_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<WitheringGaze> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage over time duration is increased to ")
			.addDuration(a -> a.mDOTDuration, WITHERING_GAZE_DOT_DURATION_2, false, Ability::isLevelTwo)
			.add(" seconds.")
			.addCooldown(WITHERING_GAZE_2_COOLDOWN, Ability::isLevelTwo);
	}

}
