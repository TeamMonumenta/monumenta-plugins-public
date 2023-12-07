package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellEggThrow extends SpellBaseGrenadeLauncher {

	public static final String SPELL_NAME = "Egg Throw";
	public static final Material GRENADE_MATERIAL = Material.WHITE_TERRACOTTA;
	public static final int EXPLODE_DELAY = 0;
	public static final int LOBS = 1;
	public static final int LOBS_DELAY = 5;
	public static final int START_DELAY = 30;
	public static final int DURATION = 250;
	public static final int COOLDOWN = 700;
	public static final double DAMAGE = 0;
	public static final int LINGERING_DURATION = 300;
	public static final int MAX_GROUND_TARGETS = 2;
	public static final int MAX_FLYING_TARGETS = 1;
	public static final int GROUND_SPAWNS = 3;
	public static final int FLYING_SPAWNS = 3;
	public static final int EGG_HATCH_TIME = 200;

	public SpellEggThrow(LivingEntity boss, @Nullable DepthsParty party) {
		super(Plugin.getInstance(), boss, GRENADE_MATERIAL, false, EXPLODE_DELAY, LOBS, LOBS_DELAY, DURATION, DepthsParty.getAscensionEigthCooldown(COOLDOWN, party), LINGERING_DURATION, 0,
				() -> {
					// Grenade Targets
					// Ground spider eggs
					List<Entity> groundTargets = new ArrayList<>(boss.getLocation().getNearbyEntities(60, 60, 60).stream().filter(e -> e.getScoreboardTags().contains("ground_egg_target")).toList());
					Collections.shuffle(groundTargets);
					// Flying spider eggs
					List<Entity> flyingTargets = new ArrayList<>(boss.getLocation().getNearbyEntities(60, 60, 60).stream().filter(e -> e.getScoreboardTags().contains("flying_egg_target")).toList());
					Collections.shuffle(flyingTargets);
					// Choose MAX_GROUND_TARGETS and MAX_FLYING_TARGETS
					List<Entity> finalTargets = new ArrayList<>(groundTargets.stream().limit(MAX_GROUND_TARGETS).toList());
					finalTargets.addAll(flyingTargets.stream().limit(MAX_FLYING_TARGETS).toList());
					return finalTargets;
				},
				(Location loc) -> {
					// Explosion Targets
					return Collections.emptyList();
				},
				(LivingEntity bosss, Location loc) -> {
					// Boss Aesthetics
					bosss.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 3f, 0.5f);
				},
				(LivingEntity bosss, Location loc) -> {
					// Grenade Aesthetics
					new PartialParticle(Particle.CRIT, loc, 5).extra(0.05).spawnAsEntityActive(bosss);
				},
				(LivingEntity bosss, Location loc) -> {
					// Explosion Aesthetics
					bosss.getWorld().playSound(loc, Sound.ENTITY_TURTLE_EGG_BREAK, SoundCategory.HOSTILE, 1.2f, 2f);
					Entity entity = LibraryOfSoulsIntegration.summon(loc.clone().add(0.5, 0, 0.5), "SpiderEgg");
					if (entity instanceof Slime slime) {
						buildEgg(loc);
						new BukkitRunnable() {
							final Slime mSlime = slime;
							final Location mLoc = loc.clone();
							final boolean mIsGround = isGroundEgg(mLoc);
							int mTicks = 0;
							@Override
							public void run() {
								if (mTicks >= EGG_HATCH_TIME) {
									// Hatch with spawning spiders
									bosss.getWorld().playSound(mLoc, Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 1.2f, 1f);
									if (mIsGround) {
										for (int i = 0; i < GROUND_SPAWNS; i++) {
											spawnGroundSpider(mLoc);
										}
									} else {
										for (int i = 0; i < FLYING_SPAWNS; i++) {
											spawnFlyingSpider(mLoc);
										}
									}
									mSlime.remove();
									new PartialParticle(Particle.FIREWORKS_SPARK, mLoc.clone().add(0, 1, 0), 75).extra(0.1).spawnAsEntityActive(boss);
									new PartialParticle(Particle.END_ROD, mLoc.clone().add(0, 1, 0), 75).extra(0.1).spawnAsEntityActive(boss);
									this.cancel();
								}
								if (!mSlime.isValid()) {
									// Egg was broken before it hatched
									bosss.getWorld().playSound(loc, Sound.ENTITY_TURTLE_EGG_BREAK, SoundCategory.HOSTILE, 1.2f, 0.5f);
									removeEgg(mLoc);
									mSlime.remove();
									new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc.clone().add(0, 1, 0), 75).extra(0.1).spawnAsEntityActive(boss);
									this.cancel();
								}
								mTicks++;
							}
						}.runTaskTimer(Plugin.getInstance(), 0, 1);
					}
				},
				(LivingEntity bosss, LivingEntity target, Location loc) -> {
					// Hit Action on Explosion Targets
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
					return new AdditionalGrenadeParameters(new Location(boss.getWorld(), -4, 8, 0), 0, START_DELAY, -1,
						new ChargeUpManager(boss, START_DELAY, Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.YELLOW, TextDecoration.BOLD)),
							BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 100), true, 1, true, true);
				},
				(Location loc) -> {
					// Landing Location telegraph
				}
		);
	}

	private static boolean isGroundEgg(Location loc) {
		return loc.getNearbyEntities(2, 2, 2).stream().anyMatch(e -> e.getScoreboardTags().contains("ground_egg_target"));
	}

	private static void buildEgg(Location loc) {
		Block block = loc.getBlock();

		TemporaryBlockChangeManager.INSTANCE.changeBlock(block, Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(0, 0, 1), Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(1, 0, 0), Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(0, 1, 0), Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(1, 1, 0), Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(0, 2, 0), Material.WHITE_TERRACOTTA, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(-1, 0, 0), Material.COBWEB, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(1, 0, -1), Material.COBWEB, EGG_HATCH_TIME, true);
		TemporaryBlockChangeManager.INSTANCE.changeBlock(block.getRelative(0, 1, 1), Material.COBWEB, EGG_HATCH_TIME, true);
	}

	private static void removeEgg(Location loc) {
		Block block = loc.getBlock();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(List.of(
			block,
			block.getRelative(0, 0, 1),
			block.getRelative(1, 0, 0),
			block.getRelative(0, 1, 0),
			block.getRelative(1, 1, 0),
			block.getRelative(0, 2, 0)
		), Material.WHITE_TERRACOTTA);

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(List.of(
			block.getRelative(-1, 0, 0),
			block.getRelative(1, 0, -1),
			block.getRelative(0, 1, 1)
		), Material.COBWEB);
	}

	private static void spawnGroundSpider(Location loc) {
		LoSPool.fromString("~DD2_Broodmother_EggGround").spawn(loc);
	}

	private static void spawnFlyingSpider(Location loc) {
		LoSPool.fromString("~DD2_Broodmother_EggPlatform").spawn(loc);
	}
}
