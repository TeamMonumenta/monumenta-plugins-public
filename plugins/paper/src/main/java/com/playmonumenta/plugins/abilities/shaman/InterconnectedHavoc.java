package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.InterconnectedHavocCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class InterconnectedHavoc extends Ability {
	private static final int DAMAGE_1 = 5;
	private static final int DAMAGE_2 = 7;
	private static final int RANGE_1 = 10;
	private static final int RANGE_2 = 16;
	public static final float KNOCKBACK = 0.2f;
	public static final int STUN_TIME = 15;

	public static final String CHARM_DAMAGE = "Interconnected Havoc Damage";
	public static final String CHARM_RANGE = "Interconnected Havoc Range";
	public static final String CHARM_ENHANCEMENT_KNOCKBACK = "Interconnected Havoc Knockback";
	public static final String CHARM_ENHANCEMENT_STUN = "Interconnected Havoc Stun Duration";

	private final double mRange;
	private final double mDamage;
	private final float mKnockback;
	private final int mStunTime;
	private int mClearTimer;
	private final List<LivingEntity> mHitMobs = new ArrayList<>();
	private final List<LivingEntity> mBlockedMobs = new ArrayList<>();

	public static final AbilityInfo<InterconnectedHavoc> INFO =
		new AbilityInfo<>(InterconnectedHavoc.class, "Interconnected Havoc", InterconnectedHavoc::new)
			.linkedSpell(ClassAbility.INTERCONNECTED_HAVOC)
			.scoreboardId("InterHavoc")
			.shorthandName("IH")
			.descriptions(
				String.format("Totems form a line between them with a maximum distance of %s, and mobs that cross those lines are dealt %s magic damage per second within that line.",
					RANGE_1,
					DAMAGE_1
				),
				String.format("Magic damage is increased to %s, and the maximum distance is now %s.",
					DAMAGE_2,
					RANGE_2),
				String.format("Mobs are now knocked back from the player when coming in contact " +
					"with the line for the first time and are stunned for %ss.",
					StringUtils.ticksToSeconds(STUN_TIME))
			)
			.simpleDescription("Forms lines between your active totems, dealing damage to mobs crossing those lines.")
			.displayItem(Material.CHAIN);

	private final InterconnectedHavocCS mCosmetic;

	public InterconnectedHavoc(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, isLevelOne() ? RANGE_1 : RANGE_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mKnockback = (float) CharmManager.getExtraPercent(mPlayer, CHARM_ENHANCEMENT_KNOCKBACK, KNOCKBACK);
		mStunTime = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_STUN, STUN_TIME);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new InterconnectedHavocCS());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Location> activeList = TotemicEmpowerment.getTotemLocations(mPlayer);
		if (activeList.isEmpty() || mPlayer.isDead()) {
			return;
		}
		for (int i = 0; i < activeList.size() - 1; i++) {
			Location startPoint = activeList.get(i);
			for (int j = i; j < activeList.size(); j++) {
				Location endPoint = activeList.get(j);
				if (!startPoint.getWorld().equals(endPoint.getWorld()) || startPoint.distance(endPoint) >= mRange) {
					continue;
				}
				List<LivingEntity> targetMobs = EntityUtils.getMobsInLine(startPoint, endPoint, 0.5);
				List<LivingEntity> mobsForDamage = new ArrayList<>(List.copyOf(targetMobs));
				mobsForDamage.removeIf(mHitMobs::contains);
				for (LivingEntity mob : mobsForDamage) {
					mHitMobs.add(mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, ClassAbility.INTERCONNECTED_HAVOC, false, false);
				}
				if (mobsForDamage.isEmpty()) {
					mCosmetic.havocLine(mPlayer, startPoint, endPoint);
				} else {
					mCosmetic.havocDamage(mPlayer, startPoint, endPoint);
				}
				if (isEnhanced()) {
					targetMobs.removeIf(mBlockedMobs::contains);
					for (LivingEntity mob : targetMobs) {
						mBlockedMobs.add(mob);
						MovementUtils.knockAway(mPlayer.getLocation(), mob, mKnockback, 0.6f, true);
						EntityUtils.applyStun(mPlugin, mStunTime, mob);
					}
				}
			}
		}
		if (oneSecond) {
			mClearTimer++;
			mHitMobs.clear();
			if (mClearTimer >= 30) {
				mBlockedMobs.clear();
				mClearTimer = 0;
			}
		}
	}
}
