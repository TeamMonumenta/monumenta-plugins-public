package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellWebCarpet extends SpellBaseGrenadeLauncher {

	public static final String SPELL_NAME = "Web Carpet";
	public static final Material GRENADE_MATERIAL = Material.COBWEB;
	public static final int EXPLODE_DELAY = 0;
	public static final int LOBS = 1;
	public static final int LOBS_DELAY = 5;
	public static final int START_DELAY = 50;
	public static final int DURATION = 250;
	public static final int COOLDOWN = 160;
	public static final int INTERNAL_COOLDOWN = 300;
	public static final int LINGERING_DURATION = 300;
	public static final int TELEGRAPH_UNITS = 30;
	public static final double LINGERING_RADIUS = 5;
	public static final double LINGERING_RADIUS_A15_INCREASE = 1.5;
	public static final double WEB_TRAP_LAUNCH_VELOCITY = 1.2;
	public static final int WEB_TRAP_VELOCITY_TICKS = 5;
	public static final int WEB_TRAP_ATTACH_DELAY = 20;
	public static final int WEB_TRAP_CIRCLE_TICKS_MODULO = 20;
	public static final int WEB_TRAP_MAX_TICKS = 160;
	public static final double WEB_TRAP_ASC4_HEALTH = 75;
	public static final double WEB_TRAP_BASE_DAMAGE = 0.5;
	public static final double WEB_TRAP_ASC_DAMAGE_INCREASE = 0.25;

	public SpellWebCarpet(LivingEntity boss, @Nullable DepthsParty party) {
		super(Plugin.getInstance(), boss, GRENADE_MATERIAL, false, EXPLODE_DELAY, LOBS, LOBS_DELAY, DURATION, DepthsParty.getAscensionEightCooldown(COOLDOWN, party), LINGERING_DURATION, getWebTrapRadius(party),
			() -> {
				// Grenade Targets
				return PlayerUtils.playersInRange(boss.getLocation(), 150, false);
			},
			(Location loc) -> {
				// Explosion Targets
				return PlayerUtils.playersInRange(loc, getWebTrapRadius(party), true);
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
				bosss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.8f, 1.5f);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Hit Action on Explosion Targets
			},
			(Location loc) -> {
				// Ring Aesthetics
			},
			(Location loc, int ticks) -> {
				// Center Aesthetics
				if (ticks % WEB_TRAP_CIRCLE_TICKS_MODULO == 0) {
					ParticleUtils.drawCircleTelegraph(loc.clone().add(0, 0.1, 0), getWebTrapRadius(party), TELEGRAPH_UNITS, 1, 0, 0.01, true, Particle.END_ROD, Plugin.getInstance(), boss);
				}
				new PartialParticle(Particle.BLOCK_MARKER, loc.clone().add(0, 0.1, 0), 1).extra(0)
					.delta(getWebTrapRadius(party) / 2.6, 0, getWebTrapRadius(party) / 3)
					.data(Material.COBWEB.createBlockData()).spawnAsEntityActive(boss);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Lingering effect action
				// Set velocity multiple times to prevent something from cancelling the knockup.
				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						target.setVelocity(new Vector(0, WEB_TRAP_LAUNCH_VELOCITY, 0));
						mTicks++;
						if (mTicks >= WEB_TRAP_VELOCITY_TICKS) {
							cancel();
						}
					}
				}.runTaskTimer(Plugin.getInstance(), 0, 1);

				// After a delay, attach the player to the ceiling
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					spawnCocoon(target.getLocation().clone());

					new PartialParticle(Particle.CLOUD, target.getLocation(), 75).delta(1).extra(0.35).spawnAsEntityActive(boss);
					Entity entity = LibraryOfSoulsIntegration.summon(target.getLocation(), "WebTrap");
					if (entity instanceof Slime slime) {
						double trapDamage = WEB_TRAP_BASE_DAMAGE;
						if (party != null && party.getAscension() >= 4) {
							EntityUtils.setMaxHealthAndHealth(slime, DepthsParty.getAscensionScaledHealth(WEB_TRAP_ASC4_HEALTH, party));
							trapDamage += WEB_TRAP_ASC_DAMAGE_INCREASE * party.getAscension();
						}
						double finalTrapDamage = trapDamage;

						new BukkitRunnable() {
							final Slime mSlime = slime;
							final Location mCastLoc = target.getLocation().clone();
							final Location mUpperLoc = mCastLoc.clone().add(0, 1, 0);
							int mTicks = 0;
							@Override
							public void run() {
								if (!mSlime.isValid() || !boss.isValid() || mTicks > WEB_TRAP_MAX_TICKS) {
									mSlime.remove();
									clearCocoon(mCastLoc);
									this.cancel();
								}
								new PartialParticle(Particle.WAX_OFF, mUpperLoc, 3).delta(1).extra(0).spawnAsEntityActive(boss);
								mTicks++;
								if (mTicks % WEB_TRAP_CIRCLE_TICKS_MODULO == 0) {
									new PartialParticle(Particle.CLOUD, mUpperLoc, 15).delta(1).extra(0.05).spawnAsEntityActive(boss);
									// Don't keep damaging the target if they managed to escape the webs by themselves.
									if (target.getLocation().distanceSquared(mCastLoc) <= 16) {
										DamageUtils.damage(boss, target, DamageEvent.DamageType.TRUE, finalTrapDamage, null, true, false, SPELL_NAME);
									} else {
										mSlime.remove();
										clearCocoon(mCastLoc);
										this.cancel();
									}
								}
							}
						}.runTaskTimer(Plugin.getInstance(), 0, 1);
					}
				}, WEB_TRAP_ATTACH_DELAY);
			},
			() -> {
				// Additional parameters
				return new AdditionalGrenadeParameters(new Location(boss.getWorld(), -4, 4, 0), 0, START_DELAY, Broodmother.GROUND_Y_LEVEL,
					new ChargeUpManager(boss, START_DELAY, Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.WHITE, TextDecoration.BOLD)),
						BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 100), true, 1, INTERNAL_COOLDOWN, true);
			},
			(Location loc) -> {
				// Landing Location telegraph
				ParticleUtils.drawCircleTelegraph(loc, getWebTrapRadius(party), TELEGRAPH_UNITS, 1, 1, 0, false, Particle.END_ROD, Plugin.getInstance(), boss);
			}
		);
	}

	private static double getWebTrapRadius(@Nullable DepthsParty party) {
		double radius = LINGERING_RADIUS;
		if (party != null && party.getAscension() >= 15) {
			radius += LINGERING_RADIUS_A15_INCREASE;
		}
		return radius;
	}

	private static void spawnCocoon(Location loc) {
		Location cocoonLocation = loc.clone();
		checkAndSetCobweb(cocoonLocation);
		checkAndSetCobweb(cocoonLocation.clone().add(0, -1, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 1, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 2, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(1, 1, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(-1, 1, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 1, 1));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 1, -1));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 0, -1));
		checkAndSetCobweb(cocoonLocation.clone().add(0, 0, 1));
		checkAndSetCobweb(cocoonLocation.clone().add(1, 0, 0));
		checkAndSetCobweb(cocoonLocation.clone().add(-1, 0, 0));
	}

	private static void clearCocoon(Location loc) {
		Location cocoonLocation = loc.clone();
		checkAndClearCobweb(cocoonLocation);
		checkAndClearCobweb(cocoonLocation.clone().add(0, -1, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 1, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 2, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(1, 1, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(-1, 1, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 1, 1));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 1, -1));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 0, -1));
		checkAndClearCobweb(cocoonLocation.clone().add(0, 0, 1));
		checkAndClearCobweb(cocoonLocation.clone().add(1, 0, 0));
		checkAndClearCobweb(cocoonLocation.clone().add(-1, 0, 0));
	}

	private static void checkAndSetCobweb(Location loc) {
		if (loc.getBlock().getType() == Material.AIR || loc.getBlock().getType() == Material.LIGHT) {
			loc.getWorld().setBlockData(loc, Material.COBWEB.createBlockData());
		}
	}

	private static void checkAndClearCobweb(Location loc) {
		if (loc.getBlock().getType() == Material.COBWEB) {
			loc.getWorld().setBlockData(loc, Material.AIR.createBlockData());
		}
	}
}
