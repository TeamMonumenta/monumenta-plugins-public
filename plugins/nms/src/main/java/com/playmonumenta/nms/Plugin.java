package com.playmonumenta.nms;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.bossfights.BossManager;
import com.playmonumenta.bossfights.utils.MetadataUtils;
import com.playmonumenta.nms.bosses.Wanderer;
import com.playmonumenta.nms.bosses.evoker.EvokerNoVex;

public class Plugin extends JavaPlugin {
	@Override
	public void onEnable() {
		BossManager.registerStatelessBoss(Wanderer.identityTag, (org.bukkit.plugin.Plugin p, LivingEntity e) -> new Wanderer(p, e));
		BossManager.registerBossDeserializer(Wanderer.identityTag, (org.bukkit.plugin.Plugin p, LivingEntity e) -> Wanderer.deserialize(p, e));

		BossManager.registerStatelessBoss(EvokerNoVex.identityTag, (org.bukkit.plugin.Plugin p, LivingEntity e) -> new EvokerNoVex(p, e));
		BossManager.registerBossDeserializer(EvokerNoVex.identityTag, (org.bukkit.plugin.Plugin p, LivingEntity e) -> EvokerNoVex.deserialize(p, e));
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		MetadataUtils.removeAllMetadata(this);
	}
}
