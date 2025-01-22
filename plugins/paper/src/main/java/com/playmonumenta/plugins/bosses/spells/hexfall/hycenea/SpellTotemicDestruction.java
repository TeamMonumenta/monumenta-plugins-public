package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.DeathImmunity;
import com.playmonumenta.plugins.effects.hexfall.DeathVulnerability;
import com.playmonumenta.plugins.effects.hexfall.LifeImmunity;
import com.playmonumenta.plugins.effects.hexfall.LifeVulnerability;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellTotemicDestruction extends Spell {
	public static String ABILITY_NAME = "Totemic Destruction";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mCastTime;
	private final Location mSpawnLoc;
	private final int mCooldown;
	private final ChargeUpManager mChargeUp;

	public SpellTotemicDestruction(Plugin plugin, LivingEntity boss, int range, int castTime, Location spawnLoc, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCastTime = castTime;
		mSpawnLoc = spawnLoc;
		mCooldown = cooldown;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {

		List<Entity> totems = new ArrayList<>();
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains("Hycenea_Island")) {
				e.addScoreboardTag("Hycenea_TotemicDestruction_Target");
			}

			if (e.getScoreboardTags().contains("boss_totemplatform")) {
				totems.add(e);
			}
		}

		int balance = 0;
		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			boolean life = balance > 0;
			if (balance == 0) {
				life = Math.random() < 0.5;
			}

			if (life) {
				mPlugin.mEffectManager.addEffect(player, LifeVulnerability.GENERIC_NAME, new LifeVulnerability(mCastTime + 5));
				balance--;
			} else {
				mPlugin.mEffectManager.addEffect(player, DeathVulnerability.GENERIC_NAME, new DeathVulnerability(mCastTime + 5));
				balance++;
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {

			@Override
			public void run() {
				totems.removeIf(Entity::isDead);

				float progress = (float) mChargeUp.getTime() / mCastTime;

				if (mChargeUp.getTime() % 40 == 0) {
					for (Player viewer : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						viewer.playSound(viewer, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, progress + 0.5f, progress + 1f);
						viewer.playSound(viewer, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, progress + 0.5f, progress + 1f);
					}
				}

				if (mChargeUp.getTime() % 40 == 10) {
					new PPSpiral(Particle.REDSTONE, mBoss.getLocation(), 3)
						.data(new Particle.DustOptions(Color.fromRGB(0, 204, 0), 1f))
						.countPerBlockPerCurve(12)
						.spawnAsBoss();
				} else if (mChargeUp.getTime() % 40 == 30) {
					new PPSpiral(Particle.REDSTONE, mBoss.getLocation(), 3)
						.data(new Particle.DustOptions(Color.fromRGB(153, 76, 37), 1f))
						.countPerBlockPerCurve(12)
						.spawnAsBoss();
				}

				if (mChargeUp.getTime() % 10 == 0) {
					for (Entity totem : totems) {
						boolean lifeOrDeath = totem.getScoreboardTags().contains("boss_totemplatform[lifeordeath=true]");

						for (int i = 0; i < 4; i++) {
							List<Location> locs = Arrays.asList(
								totem.getLocation().clone(),
								totem.getLocation().clone().add(0, 1, 0).add(new Vector(3, 0, 0).rotateAroundY(Math.toRadians(90 * i))),
								totem.getLocation().clone().add(0, 2, 0),
								totem.getLocation().clone().add(0, 4, 0)
							);

							new PPBezier(Particle.REDSTONE, locs)
								.count(5)
								.data(new Particle.DustOptions(lifeOrDeath ? Color.fromRGB(0, 204, 0) : Color.fromRGB(153, 76, 37), 1.65f))
								.spawnAsBoss();
						}
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);

						if ((mPlugin.mEffectManager.hasEffect(player, DeathVulnerability.class) && !mPlugin.mEffectManager.hasEffect(player, DeathImmunity.class))
							|| (mPlugin.mEffectManager.hasEffect(player, LifeVulnerability.class) && !mPlugin.mEffectManager.hasEffect(player, LifeImmunity.class))) {
							PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME);
						}
					}

					for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
						if (e.getScoreboardTags().contains("Hycenea_Island")) {
							if (e.getScoreboardTags().contains("Hycenea_TotemicDestruction_ShieldActive")) {
								new SpellDestroyTotemPlatform(mPlugin, e.getScoreboardTags().contains("Hycenea_Island_Life"), e, mSpawnLoc).run();
							}
							e.removeScoreboardTag("Hycenea_TotemicDestruction_Target");
							e.removeScoreboardTag("Hycenea_TotemicDestruction_ShieldActive");
						}
					}

					new PPExplosion(Particle.EXPLOSION_HUGE, mBoss.getLocation())
						.count(25)
						.delta(3)
						.spawnAsBoss();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
