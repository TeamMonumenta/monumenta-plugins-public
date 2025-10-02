package com.playmonumenta.plugins.bosses.spells.intruder;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellTwistedReplicants extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private List<Player> mPlayers;
	private final List<Integer> mSpecs = new ArrayList<>();
	private final int mYLevel;
	private final IntruderBoss.Dialogue mDialogue;
	private final IntruderBoss.Narration mNarration;

	public static final String SPAWNED_TAG = "TwistedReplicantSpawn";
	private static final ImmutableMap<Integer, String> REPLICANT_NAMES = ImmutableMap.<Integer, String>builder()
		.put(Mage.ARCANIST_SPEC_ID, "TwistedMoonweaver")
		.put(Mage.ELEMENTALIST_SPEC_ID, "TwistedPrimordialist")
		.put(Warrior.BERSERKER_SPEC_ID, "TwistedRevenant")
		.put(Warrior.GUARDIAN_SPEC_ID, "TwistedVanguard")
		.put(Cleric.PALADIN_SPEC_ID, "TwistedCrusader")
		.put(Cleric.SERAPH_SPEC_ID, "TwistedBishop")
		.put(Rogue.SWORDSAGE_SPEC_ID, "TwistedTempest")
		.put(Rogue.ASSASSIN_SPEC_ID, "TwistedShadestepper")
		.put(Alchemist.HARBINGER_SPEC_ID, "TwistedAbomination")
		.put(Alchemist.APOTHECARY_SPEC_ID, "TwistedArtificer")
		.put(Scout.RANGER_SPEC_ID, "TwistedWatchman")
		.put(Scout.HUNTER_SPEC_ID, "TwistedDeadeye")
		.put(Warlock.REAPER_SPEC_ID, "TwistedProsecutor")
		.put(Warlock.TENEBRIST_SPEC_ID, "TwistedGravecaller")
		.put(Shaman.SOOTHSAYER_ID, "TwistedSpiritcaller")
		.put(Shaman.HEXBREAKER_ID, "TwistedOmenspeaker")
		.build();

	private static final int SUMMONING_DURATION = 20;
	private static final double Y_OFFSET = 2.5;

	int mSpecCounter = 0;


	public SpellTwistedReplicants(Plugin plugin, LivingEntity boss, List<Player> players, int yLevel, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration) {
		mPlugin = plugin;
		mBoss = boss;
		mPlayers = new ArrayList<>(players);
		mYLevel = yLevel;
		mDialogue = dialogue;
		mNarration = narration;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location location = mBoss.getLocation();
		location.setY(mYLevel);
		mPlayers = IntruderBoss.playersInRange(location);
		mSpecs.clear();
		mPlayers.forEach(player -> {
			int spec = AbilityUtils.getSpecNum(player);
			if (spec == 0) {
				spec = AbilityUtils.getClassNum(player) * 2 - FastUtils.randomIntInRange(0, 1);
			}
			mSpecs.add(spec);
		});
		mBoss.setInvisible(true);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);

		mSpecCounter = mSpecs.size();
		mSpecs.forEach(spec -> {
				Mob replicant = (Mob) Objects.requireNonNull(LibraryOfSoulsIntegration.summon(LocationUtils.randomSafeLocationInCircle(location, 3, loc -> !loc.getBlock().isSolid()).subtract(0, Y_OFFSET, 0),
					REPLICANT_NAMES.getOrDefault(spec, FastUtils.getRandomElement(REPLICANT_NAMES.values().stream().toList()))));
				bossRise(replicant);

				replicant.addScoreboardTag(StatMultiplierBoss.identityTag);
				replicant.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagemult=1.4]");
				EntityUtils.scaleMaxHealth(replicant, 0.2, "TwistedReplicant");

				replicant.addScoreboardTag(SPAWNED_TAG);
				replicant.setLootTable(LootTables.EMPTY.getLootTable());
				Location halfLoc = replicant.getLocation().add(mBoss.getLocation()).multiply(0.5);
				halfLoc.add(new Vector(0, 15, 0));
				new PPBezier(Particle.SMOKE_LARGE, replicant.getLocation(), halfLoc, mBoss.getLocation())
					.count(40)
					.delay(20)
					.spawnAsBoss();
				new PPBezier(Particle.DUST_COLOR_TRANSITION, replicant.getLocation(), halfLoc, mBoss.getLocation())
					.count(60)
					.data(new Particle.DustTransition(Color.fromRGB(0x6b0000), Color.RED, 2.4f))
					.delay(20)
					.spawnAsBoss();
				new PPBezier(Particle.END_ROD, replicant.getLocation(), halfLoc, mBoss.getLocation())
					.count(40)
					.delay(20)
					.extra(99999)
					.spawnAsBoss();
			}
		);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PartialParticle(Particle.FLASH, mBoss.getLocation().add(new Vector(0, 1, 0))).minimumCount(1).spawnAsBoss();
			new PartialParticle(Particle.SQUID_INK, mBoss.getLocation())
				.count(30)
				.extra(0.7)
				.spawnAsBoss();
			mPlayers.forEach(player -> player.hideEntity(mPlugin, mBoss));
		}, 20);
		world.playSound(location, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 3.0f, 1.5f, 3);
		world.playSound(location, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 1.0f, 43);
		world.playSound(location, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3.0f, 0.1f, 48);
		world.playSound(location, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.45f, 0.4f);

		mDialogue.dialogue("THOSE. WHO SLUMBERED HERE. AWAKEN.");
		mNarration.narration("Those Amalgamations rising from the ground... They look just like you?");
	}

	private void bossRise(Mob boss) {
		boss.setAI(false);
		boss.setInvulnerable(true);

		final Location location = boss.getLocation();

		new BukkitRunnable() {
			int mTimer = 0;

			@Override
			public void run() {
				if (!boss.isValid()) {
					this.cancel();
					return;
				}

				boss.teleport(location.add(0, Y_OFFSET / SUMMONING_DURATION, 0));

				if (mTimer >= SUMMONING_DURATION) {
					this.cancel();
					return;
				}
				mTimer++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				boss.setAI(true);
				boss.setInvulnerable(false);
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void bossNearbyEntityDeath(EntityDeathEvent event) {
		if (event.getEntity().getScoreboardTags().contains(SPAWNED_TAG)) {
			mSpecCounter--;

			if (mSpecCounter <= 0) {
				mBoss.setAI(true);
				mBoss.setInvulnerable(false);
				mBoss.setInvisible(false);
				mPlayers.forEach(player -> player.showEntity(mPlugin, mBoss));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
