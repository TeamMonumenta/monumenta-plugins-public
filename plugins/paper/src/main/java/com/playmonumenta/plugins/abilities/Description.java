package com.playmonumenta.plugins.abilities;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface Description<T extends Ability> {
	Component get(@Nullable T ability, @Nullable Player player);
}
