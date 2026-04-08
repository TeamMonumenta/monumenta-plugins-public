package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.InterconnectedHavocCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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

	public static final AbilityInfo<InterconnectedHavoc> INFO =
		new AbilityInfo<>(InterconnectedHavoc.class, "Interconnected Havoc", InterconnectedHavoc::new)
			.linkedSpell(ClassAbility.INTERCONNECTED_HAVOC)
			.scoreboardId("InterHavoc")
			.shorthandName("IH")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Forms lines between your active totems, dealing damage to mobs crossing those lines.")
			.quest216Message("-------e-------r-------")
			.displayItem(Material.CHAIN);

	private final double mRange;
	private final double mDamage;
	private final float mKnockback;
	private final int mStunTime;
	private final InterconnectedHavocCS mCosmetic;

	private final List<LivingEntity> mHitMobs = new ArrayList<>();
	private final List<LivingEntity> mBlockedMobs = new ArrayList<>();
	private int mClearTimer;

	public InterconnectedHavoc(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, isLevelOne() ? RANGE_1 : RANGE_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_KNOCKBACK, KNOCKBACK);
		mStunTime = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_STUN, STUN_TIME);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new InterconnectedHavocCS());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Location> activeList = ShamanPassiveManager.getTotemLocations(mPlayer);
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

	private static Description<InterconnectedHavoc> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Summoning multiple *Totems* connects them").styles(Shaman.TOTEM_COLOR)
			.addLine("with lines that periodically damage mobs")
			.addLine("touching them.")
			.addLine()
			.addStat("Damage: %d1 (s) every 1s")
				.statValues(stat(a -> a.mDamage, DAMAGE_1))
			.addStat("Max Distance: %r1")
				.statValues(stat(a -> a.mRange, RANGE_1))
			.addDashedLine();
	}

	private static Description<InterconnectedHavoc> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Interconnected Havoc*'s damage").styles(UNDERLINED)
			.addLine("and max totem distance.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s) every 1s")
				.statValues(stat(DAMAGE_1), stat(a -> a.mDamage, DAMAGE_2))
			.addStatComparison("Max Distance: %r1 -> %r2")
				.statValues(stat(RANGE_1), stat(a -> a.mRange, RANGE_2))
			.addDashedLine();
	}

	private static Description<InterconnectedHavoc> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Mobs are now knocked back and stunned")
			.addLine("upon touching a line for the first time.")
			.addLine()
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunTime, STUN_TIME))
			.addDashedLine();
	}
}
