package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.NavigableSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class SkySeekersGrace extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "SkySeekersGrace";
	public static final String effectID = "SkySeekersGrace";
	private static final String SOURCE = "SkySeekers";
	private static final int TAG_DURATION = 10000 * 20;
	private static final int DUR = 4 * 20;
	private static final double DAMAGE_PERCENT = 0.075;
	private static final double DAMAGE_PENALTY = -0.075;
	private static final double SPEED_PERCENT = 0.1;
	private static final String ATTACK_BUFF = "SkySeekersGraceDamage";
	private static final String SPEED_BUFF = "SkySeekersGraceSpeed";
	private static final String ATTACK_DEBUFF = "SkySeekersGraceDebuff";

	public SkySeekersGrace(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onKill(EntityDeathEvent event, Player player) {
		if (noSkySeekersTagForPlayer(event.getEntity(), player)) {
			Plugin.getInstance().mEffectManager.addEffect(player, ATTACK_BUFF, new PercentDamageDealt(DUR, DAMAGE_PERCENT));
			Plugin.getInstance().mEffectManager.addEffect(player, SPEED_BUFF, new PercentSpeed(DUR, SPEED_PERCENT, SPEED_BUFF));
		} else {
			Plugin.getInstance().mEffectManager.addEffect(player, ATTACK_DEBUFF, new PercentDamageDealt(DUR, DAMAGE_PENALTY));
		}
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity damager) {
		Plugin.getInstance().mEffectManager.addEffect(damager, SOURCE + entity.getName(), new SkySeekersTag(TAG_DURATION, entity));
	}

	private boolean noSkySeekersTagForPlayer(LivingEntity entity, LivingEntity player) {
		NavigableSet<SkySeekersTag> skySeekersTags = Plugin.getInstance().mEffectManager.getEffects(entity, SkySeekersTag.class);
		for (SkySeekersTag tag : skySeekersTags) {
			if (tag.getPlayer() == player) {
				return false;
			}
		}
		return true;
	}

	public static SkySeekersGrace deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		return new SkySeekersGrace(duration);
	}

	@Override
	public String toString() {
		return String.format("SkySeekersGrace duration:%d", this.getDuration());
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Sky Seeker's Grace";
	}
}
