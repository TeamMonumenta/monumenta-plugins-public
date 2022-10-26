package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.tealspirit.*;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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

	private int mHealth = 20000;

	public final Location mSpawnLoc;
	private final Location mEndLoc;

	private int mInterspellCooldown = 0;

	private List<Entity> mMarchers;
	private List<Entity> mExchangers = new ArrayList<>();
	private List<Entity> mShielders = new ArrayList<>();
	private String mEncounterType;
	private DoomsdayClock mDoomsdayClock = null;
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


		for (Player p : PlayerUtils.playersInRange(mSpawnLoc, 75, true)) {
			if (p.getGameMode() != GameMode.SPECTATOR) {
				if (p.getScoreboardTags().contains("SKTQuest")) {
					mEncounterType = "Story";
					break;
				}
				else if (p.getScoreboardTags().contains("SKTHard")) {
					mEncounterType = "Hard";
					break;
				}
				else {
					mEncounterType = "Normal";
					break;
				}
			}
		}

		if (mEncounterType.equals("Story")) {
			mHealth = 6000;

			SpellManager activeSpells = new SpellManager(Arrays.asList(
				new ClockworkAssassination(plugin, boss),
				//new SandsOfTime(mBoss, mSpawnLoc, team, 12 * 20),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30),
				new SundialSlash(mBoss, 7 * 20)
			));

			SpellManager activeDoomsdayPhase = new SpellManager(Arrays.asList(
				//new DoomsdayClock(mBoss, mSpawnLoc, 11 * 20)
			));

			List<Spell> passiveSpells = Arrays.asList(
				//new Rewind(mBoss, mSpawnLoc),
				new TealSpiritSummon(mSpawnLoc, 40 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 40),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "the full wrath of time itself will be upon you!"));

			Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

			events.put(25, mBoss -> {
				changePhase(activeDoomsdayPhase, passiveSpells, null);
				forceCastSpell(MarchingFate.class);
				changePhase(activeSpells, passiveSpells, null);
			});

			events.put(50, mBoss -> {
				List<Location> locs = new ArrayList<>();
				double height = 2;
				locs.add(mSpawnLoc.clone().add(0, height, 0));
				for (Location loc : locs) {
					LibraryOfSoulsIntegration.summon(loc, "EchoOfOblivion");
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 1.2f);
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
					world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
					world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
				}
				PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "i call forth the echoes radiating from the edges of oblivion itself. rise, emperor!"));
			});

			events.put(75, mBoss -> {
				changePhase(activeDoomsdayPhase, passiveSpells, null);
				forceCastSpell(MarchingFate.class);
				changePhase(activeSpells, passiveSpells, null);
			});

			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);
		} else if (mEncounterType.equals("Hard")) {
			mHealth = 22500;

			RewriteHistory rewriteHistory = new RewriteHistory(mPlugin, mBoss, 20 * 5, 30, mSpawnLoc);
			MidnightToll midnightToll = new MidnightToll(mPlugin, mBoss, 20 * 5, 40, 40, mSpawnLoc);
			MidnightToll finalMidnightToll = new MidnightToll(mPlugin, mBoss, 20 * 15, 99999999, 40, mSpawnLoc);
			mDoomsdayClock = new DoomsdayClock(mBoss, mSpawnLoc, 20 * 25);
			MarchingFate mMarchingFates = new MarchingFate(mBoss, this);

			SpellManager activeSpells = new SpellManager(Arrays.asList(
				//new ClockworkAssassination(plugin, boss),
				new SandsOfTime(mBoss, mSpawnLoc, team, 12 * 20, 180),
				//new Rewind(mBoss, mSpawnLoc),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30),
				new SundialSlash(mBoss, 7 * 20),
				new SuspendedBallistae(mBoss, mPlugin, 25, 5, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			SpellManager activeRewindPhase = new SpellManager(Arrays.asList(
				new Rewind(mBoss, mSpawnLoc)
			));

			SpellManager finalStandActives = new SpellManager(Arrays.asList(
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new SundialSlash(mBoss, 7 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30),
				new SuspendedBallistae(mBoss, mPlugin, 25, 5, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			List<Spell> passiveSpells = Arrays.asList(
				mMarchingFates,
				//new Rewind(mBoss, mSpawnLoc),
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 40),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			List<Spell> finalStandPassive = Arrays.asList(
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 40),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "the full wrath of time itself will be upon you!"));

			Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

			events.put(90, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(80, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(75, mBoss -> {
				mDoomsdayClock.run();
			});

			events.put(70, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(60, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(55, mBoss -> {
				mDoomsdayClock.disableClock();
			});

			events.put(50, mBoss -> {
				List<Location> locs = new ArrayList<>();
				double height = 2;
				locs.add(mSpawnLoc.clone().add(0, height, 0));
				for (Location loc : locs) {
					LibraryOfSoulsIntegration.summon(loc, "EchoesOfOblivion");
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 1.2f);
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
					world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
					world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
				}
				PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "i call forth the echoes radiating from the edges of oblivion itself. rise, emperor!"));
			});

			events.put(40, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(45, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(30, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(finalStandActives, finalStandPassive, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
				mDoomsdayClock.run();
			});

			events.put(25, mBoss -> {
				rewriteHistory.run();
			});

			events.put(20, mBoss -> {
				mMarchingFates.removeMarchers();
				midnightToll.run();
			});

			events.put(15, mBoss -> {
				rewriteHistory.run();
			});

			events.put(10, mBoss -> {
				finalMidnightToll.run();
			});

			events.put(1, mBoss -> {
				mDoomsdayClock.disableClock();
			});

			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);
		} else if (mEncounterType.equals("Normal")) {
			mHealth = 18000;

			mDoomsdayClock = new DoomsdayClock(mBoss, mSpawnLoc, 20 * 25);
			MarchingFate mMarchingFates = new MarchingFate(mBoss, this);

			SpellManager activeSpells = new SpellManager(Arrays.asList(
				//new ClockworkAssassination(plugin, boss),
				new SandsOfTime(mBoss, mSpawnLoc, team, 12 * 20, 80),
				//new Rewind(mBoss, mSpawnLoc),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30),
				new SundialSlash(mBoss, 7 * 20),
				new SuspendedBallistae(mBoss, mPlugin, 25, 4, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			SpellManager activeRewindPhase = new SpellManager(Arrays.asList(
				new Rewind(mBoss, mSpawnLoc)
			));

			SpellManager finalStandActives = new SpellManager(Arrays.asList(
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new SundialSlash(mBoss, 7 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30),
				new SuspendedBallistae(mBoss, mPlugin, 25, 5, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			List<Spell> passiveSpells = Arrays.asList(
				mMarchingFates,
				//new Rewind(mBoss, mSpawnLoc),
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 40),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			List<Spell> finalStandPassive = Arrays.asList(
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 40),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "the full wrath of time itself will be upon you!"));

			Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

			events.put(90, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(80, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(75, mBoss -> {
				mDoomsdayClock.run();
			});

			events.put(70, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(60, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(55, mBoss -> {
				mDoomsdayClock.disableClock();
			});

			events.put(50, mBoss -> {
				List<Location> locs = new ArrayList<>();
				double height = 2;
				locs.add(mSpawnLoc.clone().add(0, height, 0));
				for (Location loc : locs) {
					LibraryOfSoulsIntegration.summon(loc, "EchoesOfOblivion");
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 1.2f);
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
					world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
					world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
				}
				PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "i call forth the echoes radiating from the edges of oblivion itself. rise, emperor!"));
			});

			events.put(40, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(45, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(activeSpells, passiveSpells, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
			});

			events.put(30, mBoss -> {
				// Cast Rewind without interruptions
				changePhase(activeRewindPhase, passiveSpells, null);
				forceCastSpell(Rewind.class);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					changePhase(finalStandActives, finalStandPassive, null);
				}, Rewind.REWIND_TIME + Rewind.CHARGE_TIME + Rewind.COOLDOWN_TIME);
				mDoomsdayClock.run();
			});

			events.put(1, mBoss -> {
				mDoomsdayClock.disableClock();
			});

			BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
			constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);
		}
	}

	@Override
	public void init() {

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, mHealth);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(mHealth);

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Orasomn\",\"color\":\"gold\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The Hand of Fate\",\"color\":\"red\",\"yellow\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		Collections.shuffle(players);
		if (!players.isEmpty() && mBoss instanceof Mob mob) {
			mob.setTarget(players.get(0));
		}
		mBoss.setAI(true);
	}

	@Override
	public void death(EntityDeathEvent event) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mSpawnLoc, detectionRange)) {
			mob.remove();
		}
		PlayerUtils.playersInRange(mSpawnLoc, TealSpirit.detectionRange, true).forEach(player -> {
			player.sendMessage(ChatColor.DARK_AQUA + "no, this cannot be! time... betrays me? why do the hands of time not turn? i... will... be... forever!");
			EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, "TealSpirit-" + mBoss.getUniqueId());
		});
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		if (mDoomsdayClock != null) {
			mDoomsdayClock.disableClock();
		}
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

	public void giveParadox() {
		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, 50, true);
		EffectManager manager = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager;
		Collections.shuffle(players);
		if (players.size() > 2) {
			for (int i = 0; i < 1; i++) {
				manager.addEffect(players.get(i), TemporalFlux.GENERIC_NAME, new TemporalFlux(20*30));
			}
		} else if (players.size() <= 2) {
			for (Player p : players) {
				manager.addEffect(p, TemporalFlux.GENERIC_NAME, new TemporalFlux(20*30));
			}
		}
	}

	public void clearParadox() {
		EffectManager manager = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager;
		for (Player p : PlayerUtils.playersInRange(mSpawnLoc, 50, true)) {
			manager.clearEffects(p, "Paradox");
		}
	}

	public void spawnExchangers() {
		World world = mBoss.getWorld();
		List<Location> locs = new ArrayList<>();
		double radius = 21;
		double height = 2;
		locs.add(mSpawnLoc.clone().add(0, height, 0 - radius));
		locs.add(mSpawnLoc.clone().add(0, height, radius));
		for (Location loc : locs) {
			mExchangers.add(LibraryOfSoulsIntegration.summon(loc, "TemporalExchanger"));
			world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
			world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
			world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
		}
	}

	public void killExchangers() {
		for (Entity exchanger : mExchangers) {
			exchanger.remove();
		}
	}

	public void spawnShields() {
		World world = mBoss.getWorld();
		List<Location> locs = new ArrayList<>();
		double radius = 21;
		double height = 2;
		locs.add(mSpawnLoc.clone().add(radius, height, 0));
		locs.add(mSpawnLoc.clone().add(-radius, height, 0));
		for (Location loc : locs) {
			mShielders.add(LibraryOfSoulsIntegration.summon(loc, "TemporalAnchor"));
			world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 0.8f, 2.0f);
			world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1);
			world.spawnParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1);
		}
	}

	public void killShields() {
		for (Entity shield : mShielders) {
			shield.remove();
		}
	}

	public void castRewind(SpellManager activeRewindPhase, SpellManager activeSpells, List<Spell> passiveSpells) {
		changePhase(activeRewindPhase, passiveSpells, null);
		forceCastSpell(Rewind.class);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				if (mT > Rewind.CHARGE_TIME + Rewind.REWIND_TIME) {
					changePhase(activeSpells, passiveSpells, null);
					this.cancel();
				}
				mT += 1;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange, mBoss)) {
				if (mob.getScoreboardTags().contains("boss_temporalshield")) {
					event.setCancelled(true);
					damager.sendMessage(ChatColor.GRAY + "The nearby Temporal Anchors prevent you from harming Orasomn!");
					return;
				}
			}
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
