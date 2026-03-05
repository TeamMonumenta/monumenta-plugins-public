package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class SoulLinkBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_soul_link";
	public static final String linkedTag = "soul_linked";


	private final List<LivingEntity> mLinkedMobs;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Choose what entities to link this mob to")
		public EntityTargets TARGETING_TYPE = EntityTargets.GENERIC_MOB_TARGET.setLimit(EntityTargets.Limit.CLOSER_ONE).setRange(20);
		@BossParam(help = "Whether this mob should die when the linked mob dies")
		public boolean DIE_ON_LINK_DEATH = true;
		@BossParam(help = "Whether the linked mob should die when this one does")
		public boolean KILL_LINK_ON_DEATH = false;
	}

	private final boolean mKillLinks;

	public SoulLinkBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mLinkedMobs = new ArrayList<>();
		Parameters parameters = BossParameters.getParameters(boss, identityTag, new Parameters());

		for (LivingEntity e : parameters.TARGETING_TYPE.getTargetsList(boss)) {
			if (parameters.DIE_ON_LINK_DEATH) {
				//Inverted the order so the mobs are linked the other way around
				BossManager.getInstance().manuallyRegisterBoss(e, new SoulLinkBoss(plugin, e, List.of(boss), false, true));
			}
			e.addScoreboardTag(linkedTag);
			mLinkedMobs.add(e);
		}

		mKillLinks = parameters.KILL_LINK_ON_DEATH;

		if (!mLinkedMobs.isEmpty()) {
			boss.addScoreboardTag(linkedTag);
		}
	}

	public SoulLinkBoss(Plugin plugin, LivingEntity boss, List<LivingEntity> linkedMobs, boolean dieOnLinkDeath, boolean killLinksOnDeath) {
		super(plugin, identityTag, boss);
		mKillLinks = killLinksOnDeath;
		if (dieOnLinkDeath) {
			for (LivingEntity e : linkedMobs) {
				BossManager.getInstance().manuallyRegisterBoss(e, new SoulLinkBoss(plugin, e, List.of(boss), false, true));
			}
		}
		mLinkedMobs = linkedMobs;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (mKillLinks) {
			for (LivingEntity e : mLinkedMobs) {
				e.removeScoreboardTag(linkedTag);
				e.setHealth(0);
			}
		}
	}
}
