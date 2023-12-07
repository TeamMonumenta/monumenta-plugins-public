package com.playmonumenta.plugins.abilities;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface Description<T extends Ability> {
	default Component get() {
		return get(null);
	}

	Component get(@Nullable T ability);
}
