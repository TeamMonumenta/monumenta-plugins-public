package com.playmonumenta.plugins.guis.peb;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.particle.ParticleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Material;

final class PartialParticlesPage extends PebPage {
	PartialParticlesPage(PebGui gui) {
		super(gui, Material.COMPARATOR, "Technical Options", "Technical options");
	}

	private Component formatLore(int value) {
		float h = Math.max(0.0f, (float) value / 200);
		TextColor color = TextColor.color(HSVLike.hsvLike(h / 3.0f, 1.0f, 1.0f));
		return Component.text("Value: ").append(Component.text(value + "%", color));
	}

	private void placeSetting(String description, Material material, ParticleCategory category, boolean emoji, int row, int col) {
		Preconditions.checkArgument(category.mObjectiveName != null, "illegal category");
		final var rValue = ReactiveValue.scoreboard(mGui, category.mObjectiveName, 100);

		entry(
			material,
			category.mDisplayName,
			description + ". Left click to increase, right click to decrease." + (emoji ? "" : " Hold shift to increase/decrease in smaller steps.")
		).lore(formatLore(rValue.get())).onClick(event -> {
			int value = rValue.get();
			if (emoji) {
				// Only 4 options: 100%, 50%, 25%, 0% (1x, 0.5x, 0.25x, 0x resolution)
				value = (int) (value * (event.isLeftClick() ? 2.0 : 0.5));
				// Handle 0
				if (value == 0 && event.isLeftClick()) {
					value = 25;
				} else if (value < 25) {
					value = 0;
				}
				value = Math.min(100, value);
			} else {
				value += (event.isLeftClick() ? 1 : -1) * (event.isShiftClick() ? 5 : 20);
				value = Math.max(0, Math.min(value, ParticleManager.MAX_PARTIAL_PARTICLE_VALUE));
			}
			rValue.set(value);
			ParticleManager.updateParticleSettings(getPlayer());
		}).set(row, col);
	}

	@Override
	protected void render() {
		super.render();
		placeSetting("Particle multiplier for your own emojis", Material.GLOW_INK_SAC, ParticleCategory.OWN_EMOJI, true, 1, 2);
		placeSetting("Particle multiplier for other players' emojis", Material.GLOW_INK_SAC, ParticleCategory.OTHER_EMOJI, true, 1, 6);

		placeSetting("Particle multiplier for your own active abilities", Material.PLAYER_HEAD, ParticleCategory.OWN_ACTIVE, false, 2, 1);
		placeSetting("Particle multiplier for your own passive abilities", Material.FIREWORK_STAR, ParticleCategory.OWN_PASSIVE, false, 2, 2);
		placeSetting("Particle multiplier for active effects on you, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OWN_BUFF, false, 2, 3);

		placeSetting("Particle multiplier for other players' active abilities", Material.PLAYER_HEAD, ParticleCategory.OTHER_ACTIVE, false, 2, 5);
		placeSetting("Particle multiplier for other players' passive abilities", Material.FIREWORK_STAR, ParticleCategory.OTHER_PASSIVE, false, 2, 6);
		placeSetting("Particle multiplier for active effects on other players, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OTHER_BUFF, false, 2, 7);

		placeSetting("Particle multiplier for bosses' abilities", Material.DRAGON_HEAD, ParticleCategory.BOSS, false, 4, 3);
		placeSetting("Particle multiplier for active effects on enemies, e.g. Spellshock's Static", Material.ENDER_PEARL, ParticleCategory.ENEMY_BUFF, false, 4, 4);
		placeSetting("Particle multiplier for non-boss enemies' abilities", Material.ZOMBIE_HEAD, ParticleCategory.ENEMY, false, 4, 5);
	}
}
