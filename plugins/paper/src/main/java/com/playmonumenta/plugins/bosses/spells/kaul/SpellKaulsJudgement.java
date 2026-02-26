package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
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
import org.bukkit.scheduler.BukkitRunnable;

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
	private static final String KAULS_JUDGEMENT_TP_TAG = "KaulsJudgementTPTag";
	public static final String KAULS_JUDGEMENT_TAG = "KaulsJudgementTag";
	public static final String IMMUNITY_SOURCE = "KaulsJudgementImmunity";
	private static final String KAULS_JUDGEMENT_MOB_SPAWN_TAG = "KaulsJudgementMobSpawn";
	private static final int KAULS_JUDGEMENT_ESCAPE_TIME = 20 * 53;
	private static final int KAULS_JUDGEMENT_CHARGE_TIME = 20 * 2;
	private static final int KAULS_JUDGEMENT_MIN_TIME_BETWEEN = 20 * 90;
	private static final String NEGATIVE_HEALTH_EFFECT_NAME = "KaulsJudgementNegativeHealthBoost";
	private static final String SLOWNESS_EFFECT_NAME = "KaulsJudgementSlowness";
	private static final String STRENGTH_EFFECT_NAME = "KaulsJudgementStrength";
	private static final String SPEED_EFFECT_NAME = "KaulsJudgementSpeed";
	private static final String SPELL_NAME = "Kaul's Judgement";

	private final Plugin mPlugin = Plugin.getInstance();
	private final LivingEntity mBoss;
	private final Consumer<Player> mOnJudgementSuccess;
	private final Location mCenter;
	private final Location mTpLoc;

	private final HashMap<Player, Location> mJudgedPlayersAndOrigins = new HashMap<>();
	private final List<Entity> mJudgementMobs = new ArrayList<>();

	private final ChargeUpManager mChargeUp;
	private boolean mOnCooldown = false;

	public SpellKaulsJudgement(LivingEntity boss, Consumer<Player> onJudgementSuccess, Location center) {
		mBoss = boss;
		mOnJudgementSuccess = onJudgementSuccess;
		mCenter = center;

		mTpLoc = findTp();
		mChargeUp = new ChargeUpManager(boss, KAULS_JUDGEMENT_CHARGE_TIME, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_GREEN, TextDecoration.BOLD)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 75);
		/* Register this instance as an event handler so it can catch player events */
		mPlugin.getServer().getPluginManager().registerEvents(this, mPlugin);
	}

	private Location findTp() {
		World world = mBoss.getWorld();
		for (Entity e : world.getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_TP_TAG) && e instanceof LivingEntity) {
				return e.getLocation();
			}
		}
		MMLog.severe("Failed to find Kaul's Judgement teleport marker entity. Is it loaded?");
		return new Location(world, 250, 40, 637); // Im the best coder ever
	}


	@Override
	public void run() {
		mOnCooldown = true;
		mJudgementMobs.clear();

		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation().add(0, 1, 0);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, KAULS_JUDGEMENT_MIN_TIME_BETWEEN);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Entity e : world.getEntities()) {
				if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_SPAWN_TAG)) {
					Location loc = e.getLocation().add(0, 1, 0);
					new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.45, 0.3, 1).spawnAsBoss();
					Entity stonebornImmortal = LibraryOfSoulsIntegration.summon(loc, "StonebornImmortal");
					if (stonebornImmortal == null) {
						MMLog.severe("[Kaul] Soul \"StonebornImmortal\" does not exist!");
						break;
					}
					mJudgementMobs.add(stonebornImmortal);
				}
			}
		}, 50);

		List<Player> players = Kaul.getArenaParticipants(mCenter);
		for (Player player : players) {
			player.sendMessage(Component.text("IT IS TIME FOR JUDGEMENT TO COME.", NamedTextColor.DARK_GREEN));
		}
		world.playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 2);
		new PartialParticle(Particle.SMOKE_LARGE, bossLoc, 50, 0.5, 0.25, 0.5, 0).spawnAsBoss();

		int amount = (players.size() + 1) / 2;
		if (players.size() > 2 && amount == 1) {
			amount = 2;
		}
		Collections.shuffle(players);

		mJudgedPlayersAndOrigins.clear();
		for (int i = 0; i < amount; i++) {
			Player player = players.get(i);
			mJudgedPlayersAndOrigins.put(player, player.getLocation());
		}

		// This is in a separate function to ensure it doesn't use local variables from this function
		judge();
	}

	private void judge() {
		ChargeUpManager raceTimer = new ChargeUpManager(mTpLoc, null, KAULS_JUDGEMENT_ESCAPE_TIME, Component.text("Escape ", NamedTextColor.RED).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 75);
		raceTimer.setTime(KAULS_JUDGEMENT_ESCAPE_TIME);

		// don't cancel this runnable when another spell is force cast
		new BukkitRunnable() {
			boolean mCharging = true;

			@Override
			public void run() {
				if (mCharging) {
					for (Player player : mJudgedPlayersAndOrigins.keySet()) {
						Location pLoc = player.getLocation();
						new PartialParticle(Particle.SPELL_WITCH, pLoc.add(0, 1.5, 0), 2, 0.4, 0.4, 0.4, 0).spawnAsBoss();
						new PartialParticle(Particle.SPELL_MOB, pLoc.add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0).spawnAsBoss();
					}
					if (mChargeUp.nextTick()) {
						mChargeUp.reset();
						mCharging = false;

						/* Start judgement */
						for (Player player : mJudgedPlayersAndOrigins.keySet()) {
							player.addScoreboardTag(KAULS_JUDGEMENT_TAG);

							Location pLoc = player.getLocation();
							player.getWorld().playSound(pLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
							new PartialParticle(Particle.SPELL_WITCH, pLoc.add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, pLoc.add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();

							Location tpLoc = mTpLoc;
							tpLoc.add(FastUtils.randomDoubleInRange(-6, 6), 0, FastUtils.randomDoubleInRange(-6, 6));
							tpLoc.setYaw(tpLoc.getYaw() + FastUtils.randomFloatInRange(-30, 30));
							tpLoc.setPitch(tpLoc.getPitch() + FastUtils.randomFloatInRange(-10, 10));
							player.teleport(tpLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);

							player.playSound(tpLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
							new PartialParticle(Particle.SPELL_WITCH, pLoc.add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, pLoc.add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();

							player.sendMessage(Component.text("What happened!? You need to find your way out of here quickly!", NamedTextColor.AQUA));
							MessagingUtils.sendTitle(player, Component.text("ESCAPE", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), 1, 20 * 3, 1);
						}
					}
					return;
				}
				if (raceTimer.previousTick()) {
					for (Player player : mJudgedPlayersAndOrigins.keySet()) {
						fail(player);
					}
					mJudgedPlayersAndOrigins.clear();

					mJudgementMobs.forEach(Entity::remove);
					mJudgementMobs.clear();

					raceTimer.reset();
					this.cancel();
					return;
				}

				for (Player player : new ArrayList<>(mJudgedPlayersAndOrigins.keySet())) {
					new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 1, 0.4, 0.4, 0.4, 0).spawnAsBoss();
					if (!player.getScoreboardTags().contains(KAULS_JUDGEMENT_TAG)) {
						succeed(player);
						mJudgedPlayersAndOrigins.remove(player);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void fail(Player player) {
		endCommon(player);

		mPlugin.mEffectManager.addEffect(player, NEGATIVE_HEALTH_EFFECT_NAME, new PercentHealthBoost(60 * 20, -0.2, NEGATIVE_HEALTH_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(player, SLOWNESS_EFFECT_NAME, new PercentSpeed(60 * 20, -0.3, SLOWNESS_EFFECT_NAME));

		Location playerLocation = player.getLocation();
		player.getWorld().playSound(playerLocation, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0);
		player.getWorld().playSound(playerLocation, Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1, 0.2f);
		new PartialParticle(Particle.FALLING_DUST, playerLocation.add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0, Material.ANVIL.createBlockData()).spawnAsBoss();

		player.sendMessage(Component.text("SUCH FAILURE.", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC));
	}

	private void succeed(Player player) {
		endCommon(player);

		mPlugin.mEffectManager.addEffect(player, STRENGTH_EFFECT_NAME, new PercentDamageDealt(60 * 20, 0.2));
		mPlugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(60 * 20, 0.2, SPEED_EFFECT_NAME));

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1);
		player.sendMessage(Component.text("You escaped! You feel much more invigorated from your survival!", NamedTextColor.AQUA));

		mOnJudgementSuccess.accept(player);
	}

	private void endCommon(Player player) {
		player.removeScoreboardTag(KAULS_JUDGEMENT_TAG);

		player.setHealth(EntityUtils.getMaxHealth(player));
		mPlugin.mEffectManager.addEffect(player, IMMUNITY_SOURCE, new DamageImmunity(8 * 20, EnumSet.allOf(DamageEvent.DamageType.class)));
		if (player.getFireTicks() > 0) {
			player.setFireTicks(1);
		}
		Location loc = mJudgedPlayersAndOrigins.get(player);
		if (loc != null) {
			player.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
		}

		new PartialParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15).spawnAsBoss();
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
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
			mJudgedPlayersAndOrigins.remove(player);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (mJudgedPlayersAndOrigins.containsKey(player)) {
			/* A player currently in judgement logged out */

			fail(player);
			mJudgedPlayersAndOrigins.remove(player);
		}
	}
}
