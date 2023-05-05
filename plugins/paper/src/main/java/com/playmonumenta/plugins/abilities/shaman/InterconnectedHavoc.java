package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class InterconnectedHavoc extends Ability {
	private static final int DAMAGE_1 = 6;
	private static final int DAMAGE_2 = 8;
	private static final int RANGE_1 = 8;
	private static final int RANGE_2 = 14;

	public static final String CHARM_DAMAGE = "Interconnected Havoc Damage";
	public static final String CHARM_RANGE = "Interconnected Havoc Range";

	private final double mRange;
	private double mDamage;
	private final List<LivingEntity> mHitMobs = new ArrayList<>();

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
					RANGE_2)
			)
			.simpleDescription("Forms lines between your active totems, dealing damage to mobs crossing those lines.")
			.displayItem(Material.CHAIN);

	public InterconnectedHavoc(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, isLevelOne() ? RANGE_1 : RANGE_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
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
				new PPLine(Particle.ENCHANTMENT_TABLE, startPoint, endPoint).countPerMeter(10).spawnAsPlayerActive(mPlayer);
				List<LivingEntity> targetMobs = EntityUtils.getMobsInLine(startPoint, endPoint, 0.75);
				targetMobs.removeIf(mHitMobs::contains);
				for (LivingEntity mob : targetMobs) {
					mHitMobs.add(mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, ClassAbility.INTERCONNECTED_HAVOC, false, false);
				}
			}
		}
		if (oneSecond) {
			mHitMobs.clear();
		}
	}
}
