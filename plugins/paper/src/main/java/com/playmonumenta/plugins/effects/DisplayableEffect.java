package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface DisplayableEffect {

	int getDisplayPriority();

	@Nullable String getDisplay();

	static List<DisplayableEffect> getEffects(Plugin plugin, LivingEntity entity) {
		List<DisplayableEffect> effects = new ArrayList<>(plugin.mEffectManager.getPriorityEffects(entity).values());
		effects.addAll(AbsorptionUtils.getAbsorptionDisplayables(entity));
		if (entity instanceof Player player) {
			effects.addAll(GalleryManager.getGalleryEffects(player));
			effects.add(BrownPolarityDisplay.getPolarityDisplay(player));
		}
		effects.removeIf(Objects::isNull);
		return effects;
	}

	static List<DisplayableEffect> getSortedEffects(Plugin plugin, LivingEntity entity) {
		List<DisplayableEffect> effects = getEffects(plugin, entity);
		effects.sort((effect1, effect2) -> effect2.getDisplayPriority() - effect1.getDisplayPriority());
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
