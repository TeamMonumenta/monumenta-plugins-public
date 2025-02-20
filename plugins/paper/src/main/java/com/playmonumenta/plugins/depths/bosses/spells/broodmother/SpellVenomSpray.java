package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.RandomUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class SpellVenomSpray extends SpellBaseGrenadeLauncher {

	public static final String SPELL_NAME = "Venom Spray";
	public static final double BLAST_RADIUS = 4;
	public static final Material GRENADE_MATERIAL = Material.LIME_CONCRETE_POWDER;
	public static final int EXPLODE_DELAY = 0;
	public static final int LOBS = 4;
	public static final int LOBS_A8_INCREASE = 2;
	public static final int LOBS_A15_INCREASE = 2;
	public static final int LOBS_DELAY = 5;
	public static final int LOBS_DELAY_A8_DECREASE = 2;
	public static final int START_DELAY = 50;
	public static final int DURATION = 250;
	public static final int COOLDOWN = 160;
	public static final double DAMAGE = 20;
	public static final List<Material> GROUND_QUAKE_BLOCKS = List.of(Material.LIME_GLAZED_TERRACOTTA, Material.LIME_CONCRETE, Material.LIME_WOOL, Material.LIME_CONCRETE_POWDER);

	public SpellVenomSpray(LivingEntity boss, @Nullable DepthsParty party) {
		super(Plugin.getInstance(), boss, GRENADE_MATERIAL, false, EXPLODE_DELAY, getLobs(party), getLobDelay(party), DURATION, DepthsParty.getAscensionEightCooldown(COOLDOWN, party), 0, 0,
			() -> {
				// Grenade Targets
				return PlayerUtils.playersInRange(boss.getLocation(), 150, true);
			},
			(Location loc) -> {
				// Explosion Targets
				return PlayerUtils.playersInRange(loc, BLAST_RADIUS, true);
			},
			(LivingEntity bosss, Location loc) -> {
				// Boss Aesthetics
				bosss.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 3f, 0.5f);
			},
			(LivingEntity bosss, Location loc) -> {
				// Grenade Aesthetics
				new PartialParticle(Particle.REDSTONE, loc, 3).delta(0.5, 0.5, 0.5)
					.extra(0.2).data(new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1))
					.spawnAsEntityActive(boss);
				new PartialParticle(Particle.REDSTONE, loc, 3).delta(0.5, 0.5, 0.5)
					.extra(0.2).data(new Particle.DustOptions(Color.fromRGB(0, 120, 0), 1))
					.spawnAsEntityActive(boss);
				new PartialParticle(Particle.END_ROD, loc, 1).extra(0).spawnAsEntityActive(bosss);
			},
			(LivingEntity bosss, Location loc) -> {
				// Explosion Aesthetics
				for (int i = 0; i < 50; i++) {
					double offsetX = RandomUtils.nextDouble(0, BLAST_RADIUS * 2) - BLAST_RADIUS;
					double offsetY = RandomUtils.nextDouble(0, BLAST_RADIUS / 2);
					double offsetZ = RandomUtils.nextDouble(0, BLAST_RADIUS * 2) - BLAST_RADIUS;
					Location spawnLoc = loc.clone().add(offsetX, offsetY, offsetZ);
					new PartialParticle(Particle.SPELL_MOB, spawnLoc, 1).extra(1).directionalMode(true)
						.delta(0, 1, 0).spawnAsEntityActive(bosss);
				}
				bosss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.5f);
				DisplayEntityUtils.groundBlockQuake(loc, BLAST_RADIUS, GROUND_QUAKE_BLOCKS, new Display.Brightness(12, 12), 0.002);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Hit Action on Explosion Targets
				DamageUtils.damage(bosss, target, DamageEvent.DamageType.BLAST, DAMAGE, null, true, false, SPELL_NAME);
				PotionUtils.applyPotion(bosss, target, new PotionEffect(PotionEffectType.POISON, 100, 1));
				PotionUtils.applyPotion(bosss, target, new PotionEffect(PotionEffectType.WITHER, 100, 1));
				if (party != null && party.getAscension() >= 4) {
					EntityUtils.applyVulnerability(Plugin.getInstance(), 100, Broodmother.getVulnerabilityAmount(party), target);
				}
			},
			(Location loc) -> {
				// Ring Aesthetics
			},
			(Location loc, int ticks) -> {
				// Center Aesthetics
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Lingering effect action
			},
			() -> {
				// Additional parameters
				return new AdditionalGrenadeParameters(new Location(boss.getWorld(), -4, 4, 0), 8, START_DELAY, Broodmother.GROUND_Y_LEVEL,
					new ChargeUpManager(boss, START_DELAY, Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.GREEN, TextDecoration.BOLD)),
						BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, 100), false, 0, 0, true);
			},
			(Location loc) -> {
				// Landing Location telegraph
				new PPCircle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 0.1, 0), BLAST_RADIUS).ringMode(true).countPerMeter(2).spawnAsBoss();
				new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 0.1, 0), 8).delta(0.1, 0, 0.1).spawnAsBoss();
			}
		);
	}

	private static int getLobs(@Nullable DepthsParty party) {
		int lobs = LOBS;
		if (party != null) {
			if (party.getAscension() >= 8) {
				lobs += LOBS_A8_INCREASE;
			}
			if (party.getAscension() >= 15) {
				lobs += LOBS_A15_INCREASE;
			}
		}
		return lobs;
	}

	private static int getLobDelay(@Nullable DepthsParty party) {
		int lobDelay = LOBS_DELAY;
		if (party != null && party.getAscension() >= 8) {
			lobDelay -= LOBS_DELAY_A8_DECREASE;
		}
		return lobDelay;
	}
}
