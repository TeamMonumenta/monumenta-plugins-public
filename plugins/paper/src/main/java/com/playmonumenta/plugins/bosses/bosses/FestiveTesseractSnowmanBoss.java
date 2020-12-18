package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FestiveTesseractSnowmanBoss extends BossAbilityGroup {
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final int LIFETIME = 60 * 20;

	public static final String identityTag = "boss_festive_tesseract";
	public static final int detectionRange = 40;

	private int mTicksLived = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FestiveTesseractSnowmanBoss(plugin, boss);
	}

	public FestiveTesseractSnowmanBoss(Plugin plugin, LivingEntity boss) {
		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				mTicksLived += 5;

				Location loc = boss.getLocation();
				loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
				loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
				loc.getWorld().spawnParticle(Particle.SNOWBALL, loc, 2, 0.2, 0.2, 0.2, 0);
				if (mTicksLived > LIFETIME) {
					loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 0.5, 0), 30, 0.8, 0.5, 0.8, 0.05);
					boss.damage(999);
				}
			})
		);

		super.constructBoss(plugin, identityTag, boss, null, passiveSpells, detectionRange, null);
	}
}
