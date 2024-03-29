package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface DisplayableEffect {

	Map<LivingEntity, List<String>> CACHED_LIST_MAP = new HashMap<>();
	AtomicInteger LAST_TICK = new AtomicInteger(-1);

	int getDisplayPriority();

	@Nullable Component getDisplay();

	@Nullable Component getDisplayWithoutTime();

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
		return sortEffects(getEffects(plugin, entity));
	}

	static List<DisplayableEffect> sortEffects(List<DisplayableEffect> effects) {
		effects.sort((effect1, effect2) -> effect2.getDisplayPriority() - effect1.getDisplayPriority());
		return effects;
	}

	static List<Component> getSortedEffectDisplayComponents(Plugin plugin, LivingEntity entity) {
		return getSortedEffects(plugin, entity).stream().map(DisplayableEffect::getDisplay).filter(Objects::nonNull).toList();
	}

	static List<String> getSortedEffectDisplays(Plugin plugin, LivingEntity entity) {
		int currentTick = Bukkit.getCurrentTick();
		if (LAST_TICK.get() != currentTick) {
			LAST_TICK.set(currentTick);
			CACHED_LIST_MAP.clear();
		} else {
			List<String> cachedList = CACHED_LIST_MAP.get(entity);
			if (cachedList != null) {
				return cachedList;
			}
		}

		List<String> displays = getSortedEffectDisplayComponents(plugin, entity).stream().map(MessagingUtils::legacyFromComponent).toList();
		CACHED_LIST_MAP.put(entity, displays);
		return displays;
	}
}
