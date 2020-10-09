package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


public class SwingBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_swing";
	public static final int detectionRange = 30;

	private static final int RADIUS = 3;
	private static final int DURATION = 20;
	private static final int COOLDOWN = 20 * 8;
	private static final Particle.DustOptions REDSTONE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SwingBoss(plugin, boss);
	}

	public SwingBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellBaseAoE(plugin, boss, RADIUS, DURATION, COOLDOWN, false, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
						(Location loc) -> {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1, 2));
							World world = loc.getWorld();
							world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, ((double) RADIUS) / 3, ((double) RADIUS) / 3, ((double) RADIUS) / 3, 0.05);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.CRIT, loc, 1, 0.25, 0.25, 0.25, 0.05);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.65F);
						}, (Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0.3);
							world.spawnParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, REDSTONE_COLOR);
						}, (Location loc) -> {
							for (Player player : PlayerUtils.playersInRange(boss.getLocation(), RADIUS)) {
								BossUtils.bossDamage(boss, player, 35);
							}
						})));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}
