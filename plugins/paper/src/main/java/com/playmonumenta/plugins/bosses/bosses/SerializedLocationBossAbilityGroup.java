package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.utils.SerializationUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public abstract class SerializedLocationBossAbilityGroup extends BossAbilityGroup {
	public final Location mSpawnLoc;
	public final Location mEndLoc;

	public SerializedLocationBossAbilityGroup(Plugin plugin, String identityTag, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}
}
