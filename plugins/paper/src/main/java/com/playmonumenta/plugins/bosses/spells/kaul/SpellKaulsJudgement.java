package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/*
 * Kaul’s Judgement: (Stone Brick)Give a Tellraw to ¼ (min 2) of the
 * players as a warning then teleport them after 1-2 seconds. The players
 * selected are teleported to a mini-dungeon* of some kind. In order to
 * get out and back to the fight they must traverse the dungeon.
 * (players in the mini dungeon can’t get focused by Kaul’s attacks,
 * like his passive) with a timer, to be determined to the dungeon’s length
 * (Players that got banished get strength 1 and speed 1 for 30s if they survived)
 * (triggers once in phase 2 , and twice in phase 3)
 *
 *
 * This is a very weird skill implementation wise. Only one instance can ever exist
 */
public class SpellKaulsJudgement extends Spell implements Listener {
	private static final int KAULS_JUDGEMENT_RANGE = 50;
	private static final String KAULS_JUDGEMENT_TP_TAG = "KaulsJudgementTPTag";
	private static final String KAULS_JUDGEMENT_TAG = "KaulsJudgementTag";
	private static final String KAULS_JUDGEMENT_MOB_SPAWN_TAG = "KaulsJudgementMobSpawn";
	private static final String KAULS_JUDGEMENT_MOB_TAG = "deleteelite";
	private static final int KAULS_JUDGEMENT_TOTAL_TIME = 20 * 55;
	private static final int KAULS_JUDGEMENT_CHARGE_TIME = 20 * 2;
	private static final int KAULS_JUDGEMENT_MIN_TIME_BETWEEN = 20 * 90;
	private static final String NEGATIVE_HEALTH_EFFECT_NAME = "KaulsJudgementNegativeHealthBoost";
	private static final String SLOWNESS_EFFECT_NAME = "KaulsJudgementSlowness";
	private static final String STRENGTH_EFFECT_NAME = "KaulsJudgementStrength";
	private static final String SPEED_EFFECT_NAME = "KaulsJudgementSpeed";
	private static final String SPELL_NAME = "Kaul's Judgement";

	private final Plugin mPlugin = Plugin.getInstance();
	private final Location mBossLoc;
	private final LivingEntity mBoss;
	private @Nullable LivingEntity mTp = null;
	private boolean mOnCooldown = false;

	private final HashMap<Player, Location> mJudgedPlayersAndOrigins = new HashMap<>();

	private final ChargeUpManager mChargeUp;

	public SpellKaulsJudgement(LivingEntity boss) {
		mBoss = boss;
		mBossLoc = boss.getLocation();
		mChargeUp = new ChargeUpManager(boss, KAULS_JUDGEMENT_CHARGE_TIME, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_GREEN, TextDecoration.BOLD)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 75);
		/* Register this instance as an event handler so it can catch player events */
		mPlugin.getServer().getPluginManager().registerEvents(this, mPlugin);
	}

	@Override
	public void run() {
		mOnCooldown = true;
		World world = mBossLoc.getWorld();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, KAULS_JUDGEMENT_MIN_TIME_BETWEEN);

		/* Clear lingering Judgement mobs; shouldn't be necessary */
		for (Entity e : world.getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_TAG)) {
				e.remove();
			}
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Entity e : world.getEntities()) {
				if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_SPAWN_TAG)) {
					Location loc = e.getLocation().add(0, 1, 0);
					new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.45, 0.3, 1).spawnAsBoss();
					LibraryOfSoulsIntegration.summon(loc, "StonebornImmortal");
				}
			}
		}, 50);

		/* Clear Judgement mobs at end of cast */
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.getEntities().stream()
				.filter(e -> ScoreboardUtils.checkTag(e, KAULS_JUDGEMENT_MOB_TAG))
				.forEach(Entity::remove);
		}, KAULS_JUDGEMENT_TOTAL_TIME);

		List<Player> players = PlayerUtils.playersInRange(mBossLoc, KAULS_JUDGEMENT_RANGE, true);
		players.removeIf(p -> p.getLocation().getY() >= 61); //Get rid of spectators
		for (Player player : players) {
			player.sendMessage(Component.text("IT IS TIME FOR JUDGEMENT TO COME.", NamedTextColor.DARK_GREEN));
		}
		world.playSound(mBossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 2);
		new PartialParticle(Particle.SMOKE_LARGE, mBossLoc, 50, 0.5, 0.25, 0.5, 0).spawnAsBoss();

		int amount = Math.max(2, (players.size() + 1)/2);
		Collections.shuffle(players);
		while (players.size() > amount) {
			players.remove(0);
		}

		mJudgedPlayersAndOrigins.clear();
		for (Player player : players) {
			mJudgedPlayersAndOrigins.put(player, player.getLocation());
		}

		// This is in a separate function to ensure it doesn't use local variables from this function
		judge();
	}

	private void judge() {
		if (mTp == null) {
			return;
		}

		ChargeUpManager raceTimer = new ChargeUpManager(mTp.getLocation(), null, KAULS_JUDGEMENT_TOTAL_TIME - KAULS_JUDGEMENT_CHARGE_TIME, Component.text("Escape ", NamedTextColor.RED).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 75);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Shouldn't be possible, canRun fails if mTp is null
				if (mTp == null) {
					cancel();
					return;
				}
				mTicks++;

				if (mTicks < KAULS_JUDGEMENT_CHARGE_TIME) {
					if (mBoss.isDead()) {
						this.cancel();
						return;
					}
					mChargeUp.nextTick();
					/* pre-judgement particles */
					mJudgedPlayersAndOrigins.keySet().forEach((player) -> {
						new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 2, 0.4, 0.4, 0.4, 0).spawnAsBoss();
						new PartialParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0).spawnAsBoss();
					});
				} else if (mTicks == KAULS_JUDGEMENT_CHARGE_TIME) {
					mChargeUp.reset();
					raceTimer.nextTick(KAULS_JUDGEMENT_TOTAL_TIME - KAULS_JUDGEMENT_CHARGE_TIME);
					/* Start judgement */
					mJudgedPlayersAndOrigins.keySet().forEach((player) -> {
						player.addScoreboardTag(KAULS_JUDGEMENT_TAG);
						/* Spawn a copy of particles and sounds at departure location */
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
						new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
						new PartialParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();

						if (mTp != null) {
							Location tpLoc = mTp.getLocation();
							tpLoc.add(FastUtils.randomDoubleInRange(-6, 6), 0, FastUtils.randomDoubleInRange(-6, 6));
							tpLoc.setYaw(tpLoc.getYaw() + FastUtils.randomFloatInRange(-30, 30));
							tpLoc.setPitch(tpLoc.getPitch() + FastUtils.randomFloatInRange(-10, 10));
							player.teleport(tpLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);

							/* Spawn a copy of particles and sounds at arrival location */
							player.playSound(tpLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
							new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();

							player.sendMessage(Component.text("What happened!? You need to find your way out of here quickly!", NamedTextColor.AQUA));
							MessagingUtils.sendTitle(player, Component.text("ESCAPE", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), 1, 20 * 3, 1);
						}
					});
				} else if (mTicks < KAULS_JUDGEMENT_TOTAL_TIME) {
					if (mBoss.isDead()) {
						this.cancel();
						return;
					}
					raceTimer.previousTick();
					/* Judgement ticks - anyone who loses the tag early must have succeeded */
					new ArrayList<>(mJudgedPlayersAndOrigins.keySet()).forEach((player) -> {
						new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 1, 0.4, 0.4, 0.4, 0).spawnAsBoss();
						if (!player.getScoreboardTags().contains(KAULS_JUDGEMENT_TAG)) {
							succeed(player);
						}
					});
				} else {
					raceTimer.reset();
					/* Judgement ends - anyone left in judgement fails
					 * Make a copy to avoid concurrent modification exceptions
					 */
					new ArrayList<>(mJudgedPlayersAndOrigins.keySet()).forEach(SpellKaulsJudgement.this::fail);
					//This shouldn't do anything, fail already clears players
					mJudgedPlayersAndOrigins.clear();
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				raceTimer.reset();
				mChargeUp.reset();
				new ArrayList<>(mJudgedPlayersAndOrigins.keySet()).forEach(p -> endCommon(p));
				super.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void fail(Player player) {
		mPlugin.mEffectManager.addEffect(player, NEGATIVE_HEALTH_EFFECT_NAME, new PercentHealthBoost(60 * 20, -0.2, NEGATIVE_HEALTH_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(player, SLOWNESS_EFFECT_NAME, new PercentSpeed(60 * 20, -0.3, SLOWNESS_EFFECT_NAME));

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1, 0.2f);
		new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0, Material.ANVIL.createBlockData()).spawnAsBoss();
		player.sendMessage(Component.text("SUCH FAILURE.", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC));

		endCommon(player);
	}

	private void succeed(Player player) {
		mPlugin.mEffectManager.addEffect(player, STRENGTH_EFFECT_NAME, new PercentDamageDealt(60 * 20, 0.2));
		mPlugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(60 * 20, 0.2, SPEED_EFFECT_NAME));

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
		player.sendMessage(Component.text("You escaped! You feel much more invigorated from your survival!", NamedTextColor.AQUA));

		endCommon(player);
	}

	private void endCommon(Player player) {
		player.removeScoreboardTag(KAULS_JUDGEMENT_TAG);

		player.setHealth(EntityUtils.getMaxHealth(player));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 8 * 20, 8));
		if (player.getFireTicks() > 0) {
			player.setFireTicks(1);
		}
		Location loc = mJudgedPlayersAndOrigins.get(player);
		if (loc != null) {
			player.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
		}
		new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();
		mJudgedPlayersAndOrigins.remove(player);
	}

	@Override
	public boolean canRun() {
		for (Entity e : mBossLoc.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_TP_TAG) && e instanceof LivingEntity) {
				mTp = (LivingEntity) e;
				break;
			}
		}
		if (mTp == null) {
			MMLog.severe("Failed to find Kaul's Judgement teleport marker entity. Is it loaded?");
		}
		return mTp != null && !mOnCooldown;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 16;
	}

	@Override
	public int castTicks() {
		return 20 * 4;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (mJudgedPlayersAndOrigins.containsKey(player)) {
			event.setCancelled(true);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> fail(player), 1);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (mJudgedPlayersAndOrigins.containsKey(player)) {
			/* A player currently in judgement logged out */

			fail(player);
		}
	}
}
