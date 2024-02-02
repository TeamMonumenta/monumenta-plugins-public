package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SoothingCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicCombos;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrigidCombos;
import com.playmonumenta.plugins.depths.abilities.shadow.DarkCombos;
import com.playmonumenta.plugins.depths.abilities.windwalker.WindsweptCombos;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DeepGodsEndowment extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "DeepGodsEndowment";
	public static final String effectID = "DeepGodsEndowment";

	public static final int HIT_REQUIREMENT = 5;
	public static final String COOLDOWN_EFFECT = "DeepGodsEndowmentCooldownEffect";

	private int mComboCount = 0;

	public DeepGodsEndowment(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (entity instanceof Player player && DepthsUtils.isValidComboAttack(event, player) && !(mComboCount == HIT_REQUIREMENT - 1 && Plugin.getInstance().mEffectManager.hasEffect(player, COOLDOWN_EFFECT))) {
			mComboCount++;

			if (mComboCount >= HIT_REQUIREMENT) {
				mComboCount = 0;
				Plugin.getInstance().mEffectManager.addEffect(player, COOLDOWN_EFFECT, new OnHitTimerEffect(DepthsCombosAbility.COOLDOWN_DURATION, 1));

				int randInt = FastUtils.RANDOM.nextInt(0, 6);
				switch (randInt) {
					case 0 -> SoothingCombos.activate(player);
					case 1 -> EarthenCombos.activate(enemy, player);
					case 2 -> VolcanicCombos.activate(enemy, player);
					case 3 -> FrigidCombos.activate(enemy, player);
					case 4 -> DarkCombos.activate(enemy, player);
					case 5 -> WindsweptCombos.activate(enemy, player);
					default -> {
					}
				}
			}
		}
	}

	public static DeepGodsEndowment deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new DeepGodsEndowment(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Deep God's Endowment";
	}

	@Override
	public String toString() {
		return String.format("DeepGodsEndowment duration:%d", this.getDuration());
	}
}
