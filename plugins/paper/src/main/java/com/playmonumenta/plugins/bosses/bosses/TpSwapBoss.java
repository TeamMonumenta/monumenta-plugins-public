package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.SpellTpSwapPlaces;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public class TpSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpswap";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Range to select a player to be swapped with.")
		public int TARGET_RANGE = 6;

		@BossParam(help = "Distance between player and mob at which the swap can be executed with on launch.")
		public int TELEPORT_RANGE = 6;
		public int DELAY = 100;
		public int DURATION = 50;
		public int DETECTION = 20;
		public int COOLDOWN = 12 * 20;
		public ParticlesList PARTICLE = ParticlesList.fromString("[(PORTAL,10,1,1,1,0.03)]");
	}

	private final SpellTpSwapPlaces mSpell;

	public TpSwapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		mSpell = new SpellTpSwapPlaces(plugin, boss, p.COOLDOWN, p.TARGET_RANGE, p.TELEPORT_RANGE, p.DURATION, p.PARTICLE);

		super.constructBoss(mSpell, p.DETECTION, null, p.DELAY);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mSpell.cancelIfTarget(event.getPlayer());
	}
}
