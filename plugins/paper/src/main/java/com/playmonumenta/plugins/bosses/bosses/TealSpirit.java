package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.tealspirit.DoomsdayClock;
import com.playmonumenta.plugins.bosses.spells.tealspirit.MarchingFate;
import com.playmonumenta.plugins.bosses.spells.tealspirit.MidnightToll;
import com.playmonumenta.plugins.bosses.spells.tealspirit.PairedUnnaturalForce;
import com.playmonumenta.plugins.bosses.spells.tealspirit.Rewind;
import com.playmonumenta.plugins.bosses.spells.tealspirit.RewriteHistory;
import com.playmonumenta.plugins.bosses.spells.tealspirit.SandsOfTime;
import com.playmonumenta.plugins.bosses.spells.tealspirit.SundialSlash;
import com.playmonumenta.plugins.bosses.spells.tealspirit.SuspendedBallistae;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TealAntiCheat;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TealSpiritSummon;
import com.playmonumenta.plugins.bosses.spells.tealspirit.TemporalRift;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

public class TealSpirit extends BossAbilityGroup {
	public static final String identityTag = "boss_tealspirit";
	public static final int detectionRange = 70;

	private final int mHealth;

	public final Location mSpawnLoc;
	private final Location mEndLoc;

	private final List<Entity> mMarchers = new ArrayList<>();
	private String mEncounterType;
	private @Nullable DoomsdayClock mDoomsdayClock = null;

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

		Team team = ScoreboardUtils.getExistingTeamOrCreate("TealSpiritVulnerable", NamedTextColor.AQUA);
		team.addEntity(mBoss);

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		mEncounterType = "Normal";
		for (Player p : players) {
			if (p.getScoreboardTags().contains("SKTQuest")) {
				mEncounterType = "Story";
				break;
			} else if (p.getScoreboardTags().contains("SKTHard")) {
				mEncounterType = "Hard";
				break;
			} else {
				mEncounterType = "Normal";
				break;
			}
		}


		SpellManager activeSpells;
		List<Spell> passiveSpells;
		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		if (mEncounterType.equals("Story")) {
			mHealth = 6000;

			activeSpells = new SpellManager(Arrays.asList(
				new SuspendedBallistae(mBoss, mPlugin, 25, 5, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30, 90),
				new SundialSlash(mBoss, 7 * 20)
			));

			passiveSpells = Arrays.asList(
				new TealSpiritSummon(mSpawnLoc, 40 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 25),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			events.put(25, mBoss -> {
				changePhase(activeSpells, passiveSpells, null);
			});

			events.put(50, getSummonEchoAction());

			events.put(75, mBoss -> {
				changePhase(activeSpells, passiveSpells, null);
			});
		} else if (mEncounterType.equals("Hard")) {
			mHealth = 24500;

			RewriteHistory rewriteHistory = new RewriteHistory(mPlugin, mBoss, 20 * 5, 30, mSpawnLoc);
			MidnightToll midnightToll = new MidnightToll(mPlugin, mBoss, 20 * 5, 80, 40, mSpawnLoc);
			MidnightToll finalMidnightToll = new MidnightToll(mPlugin, mBoss, 20 * 15, 99999999, 40, mSpawnLoc);
			mDoomsdayClock = new DoomsdayClock(mBoss, mSpawnLoc, 20 * 25);
			MarchingFate mMarchingFates = new MarchingFate(mBoss, this, true);

			activeSpells = new SpellManager(Arrays.asList(
				new SandsOfTime(mBoss, mSpawnLoc, team, 24 * 20, 120, 20),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30, 150),
				new SundialSlash(mBoss, 7 * 20),
				new SuspendedBallistae(mBoss, mPlugin, 25, 5, 80,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			passiveSpells = Arrays.asList(
				mMarchingFates,
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> mBoss.getLocation().distance(mSpawnLoc) > 25),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			SpellManager finalPhaseActiveSpells = new SpellManager(Arrays.asList(
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30, 150),
				new SundialSlash(mBoss, 7 * 20),
				new SuspendedBallistae(mBoss, mPlugin, 25, 4, 80,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			SpellManager activeRewindPhase = new SpellManager(Arrays.asList(
				new Rewind(mBoss, mSpawnLoc, this, activeSpells, passiveSpells)
			));

			BossBarManager.BossHealthAction rewindAction = getRewindAction(activeRewindPhase, passiveSpells);

			events.put(90, rewindAction);

			events.put(80, rewindAction);

			events.put(75, mBoss -> {
				if (mDoomsdayClock != null) {
					mDoomsdayClock.run();
				}
			});

			events.put(70, rewindAction);

			events.put(60, rewindAction);

			events.put(55, mBoss -> {
				if (mDoomsdayClock != null) {
					mDoomsdayClock.disableClock();
				}
			});

			events.put(50, getSummonEchoAction());

			events.put(40, rewindAction);

			events.put(45, rewindAction);

			events.put(30, mBoss -> {
				rewindAction.run(mBoss);
				if (mDoomsdayClock != null) {
					mDoomsdayClock.run();
				}
			});

			events.put(25, mBoss -> {
				rewriteHistory.run();
			});

			events.put(20, mBoss -> {
				mMarchingFates.removeMarchers();
				midnightToll.run();
				changePhase(finalPhaseActiveSpells, passiveSpells, null);
			});

			events.put(15, mBoss -> {
				rewriteHistory.run();
			});

			events.put(10, mBoss -> {
				finalMidnightToll.run();
			});

			events.put(1, mBoss -> {
				if (mDoomsdayClock != null) {
					mDoomsdayClock.disableClock();
				}
			});
		} else {
			mHealth = 19000;

			mDoomsdayClock = new DoomsdayClock(mBoss, mSpawnLoc, 20 * 25);
			MarchingFate mMarchingFates = new MarchingFate(mBoss, this, false);

			activeSpells = new SpellManager(Arrays.asList(
				new SandsOfTime(mBoss, mSpawnLoc, team, 25 * 20, 80, 40),
				new TemporalRift(mBoss, mSpawnLoc, 15 * 20),
				new PairedUnnaturalForce(mPlugin, mBoss, mSpawnLoc, 0, 15, 30, 80),
				new SundialSlash(mBoss, 7 * 20),
				new SuspendedBallistae(mBoss, mPlugin, 25, 3, 50,
					20 * 6, 20 * 2, 20 * 1, mSpawnLoc, 5)
			));

			passiveSpells = Arrays.asList(
				mMarchingFates,
				new TealSpiritSummon(mSpawnLoc, 30 * 20),
				new SpellBlockBreak(mBoss),
				new SpellShieldStun(10 * 20),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, mBoss -> {
					return mBoss.getLocation().distance(mSpawnLoc) > 25;
				}),
				new TealAntiCheat(boss, 20, spawnLoc)
			);

			SpellManager activeRewindPhase = new SpellManager(Arrays.asList(
				new Rewind(mBoss, mSpawnLoc, this, activeSpells, passiveSpells)
			));

			BossBarManager.BossHealthAction rewindAction = getRewindAction(activeRewindPhase, passiveSpells);

			events.put(90, rewindAction);

			events.put(80, rewindAction);

			events.put(70, rewindAction);

			events.put(60, rewindAction);

			events.put(50, getSummonEchoAction());

			events.put(40, rewindAction);

			events.put(45, rewindAction);

			events.put(30, mBoss -> {
				rewindAction.run(mBoss);
				if (mDoomsdayClock != null) {
					mDoomsdayClock.run();
				}
			});

			events.put(20, rewindAction);

			events.put(10, rewindAction);

			events.put(1, mBoss -> {
				if (mDoomsdayClock != null) {
					mDoomsdayClock.disableClock();
				}
			});
		}

		players.forEach(player -> player.sendMessage(Component.text("the full wrath of time itself will be upon you!", NamedTextColor.DARK_AQUA)));

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 20 * 10);
	}

	private BossBarManager.BossHealthAction getRewindAction(SpellManager activeRewindPhase, List<Spell> passiveSpells) {
		return mBoss -> {
			changePhase(activeRewindPhase, passiveSpells, null);
			forceCastSpell(Rewind.class);
		};
	}

	private BossBarManager.BossHealthAction getSummonEchoAction() {
		return mBoss -> {
			Location loc = mSpawnLoc.clone().add(0, 2, 0);
			LibraryOfSoulsIntegration.summon(loc, "EchoesOfOblivion");
			World world = mBoss.getWorld();
			world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.4f, 1.2f);
			world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 0.8f, 2.0f);
			new PartialParticle(Particle.END_ROD, loc, 20, 0.3, 0, 0.3, 0.1).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.SPELL_WITCH, loc, 20, 0.3, 0, 0.3, 0.1).spawnAsEntityActive(mBoss);
			PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player -> player.sendMessage(Component.text("i call forth the echoes radiating from the edges of oblivion itself. rise, emperor!", NamedTextColor.DARK_AQUA)));
		};
	}

	@Override
	public void init() {

		EntityUtils.setMaxHealthAndHealth(mBoss, mHealth);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		if (mEncounterType.equals("Normal")) {
			EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, 30);
		}

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.AQUA + "Orasomn", ChatColor.RED + "The Hand of Fate");
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}

		mBoss.setAI(true);
		mBoss.setGlowing(true);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
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

	public void setMarchers(List<Entity> marchers) {
		mMarchers.clear();
		mMarchers.addAll(marchers);
	}

	public void killMarchers() {
		for (Entity marcher : mMarchers) {
			marcher.remove();
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (hasRunningSpellOfType(Rewind.class)) {
			event.setDamage(event.getDamage() * 0.1);
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		event.getPlayer().sendMessage(Component.text("the endless march of time will decay even your bones. you will be forgotten.", NamedTextColor.DARK_AQUA));
	}
}
