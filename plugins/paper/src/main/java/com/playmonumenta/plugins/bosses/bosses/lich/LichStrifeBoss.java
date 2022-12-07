package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.lich.horseman.SpellLichSinisterReach;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class LichStrifeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_strife";
	public static final int detectionRange = 50;

	Location mCenter;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LichStrifeBoss(plugin, boss);
	}

	public LichStrifeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mCenter = Lich.getLichSpawn();

		Location loc = mBoss.getLocation().clone().add(0, 0, 23);
		int playercount = Lich.playersInRange(loc, detectionRange, true).size();
		double hpdel = 425;
		double hp = (int) (hpdel * (1 + (1 - 1 / Math.E) * Math.log(playercount)));
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
		mBoss.setHealth(hp);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellLichSinisterReach(plugin, mBoss, mCenter, detectionRange)
		));

		List<Spell> passives = Arrays.asList(
			new SpellBossBlockBreak(mBoss, mCenter.getY(), 1, 3, 1, false, false),
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss.getVehicle(), mCenter, b -> loc.distance(b.getLocation()) > 80),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss.getVehicle(), mCenter, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                               b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                               b.getLocation().getBlock().getType() == Material.LAVA)
		);

		super.constructBoss(activeSpells, passives, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		LivingEntity horse = (LivingEntity) mBoss.getVehicle();
		if (horse != null) {
			horse.setHealth(0);
		}
	}
}
