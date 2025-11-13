package com.playmonumenta.plugins.bosses.bosses;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.effects.ProjectileIframe;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class WormSegmentBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_wormsegment";
	private @Nullable LivingEntity mHead;
	public static final int detectionRange = 64;

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(
		InfernoDamage.class,
		CustomDamageOverTime.class,
		ProjectileIframe.class);

	// Helper bosstag to force the transmission of effects


	public WormSegmentBoss(Plugin plugin, LivingEntity boss, @Nullable LivingEntity head) {
		super(plugin, identityTag, boss);
		mHead = head;
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		if (COPIED_EFFECTS.contains(event.getEffect().getClass())) {
			event.setEntity(this.getHead());
		}
	}

	public void setHead(LivingEntity head) {
		mHead = head;
	}

	public LivingEntity getHead() {
		return (mHead == null ? mBoss : mHead);
	}
}
