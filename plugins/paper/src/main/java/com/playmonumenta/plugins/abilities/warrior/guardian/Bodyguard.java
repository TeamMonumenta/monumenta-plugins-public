package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.BodyguardCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Bodyguard extends Ability {
	private static final int COOLDOWN = 30 * 20;
	private static final int RANGE = 25;
	private static final int RADIUS = 4;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 12;
	private static final int BUFF_DURATION = 20 * 10;
	private static final int STUN_DURATION = 20 * 3;
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
			.descriptions(
				"Left-click the air twice while looking directly at another player within 25 blocks to charge to them (cannot be used in safezones). " +
					"Upon arriving, knock away all mobs within 4 blocks. Both you and the other player gain 8 absorption health for 10 seconds. " +
					"Left-click twice while looking down to cast on yourself. Cooldown: 30s.",
				"Absorption increased to 12 health. Additionally, affected mobs are stunned for 3 seconds.")
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

	private final BodyguardCS mCosmetic;

	public Bodyguard(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BodyguardCS());
	}

	public boolean cast(boolean allowSelfCast) {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location userLoc = mPlayer.getLocation();

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		Player targetPlayer = EntityUtils.getPlayerAtCursor(mPlayer, range);
		if (targetPlayer != null) {
			mCosmetic.onBodyguardOther(mPlayer, targetPlayer, world);

			Vector dir = userLoc.getDirection();
			Location targetLoc = targetPlayer.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);

			giveAbsorption(targetPlayer);

			if (userLoc.distance(targetLoc) > 1
				    && !ZoneUtils.hasZoneProperty(userLoc, ZoneProperty.NO_MOBILITY_ABILITIES)
				    && !ZoneUtils.hasZoneProperty(targetLoc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
				mPlayer.teleport(targetLoc);
			}
		} else if (!allowSelfCast) {
			return false;
		}

		putOnCooldown();

		mCosmetic.onBodyguard(mPlayer, world, userLoc);

		giveAbsorption(mPlayer);

		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		int duration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS))) {
			MovementUtils.knockAway(mPlayer, mob, knockback, true);
			if (isLevelTwo()) {
				EntityUtils.applyStun(mPlugin, duration, mob);
			}
		}
		return true;
	}

	private void giveAbsorption(Player player) {
		AbsorptionUtils.addAbsorption(player, mAbsorptionHealth, mAbsorptionHealth, CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, BUFF_DURATION));
	}
}
