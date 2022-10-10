package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DelveScalingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_delve_scaling";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Player Detection on initialize. Default = 100.")
		public int DETECTION = 100;

		@BossParam(help = "Boost Damage of Boss for every delve point. Default = 0.03 [3%]")
		public double DAMAGE_PER_POINT = 0.03;

		@BossParam(help = "Maximum Possible Damage Boost. Default = 2.0 [+200%]")
		public double MAX_DAMAGE_SCALE = 2.0;

		@BossParam(help = "Boost EHP of Boss for every delve point.\nWorks via Damage Reduction (DR = 1 / (1 + POINTS * EHP_PER_POINT)). Default = 0.012 [1.2%]")
		public double EHP_PER_POINT = 0.012;

		@BossParam(help = "Maximum Possible EHP Boost of Boss. Default = 1.5 [+150%]")
		public double MAX_EHP_SCALE = 1.5;
	}

	private int mPoints = 0;
	private final double mDamageMult;
	private final double mResistMult;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DelveScalingBoss(plugin, boss);
	}

	public DelveScalingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		DelveScalingBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new DelveScalingBoss.Parameters());
		Player player = EntityUtils.getNearestPlayer(mBoss.getLocation(), p.DETECTION);

		if (player != null) {
			Map<String, DelvesManager.DungeonDelveInfo> map = DelvesManager.PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(player.getUniqueId(), new HashMap<>());
			if (map.containsKey(ServerProperties.getShardName())) {
				DelvesManager.DungeonDelveInfo info = map.get(ServerProperties.getShardName());
				mPoints = info.mTotalPoint;
			}
		}
		mDamageMult = 1 + Math.min(mPoints * p.DAMAGE_PER_POINT, p.MAX_DAMAGE_SCALE);
		mResistMult = 1 / (1 + Math.min(mPoints * p.EHP_PER_POINT, p.MAX_EHP_SCALE));

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mPoints > 0) {
			event.setDamage(event.getDamage() * mDamageMult);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mPoints > 0) {
			event.setDamage(event.getDamage() * mResistMult);
		}
	}
}
