package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.tealspirit.DelayedAssassination;
import com.playmonumenta.plugins.bosses.spells.tealspirit.DoomsdayClock;
import com.playmonumenta.plugins.bosses.spells.tealspirit.MarchingFate;
import com.playmonumenta.plugins.bosses.spells.tealspirit.Rewind;
import com.playmonumenta.plugins.bosses.spells.tealspirit.SandsOfTime;
import com.playmonumenta.plugins.bosses.spells.tealspirit.SundialSlash;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TealSpiritSummon;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TemporalInstability;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TemporalRift;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TrabemTemporis;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

public class TealSpirit extends BossAbilityGroup {
	public static final String identityTag = "boss_tealspirit";
	public static final int detectionRange = 70;

	private static final int HEALTH = 10000;

	private static final double RADIUS = 26;
	private static final double HEIGHT_UP = 6;
	private static final double HEIGHT_DOWN = 3;

	public final Location mSpawnLoc;
	private final Location mEndLoc;

	private int mInterspellCooldown = 0;
	private final BukkitRunnable mRunnable;

	private List<Entity> mMarchers;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new TealSpirit(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public TealSpirit(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		World world = mSpawnLoc.getWorld();

		Team team = ScoreboardUtils.getExistingTeamOrCreate("TealSpiritVulnerable", NamedTextColor.AQUA);
		team.addEntity(mBoss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new DelayedAssassination(plugin, mBoss, 8 * 20),
			new SandsOfTime(mBoss, mSpawnLoc, team, 12 * 20, this),
			new DoomsdayClock(mBoss, mSpawnLoc, 11 * 20, this),
			new TemporalRift(mBoss, mSpawnLoc, 15 * 20, this),
			new TrabemTemporis(mBoss, mSpawnLoc, 20 * 20, this),
			new SundialSlash(mBoss, 7 * 20)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new MarchingFate(mBoss, this),
			new Rewind(mBoss, mSpawnLoc, 60 * 20, this),
			new TemporalInstability(mBoss, mSpawnLoc, team),
			new TealSpiritSummon(mSpawnLoc, 30 * 20),
			new SpellBlockBreak(mBoss),
			new SpellShieldStun(10 * 20)
		);

		PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "the full wrath of time itself will be upon you!"));

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

		events.put(75, mBoss -> {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!isInterspellCooldown()) {
						forceCastSpell(TrabemTemporis.class);
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 5);
		});

		events.put(50, mBoss -> {
			List<Location> locs = new ArrayList<>();
			double radius = 21;
			double height = 2;
			locs.add(mSpawnLoc.clone().add(0, height, radius));
			locs.add(mSpawnLoc.clone().add(0, height, -radius));
			for (Location loc : locs) {
				LibraryOfSoulsIntegration.summon(loc, "EchoesOfOblivion");
				world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 1.2f);
				world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
				world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
				world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
			}

			PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "i call forth the echoes radiating from the edges of oblivion itself. rise, emperor!"));
		});

		events.put(25, mBoss -> {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!isInterspellCooldown()) {
						forceCastSpell(DoomsdayClock.class);
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 5);
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);

		mRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mInterspellCooldown > 0) {
					mInterspellCooldown -= 5;
				}

				mT += 5;
				if (mT > 100 && mT % 10 == 0) {
					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
						Location loc = player.getLocation();
						double height = loc.getY() - mSpawnLoc.getY();
						if (LocationUtils.xzDistance(mSpawnLoc, loc) > RADIUS || (height > HEIGHT_UP && player.isOnGround()) || height < -HEIGHT_DOWN) {
							BossUtils.bossDamagePercent(mBoss, player, 0.1, (Location) null);
							if (mT % 40 == 0) {
								player.sendMessage(ChatColor.RED + "You are too far from the fight!");
							}
						}
					}
				}
			}
		};
		mRunnable.runTaskTimer(plugin, 0, 5);
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, HEALTH);
		mBoss.setHealth(HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		Collections.shuffle(players);
		if (!players.isEmpty() && mBoss instanceof Mob mob) {
			mob.setTarget(players.get(0));
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		killMarchers();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mSpawnLoc, detectionRange)) {
			mob.remove();
		}

		mRunnable.cancel();

		PlayerUtils.playersInRange(mSpawnLoc, TealSpirit.detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "no, this cannot be! time... betrays me? why do the hands of time not turn? i... will... be... forever!"));

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	public boolean isInterspellCooldown() {
		return mInterspellCooldown > 0;
	}

	public void setInterspellCooldown(int ticks) {
		mInterspellCooldown = Math.max(mInterspellCooldown, ticks);
	}

	public void setMarchers(List<Entity> marchers) {
		mMarchers = marchers;
	}

	public void killMarchers() {
		for (Entity marcher : mMarchers) {
			marcher.remove();
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		event.getPlayer().sendMessage(ChatColor.DARK_AQUA + "the endless march of time will decay even your bones. you will be forgotten.");
	}
}
