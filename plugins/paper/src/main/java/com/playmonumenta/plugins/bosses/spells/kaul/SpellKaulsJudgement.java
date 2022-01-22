package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
	private static final int KAULS_JUDGEMENT_TIME = 20 * 55;

	private static SpellKaulsJudgement INSTANCE = null;

	private final Plugin mPlugin = Plugin.getInstance();
	private final Location mBossLoc;
	private LivingEntity mTp = null;
	private boolean mOnCooldown = false;

	private final List<Player> mJudgedPlayers = new ArrayList<Player>();
	private final HashMap<Player, Location> mOrigPlayerLocs = new HashMap<Player, Location>();

	private final ChargeUpManager mChargeUp;

	private SpellKaulsJudgement(Location bossLoc) {
		mBossLoc = bossLoc;
		for (Entity e : bossLoc.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_TP_TAG) && e instanceof LivingEntity) {
				mTp = (LivingEntity) e;
				break;
			}
		}

		mChargeUp = new ChargeUpManager(mBossLoc, null, 20 * 2, ChatColor.GREEN + "Charging " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Kaul's Judgement...",
			BarColor.GREEN, BarStyle.SEGMENTED_10, 75);
		/* Register this instance as an event handler so it can catch player events */
		mPlugin.getServer().getPluginManager().registerEvents(this, mPlugin);
	}

	/*
	 * If a boss is specified, overwrites the current boss entity
	 * If no boss is specified (null), does not create an instance
	 */
	public static @Nullable SpellKaulsJudgement getInstance(Location bossLoc) {
		if (INSTANCE == null) {
			if (bossLoc == null) {
				return null;
			} else {
				INSTANCE = new SpellKaulsJudgement(bossLoc);
			}
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		World world = mBossLoc.getWorld();
		new BukkitRunnable() {
			@Override
			public void run() {
				mOnCooldown = false;
			}
		}.runTaskLater(mPlugin, 20 * 90);

		for (Entity e : world.getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_TAG)) {
				e.remove();
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Entity e : world.getEntities()) {
					if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_SPAWN_TAG)) {
						Location loc = e.getLocation().add(0, 1, 0);
						world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.45, 0.3, 1);
						LibraryOfSoulsIntegration.summon(loc, "StonebornImmortal");
					}
				}
			}
		}.runTaskLater(mPlugin, 50);

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Entity e : world.getEntities()) {
					if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_TAG)) {
						e.remove();
					}
				}
			}
		}.runTaskLater(mPlugin, KAULS_JUDGEMENT_TIME);

		List<Player> players = PlayerUtils.playersInRange(mBossLoc, KAULS_JUDGEMENT_RANGE, true);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		for (Player player : players) {
			player.sendMessage(ChatColor.DARK_GREEN + "IT IS TIME FOR JUDGEMENT TO COME.");
		}
		world.playSound(mBossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 10, 2);
		world.spawnParticle(Particle.SMOKE_LARGE, mBossLoc, 50, 0.5, 0.25, 0.5, 0);
		double amount = Math.ceil(players.size() / 2);
		if (amount < 2) {
			amount++;
		}

		Collections.shuffle(players);
		while (players.size() > amount) {
			players.remove(0);
		}

		mOrigPlayerLocs.clear();
		for (Player player : players) {
			mOrigPlayerLocs.put(player, player.getLocation());
		}
		mJudgedPlayers.addAll(players);

		// This is in a separate function to ensure it doesn't use local variables from this function
		judge();
	}

	private void judge() {
		new BukkitRunnable() {
			World mWorld = mBossLoc.getWorld();
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;

				if (mTicks < 20 * 2) {
					mChargeUp.nextTick();
					/* pre-judgement particles */
					for (Player player : mJudgedPlayers) {
						mWorld.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 2, 0.4, 0.4, 0.4, 0);
						mWorld.spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0);
					}
				} else if (mTicks == 20 * 2) {
					mChargeUp.reset();
					/* Start judgement */
					for (Player player : mJudgedPlayers) {
						player.addScoreboardTag(KAULS_JUDGEMENT_TAG);
						mWorld.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
						mWorld.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
						mWorld.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
						player.teleport(mTp);
						player.playSound(mTp.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
						player.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
						player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
						player.sendMessage(ChatColor.AQUA + "What happened!? You need to find your way out of here quickly!");
						player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ESCAPE", "", 1, 20 * 3, 1);
					}
				} else if (mTicks < KAULS_JUDGEMENT_TIME) {
					/* Judgement ticks - anyone who loses the tag early must have succeeded */
					Iterator<Player> iter = mJudgedPlayers.iterator();
					while (iter.hasNext()) {
						Player player = iter.next();

						mWorld.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 1, 0.4, 0.4, 0.4, 0);
						if (!player.getScoreboardTags().contains(KAULS_JUDGEMENT_TAG)) {
							iter.remove();
							succeed(player);
						}
					}
				} else {
					/* Judgement ends - anyone left in judgement fails
					 * Make a copy to avoid concurrent modification exceptions
					 */
					for (Player player : new ArrayList<Player>(mJudgedPlayers)) {
						fail(player);
					}
					mOrigPlayerLocs.clear();
					mJudgedPlayers.clear();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void fail(Player player) {
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.HEALTH_BOOST, 60 * 20, -2));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.SLOW, 60 * 20, 1));
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1, 0.2f);
		player.getWorld().spawnParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0, Material.ANVIL.createBlockData());
		player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "SUCH FAILURE.");

		endCommon(player);
	}

	private void succeed(Player player) {
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 30 * 20, 0));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.SPEED, 30 * 20, 0));
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
		player.sendMessage(ChatColor.AQUA + "You escaped! You feel much more invigorated from your survival!");

		endCommon(player);
	}

	private void endCommon(Player player) {
		player.removeScoreboardTag(KAULS_JUDGEMENT_TAG);

		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 8 * 20, 8));
		if (player.getFireTicks() > 0) {
			player.setFireTicks(1);
		}
		Location loc = mOrigPlayerLocs.get(player);
		if (loc != null) {
			player.teleport(loc);
			mOrigPlayerLocs.remove(player);
		}
		player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
		player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
		mJudgedPlayers.remove(player);
	}

	@Override
	public boolean canRun() {
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

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();

		if (damagee instanceof Player player) {
			if (mOrigPlayerLocs.containsKey(player)) {
				/* A player currently in judgement took damage */
				if (event.getFinalDamage() > player.getHealth()) {
					/* This would kill the player */

					fail(player);
					event.setDamage(0.1);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (mOrigPlayerLocs.containsKey(player)) {
			/* A player currently in judgement logged out */

			fail(player);
		}
	}
}
