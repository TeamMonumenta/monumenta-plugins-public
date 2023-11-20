package com.playmonumenta.plugins.particle.colors;

import org.bukkit.Color;

public interface BlendMode {

	Color apply(Color background, Color foreground);

}
