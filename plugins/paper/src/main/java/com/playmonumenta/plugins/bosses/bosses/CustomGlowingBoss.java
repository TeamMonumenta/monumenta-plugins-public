package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.CustomGlowingSpell;
import org.bukkit.entity.LivingEntity;

public class CustomGlowingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_glowing_spread";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Choose what entities to give the glowing to")
		public EntityTargets TARGETING_TYPE = EntityTargets.GENERIC_MOB_TARGET.setLimit(EntityTargets.Limit.CLOSER_ONE).setRange(20);
		@BossParam(help = "What color other entities glow, case sensitive, defaults to black")
		public String OTHER_COLOR = "black";
		@BossParam(help = "What color self entities glow, case sensitive, defaults to white")
		public String SELF_COLOR = "white";
		@BossParam(help = "Range in which the entity applies glowing")
		public int RADIUS = 20;
		@BossParam(help = "Glowing priority")
		public int PRIORITY = 101;
		@BossParam(help = "Whether passengers are considered self or other")
		public boolean PASSENGER_SELF = true;
		@BossParam(help = "Whether to glow itself and, if passengerself is set to true passengers")
		public boolean GLOW_SELF = true;
	}

	public CustomGlowingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		CustomGlowingBoss.Parameters parameters = BossParameters.getParameters(boss, identityTag, new CustomGlowingBoss.Parameters());
		CustomGlowingSpell mGlowSpell = new CustomGlowingSpell(boss, parameters.OTHER_COLOR, parameters.SELF_COLOR, parameters.TARGETING_TYPE, parameters.PRIORITY, parameters.PASSENGER_SELF, parameters.GLOW_SELF);
		super.constructBoss(mGlowSpell, parameters.RADIUS, null, 0);
	}
}
