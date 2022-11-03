package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DisplayableEffect {

	int getDuration();

	@Nullable String getDisplay();

	static List<DisplayableEffect> getEffects(Plugin plugin, LivingEntity entity) {
		List<DisplayableEffect> effects = new ArrayList<>(plugin.mEffectManager.getPriorityEffects(entity).values());
		effects.addAll(AbsorptionUtils.getAbsorptionDisplayables(entity));
		return effects;
	}

	static List<DisplayableEffect> getSortedEffects(Plugin plugin, LivingEntity entity) {
		List<DisplayableEffect> effects = getEffects(plugin, entity);
		effects.sort((effect1, effect2) -> effect2.getDuration() - effect1.getDuration());
		return effects;
	}

	static List<String> getSortedEffectDisplays(Plugin plugin, LivingEntity entity) {
		List<String> displays = new ArrayList<>();
		for (DisplayableEffect effect : getSortedEffects(plugin, entity)) {
			String display = effect.getDisplay();
			if (display != null) {
				displays.add(display);
			}
		}
		return displays;
	}
}
