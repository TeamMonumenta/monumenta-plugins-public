package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.BodyguardCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Bodyguard extends Ability {
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 30;
	private static final int RANGE = 25;
	private static final int RADIUS = 4;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 12;
	private static final int BUFF_DURATION = Constants.TICKS_PER_SECOND * 10;
	private static final int STUN_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final float KNOCKBACK = 0.45f;

	public static final String CHARM_COOLDOWN = "Bodyguard Cooldown";
	public static final String CHARM_RANGE = "Bodyguard Range";
	public static final String CHARM_RADIUS = "Bodyguard Stun Radius";
	public static final String CHARM_ABSORPTION = "Bodyguard Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Bodyguard Absorption Duration";
	public static final String CHARM_STUN_DURATION = "Bodyguard Stun Duration";
	public static final String CHARM_KNOCKBACK = "Bodyguard Knockback";

	public static final AbilityInfo<Bodyguard> INFO =
		new AbilityInfo<>(Bodyguard.class, "Bodyguard", Bodyguard::new)
			.linkedSpell(ClassAbility.BODYGUARD)
			.scoreboardId("Bodyguard")
			.shorthandName("Bg")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Teleport to another player, giving them and yourself absorption and stunning nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castSelf", "cast on self or others", bg -> bg.cast(true),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick().lookDirections(AbilityTrigger.LookDirection.DOWN)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.addTrigger(new AbilityTriggerInfo<>("castOthers", "cast on others only", bg -> bg.cast(false),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick()
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.IRON_CHESTPLATE);

	private final double mAbsorptionHealth;
	private final int mAbsorptionDuration;
	private final double mRange;
	private final float mKnockback;
	private final double mKnockbackRadius;
	private final int mStunDuration;

	private final BodyguardCS mCosmetic;

	public Bodyguard(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, BUFF_DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mKnockbackRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new BodyguardCS());
	}

	public boolean cast(final boolean allowSelfCast) {
		if (isOnCooldown()) {
			return false;
		}

		final World world = mPlayer.getWorld();
		final Location userLoc = mPlayer.getLocation();

		final Player targetPlayer = EntityUtils.getPlayerAtCursor(mPlayer, mRange, 0.5);
		if (targetPlayer != null) {
			mCosmetic.onBodyguardOther(mPlayer, targetPlayer, world);

			final Vector dir = userLoc.getDirection();
			final Location otherLoc = targetPlayer.getLocation().setDirection(mPlayer.getEyeLocation().getDirection());
			Location targetLoc = otherLoc.clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
			final BoundingBox box = mPlayer.getBoundingBox().shift(targetLoc.clone().subtract(mPlayer.getLocation()));
			if (LocationUtils.collidesWithBlocks(box, mPlayer.getWorld())) {
				targetLoc = otherLoc;
			}

			giveAbsorption(targetPlayer);

			if (userLoc.distance(targetLoc) > 1
				&& !ZoneUtils.hasZoneProperty(userLoc, ZoneProperty.NO_MOBILITY_ABILITIES)
				&& !ZoneUtils.hasZoneProperty(targetLoc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
				PlayerUtils.playerTeleport(mPlayer, targetLoc);
			}
		} else if (!allowSelfCast) {
			return false;
		}

		putOnCooldown();
		giveAbsorption(mPlayer);
		mCosmetic.onBodyguard(mPlayer, world, userLoc);

		for (final LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mKnockbackRadius)) {
			MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
			if (isLevelTwo()) {
				EntityUtils.applyStun(mPlugin, mStunDuration, mob);
			}
		}
		return true;
	}

	private void giveAbsorption(final Player player) {
		AbsorptionUtils.addAbsorption(player, mAbsorptionHealth, mAbsorptionHealth, mAbsorptionDuration);
	}

	private static Description<Bodyguard> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger(1, "looking directly at another player")
			.add(" within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks to charge to them. Upon arriving, knock away all mobs within ")
			.add(a -> a.mKnockbackRadius, RADIUS)
			.add(" blocks. Both you and the other player gain ")
			.add(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH_1, false, Ability::isLevelOne)
			.add(" absorption health for ")
			.addDuration(a -> a.mAbsorptionDuration, BUFF_DURATION)
			.add(" seconds. ")
			.addTrigger(0)
			.add(" to cast on yourself.")
			.addCooldown(COOLDOWN);
	}

	private static Description<Bodyguard> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The absorption health is increased to ")
			.add(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH_2, false, Ability::isLevelTwo)
			.add(". Additionally, affected mobs are stunned for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds.");
	}
}
