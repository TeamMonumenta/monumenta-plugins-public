package com.playmonumenta.plugins.abilities;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface Description<T extends Ability> {
	default Component get() {
		return get(null, null);
	}

	default Component get(@Nullable Player player) {
		return get(null, player);
	}

	Component get(@Nullable T ability, @Nullable Player player);
}
