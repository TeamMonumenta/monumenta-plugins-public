package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellArachnopocolypse;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthsWrath;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellGroundSurge;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellKaulsJudgement;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStorm;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStrike;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPutridPlague;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellVolcanicDemise;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


/* Woah it's Kaul! */

/*
Phase 1 :
Attacks :
Raise Jungle
Arachnopocalypse
Putrid plague
Earth's Wrath


Phase 2 :
Earth's wrath
Putrid Plague
Raise Jungle
Kaul's Judgement

Phase 2.5 (50% health) :
Summons a powerful Primordial Elemental that is invulnerable and immovable until out of the ground. Players will have 15 seconds to prepare for the elemental's arrival. Kaul will not be attacking or casting any abilities (except for his passives) during this time. (512 health)

Elemental's Abilities:
Normal Block break passive
Raise Jungle (Kaul's ability), however the timer for raising them will be 30 seconds instead of 40.
Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that spans out 5 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.
Stone Blast: After 1 second, fires at all players a powerful block breaking bolt. Intersecting with a player causes 15 damage and applies Weakness II and Slowness II. Intersecting with a block causes a TNT explosion to happen instead. The bolt will stop travelling if it hits a player or a block.
Once the elemental is dead, Kaul returns to the fight. The elemental will meld into the ground for later return in Phase 3.5

Phase 3:
Earthâ€™s Wrath
Putrid plague
Volcanic demise
Kaul's Judgement

Phase 3.5 (20% health [Let's make this even harder shall we?]) :
The Primordial Elemental from Phase 2.5 returns, however he is completely invulnerable to all attacks, and gains Strength I for the rest of the fight. The elemental will remain active until the end of the fight.
The elemental will lose his "Raise Jungle" ability, but will still possess the others.

 *
 */
/*
 * Base Spells:
 * /
 * Volcanic Demise (Magma cream): Kaul shoots at each player at the
 * same time, casting particle fireballs at each player with a large
 * hit radius. On contact with a player, deals 20 damage and ignites
 * the player for 10 seconds. If the player shields the fireball, the
 * shield takes 50% durability damage and is put on cooldown for 30
 * seconds (The player is still set on fire). On contact with a block,
 * explodes and leaves a fiery aura that deals 10 damage and ignites
 * for 5 seconds (+3 seconds for fire duration if a player is already on
 * fire) to players who stand in it. The aura lasts 5 seconds.
 * (Aka Disco Inferno)
 */

public class Kaul extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_kaul";
	public static final int detectionRange = 50;
	public static final int ARENA_RADIUS = 55;
	// Barrier layer is from Y 62.0 to 64.0
	public static final int ARENA_MAX_Y = 62;

	public static final int PILLAR_OFFSET = 18;
	public static final Vector RED_OFFSET = new Vector(PILLAR_OFFSET, 0, -PILLAR_OFFSET);
	public static final Vector BLUE_OFFSET = new Vector(PILLAR_OFFSET, 0, PILLAR_OFFSET);
	public static final Vector YELLOW_OFFSET = new Vector(-PILLAR_OFFSET, 0, PILLAR_OFFSET);
	public static final Vector GREEN_OFFSET = new Vector(-PILLAR_OFFSET, 0, -PILLAR_OFFSET);

	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_1 = 18;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_2 = 12;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_3 = 10;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_4 = 6;
	private static final int MAX_HEALTH = 2048;
	private static final int DIALOGUE_DELAY = 20 * 4;
	private static final double SCALING_X = 0.7;
	private static final double SCALING_Y = 0.65;
	private static final String primordial = "PrimordialElemental";
	private static final String immortal = "ImmortalElemental";
	private static final int FLOOR_Y = 8;
	private static final double PILLAR_DISTANCE = Math.sqrt(18 * 18 * 2);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private final Location mShrineLoc;
	private boolean mDefeated = false;
	private boolean mCooldown = false;
	private boolean mPrimordialPhase = false;
	private int mPlayerCount;
	private double mDefenseScaling;
	@Nullable
	private LivingEntity mImmortal;

	public Kaul(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mShrineLoc = boss.getLocation();
		mShrineLoc.setY(FLOOR_Y);

		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
		mBoss.setRemoveWhenFarAway(false);
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : getArenaParticipants()) {
					if (player.isSleeping()) {
						DamageUtils.damage(mBoss, player, DamageType.OTHER, 22);
						EffectType.applyEffect(EffectType.SLOW, player, 15 * 20, 0.3, "KaulAntiSleepSlowness", false);
						player.sendMessage(Component.text("THE JUNGLE FORBIDS YOU TO DREAM.", NamedTextColor.DARK_GREEN));
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 0.85f);
					}
				}
				if (mDefeated || !mBoss.isValid()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		/* These spells need to be shared between phases in this manner to prevent double casting on phase change
		 * I would do this for all of the spells here but I'll just thank Java for having no support for true pass by
		 * copy for objects instead
		 */
		SpellKaulsJudgement kaulsJudgement = new SpellKaulsJudgement(mBoss, this::judgementSuccess, mShrineLoc);
		SpellVolcanicDemise volcanicDemise = new SpellVolcanicDemise(plugin, mBoss, 40D, mShrineLoc);

		SpellManager phase1Spells = new SpellManager(
			Arrays.asList(new SpellRaiseJungle(mPlugin, mBoss, 10, 20 * 9, 20 * 10, mShrineLoc),
				new SpellPutridPlague(mPlugin, mBoss, false, mShrineLoc),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineLoc.getY()),
				new SpellArachnopocolypse(mPlugin, mBoss, mShrineLoc)));

		SpellManager phase2Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, false, mShrineLoc),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineLoc.getY()),
				new SpellRaiseJungle(mPlugin, mBoss, 10, 20 * FLOOR_Y, 20 * 10, mShrineLoc),
				new SpellArachnopocolypse(mPlugin, mBoss, mShrineLoc),
				kaulsJudgement));

		SpellManager phase3Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, true, mShrineLoc),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineLoc.getY()),
				new SpellGroundSurge(mPlugin, mBoss, mShrineLoc),
				volcanicDemise, kaulsJudgement));

		SpellManager phase4Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, true, mShrineLoc),
				new SpellEarthsWrath(mPlugin, mBoss, mShrineLoc.getY()),
				new SpellGroundSurge(mPlugin, mBoss, mShrineLoc),
				volcanicDemise));

		List<UUID> sentWaterMessage = new ArrayList<>();
		List<UUID> cd = new ArrayList<>();
		SpellPlayerAction action = new SpellPlayerAction(this::getArenaParticipants, (Player player) -> {
			Vector loc = player.getLocation().toVector();
			boolean inWater = player.getLocation().getBlock().isLiquid();
			boolean tooFarAway = !loc.isInSphere(mShrineLoc.toVector(), 42);
			if (inWater || tooFarAway) {
				if (cd.contains(player.getUniqueId())) {
					return;
				}
				/* Damage has no direction so can't be blocked */
				if (BossUtils.bossDamagePercent(mBoss, player, 0.4)) {
					/* Player survived the damage */
					MovementUtils.knockAway(spawnLoc, player, -2.5f, 0.85f);

					world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 1.3f);
					new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 80, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();

					cd.add(player.getUniqueId());
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> cd.remove(player.getUniqueId()), 10);
				}
				if (inWater) {
					if (!sentWaterMessage.contains(player.getUniqueId())) {
						sentWaterMessage.add(player.getUniqueId());
						player.sendMessage(Component.text("That hurt! It seems like the water is extremely corrosive. Best to stay out of it.", NamedTextColor.AQUA));
					}
				} else {
					player.sendMessage(Component.text("You feel a powerful force pull you back in fiercely. It seems there's no escape from this fight.", NamedTextColor.AQUA));
				}
			}
		});

		// These spells need to be shared between phases in this manner to prevent double casting on phase change
		SpellLightningStorm lightningStorm = new SpellLightningStorm(mPlugin, boss, mShrineLoc);
		SpellBlockBreak bossBlockBreak = new SpellBlockBreak(mBoss, 1, 3, 1, FLOOR_Y, false, true, false);
		SpellShieldStun shieldStun = new SpellShieldStun(30 * 20);
		SpellBaseParticleAura greenParticleAura = new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) ->
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0),
				FLOOR_Y, 0.35, 0.45, 0.35, Material.GREEN_CONCRETE.createBlockData()).spawnAsBoss());
		SpellBaseParticleAura angryParticleAura = new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData()).spawnAsBoss();
			new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, 0.1).spawnAsBoss();
			new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, RED_COLOR).spawnAsBoss();
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.BLUE_WOOL.createBlockData()).spawnAsBoss();
		});
		SpellConditionalTeleport conditionalTeleport = new SpellConditionalTeleport(mBoss, spawnLoc,
			b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				|| b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				|| b.getLocation().getBlock().getType() == Material.LAVA
				|| b.getLocation().getBlock().getType() == Material.WATER);

		List<Spell> phase1PassiveSpells = Arrays.asList(
			new SpellLightningStrike(boss, LIGHTNING_STRIKE_COOLDOWN_SECONDS_1, false, mShrineLoc),
			lightningStorm, bossBlockBreak, shieldStun, greenParticleAura, conditionalTeleport, action
		);

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new SpellLightningStrike(boss, LIGHTNING_STRIKE_COOLDOWN_SECONDS_2, true, mShrineLoc),
			lightningStorm, bossBlockBreak, shieldStun, greenParticleAura, conditionalTeleport, action
		);

		List<Spell> phase3PassiveSpells = Arrays.asList(
			new SpellLightningStrike(boss, LIGHTNING_STRIKE_COOLDOWN_SECONDS_3, true, mShrineLoc),
			lightningStorm, bossBlockBreak, shieldStun, angryParticleAura, conditionalTeleport, action
		);

		List<Spell> phase4PassiveSpells = Arrays.asList(
			new SpellLightningStrike(boss, LIGHTNING_STRIKE_COOLDOWN_SECONDS_4, true, mShrineLoc),
			lightningStorm, bossBlockBreak, shieldStun, angryParticleAura, conditionalTeleport, action
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			Collection<Player> players = getArenaParticipants();
			String message = players.size() > 1 ?
				"THE JUNGLE WILL TAKE YOUR PRESENCE NO MORE. PERISH, USURPERS." :
				"THE JUNGLE WILL NOT ALLOW A LONE MORTAL LIKE YOU TO LIVE. PERISH, FOOLISH USURPER!";
			sendDialogue(message);
		});

		events.put(75, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Arachnopocolypse at 75% health! Overriding with phase 1 spells");
				changePhase(phase1Spells, phase1PassiveSpells, null);
			}
			forceCastSpell(SpellArachnopocolypse.class);
		});

		// Phase 2
		events.put(66, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 66% health");
			sendDialogue("THE JUNGLE WILL DEVOUR YOU. ALL RETURNS TO ROT.");
			knockbackPlayers();
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				int delay = 20 * 2;
				new PPSpiral(Particle.SPELL_WITCH, spawnLoc, 10)
					.countPerBlockPerCurve(24)
					.delta(0.25)
					.ticks(delay)
					.curves(5)
					.curveAngle(360)
					.reversed(true)
					.spawnAsBoss();
				new PPSpiral(Particle.BLOCK_CRACK, spawnLoc, 10)
					.countPerBlockPerCurve(24)
					.delta(0.25)
					.data(Material.COARSE_DIRT.createBlockData())
					.ticks(delay)
					.curves(5)
					.curveAngle(360)
					.reversed(true)
					.spawnAsBoss();

				teleport(spawnLoc.clone().add(0, 5, 0));
				new BukkitRunnable() {
					float mT = 0;

					@Override
					public void run() {
						mT++;
						world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 3, 0.5f + (mT / 25));
						new PartialParticle(Particle.SPELL_WITCH, mShrineLoc.clone().add(0, 3, 0), 20, 8, 5, 8, 0).spawnAsBoss();

						if (mT >= delay) {
							this.cancel();
							phase2(mShrineLoc, phase2Spells, phase2PassiveSpells);
						}
					}
				}.runTaskTimer(mPlugin, 30, 1);
			}, 20 * 2);
		});

		// Forcecast Raise Jungle
		events.put(60, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Raise Jungle at 60% health! Overriding with phase 2 spells");
				changePhase(phase2Spells, phase2PassiveSpells, null);
			}
			forceCastSpell(SpellRaiseJungle.class);
		});

		// Phase 2.5
		events.put(50, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 50% health");
			sendDialogue("THE EARTH AND JUNGLE ARE ENTWINED. PRIMORDIAL, HEWN FROM SOIL AND STONE, END THEM.");
			knockbackPlayers();
			teleport(spawnLoc.clone().add(0, 5, 0));
			changePhase(SpellManager.EMPTY, phase1PassiveSpells, null);

			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mPrimordialPhase = true;

			int delay = 66;

			new PPSpiral(Particle.SPELL_WITCH, spawnLoc, 10)
				.countPerBlockPerCurve(20)
				.delta(0.25)
				.ticks(delay)
				.curves(5)
				.curveAngle(360)
				.reversed(true)
				.spawnAsBoss();

			new PPSpiral(Particle.BLOCK_CRACK, spawnLoc, 10)
				.countPerBlockPerCurve(20)
				.delta(0.25)
				.extra(0.25)
				.data(Material.DIRT.createBlockData())
				.ticks(delay)
				.curves(5)
				.curveAngle(360)
				.reversed(true)
				.spawnAsBoss();

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				world.playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0);
				world.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
				new PartialParticle(Particle.CRIT_MAGIC, spawnLoc, 50, 0.1, 0.1, 0.1, 1).spawnAsBoss();
				new PartialParticle(Particle.BLOCK_CRACK, spawnLoc, 150, 0.1, 0.1, 0.1, 0.5,
					Material.DIRT.createBlockData()).spawnAsBoss();

				LivingEntity miniboss = (LivingEntity) LibraryOfSoulsIntegration.summon(spawnLoc, primordial);
				if (miniboss == null) {
					MMLog.warning(() -> "[Kaul] Kaul tried to summon the Primordial Elemental, but the miniboss is null!");
					return;
				}

				MMLog.info(() -> "[Kaul] Kaul has summoned the Primordial Elemental");
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!miniboss.isValid()) {
							MMLog.info(() -> "[Kaul] Kaul's Primordial Elemental is dead or no longer valid. Entering next phase");
							this.cancel();

							mBoss.setInvulnerable(false);
							mBoss.setAI(true);
							teleport(spawnLoc);
							mPrimordialPhase = false;

							changePhase(phase2Spells, phase2PassiveSpells, null, 20 * 10);
						}
					}
				}.runTaskTimer(mPlugin, 0, 20);
			}, delay);
		});

		// Force-cast Kaul's Judgement if it hasn't been cast yet.
		events.put(40, mBoss -> {
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Kaul's Judgement " + kaulsJudgement + " at 40% health! Overriding with phase 2 spells");
				changePhase(phase2Spells, phase2PassiveSpells, null);
			}
			forceCastSpell(SpellKaulsJudgement.class);
		});

		// Phase 3
		events.put(33, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 33% health");
			sendDialogue("YOU ARE NOT ANTS, BUT PREDATORS. YET THE JUNGLE'S WILL IS MANIFEST; DEATH COMES TO ALL.");
			knockbackPlayers();
			changePhase(SpellManager.EMPTY, phase1PassiveSpells, null);

			mBoss.setInvulnerable(true);
			mBoss.setAI(false);

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				teleport(spawnLoc.clone().add(0, 5, 0));

				world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5, 0.75f);

				int delay = 50;

				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					for (SpellPutridPlague.Pillar pillar : SpellPutridPlague.Pillar.values()) {
						Vector offset = pillar.mOffset;
						Location loc = mShrineLoc.clone().add(offset).add(0, 15, 0);

						world.createExplosion(loc, 6, true);
						world.createExplosion(loc.clone().subtract(0, 4, 0), 6, true);

						Vector dir = offset.clone().multiply(-1 / PILLAR_DISTANCE);

						if (pillar == SpellPutridPlague.Pillar.BLUE) {
							new PPLine(Particle.FALLING_DUST, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delta(0.4)
								.delay(delay)
								.data(Material.BLUE_WOOL.createBlockData())
								.spawnAsBoss();
							new PPLine(Particle.BLOCK_CRACK, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delta(0.4)
								.delay(delay)
								.data(Material.BLUE_WOOL.createBlockData())
								.spawnAsBoss();
							new PPLine(Particle.EXPLOSION_NORMAL, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(5)
								.delta(0.4)
								.delay(delay)
								.spawnAsBoss();
						} else if (pillar == SpellPutridPlague.Pillar.RED) {
							new PPLine(Particle.REDSTONE, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delta(0.4)
								.delay(delay)
								.data(RED_COLOR)
								.spawnAsBoss();
							new PPLine(Particle.FALLING_DUST, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(15)
								.delta(0.4)
								.delay(delay)
								.data(Material.RED_WOOL.createBlockData())
								.spawnAsBoss();
						} else if (pillar == SpellPutridPlague.Pillar.YELLOW) {
							new PPLine(Particle.FLAME, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delay(delay)
								.delta(0.3)
								.extra(0.1)
								.spawnAsBoss();
							new PPLine(Particle.SMOKE_LARGE, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delta(0.4)
								.delay(delay)
								.spawnAsBoss();
						} else if (pillar == SpellPutridPlague.Pillar.GREEN) {
							new PPLine(Particle.FALLING_DUST, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(20)
								.delta(0.4)
								.delay(delay)
								.data(Material.GREEN_TERRACOTTA.createBlockData())
								.spawnAsBoss();
							new PPLine(Particle.BLOCK_CRACK, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(10)
								.delta(0.4)
								.delay(delay)
								.data(Material.GREEN_TERRACOTTA.createBlockData())
								.spawnAsBoss();
							new PPLine(Particle.EXPLOSION_NORMAL, loc, dir, PILLAR_DISTANCE)
								.countPerMeter(5)
								.delta(0.4)
								.delay(delay)
								.spawnAsBoss();
						}
					}
				}, 2 * 20);

				new BukkitRunnable() {
					float mT = 0;

					@Override
					public void run() {
						mT++;
						if (mT % 2 == 0) {
							new PartialParticle(Particle.SPELL_WITCH, mShrineLoc.clone().add(0, 3, 0), 30, 8, 5, 9, 0).spawnAsBoss();
						}
						new PartialParticle(Particle.FLAME, mShrineLoc.clone().add(0, 3, 0), 30, 8, 5, 9, 0).spawnAsBoss();
						new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1.25, 0), 48, 0.35, 0.45, 0.35, 0).spawnAsBoss();
						new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.25, 0), 3, 0.35, 0.45, 0.35, 0).spawnAsBoss();

						if (mT > delay) {
							this.cancel();
							phase3(mShrineLoc, phase3Spells, phase3PassiveSpells);
						}
					}
				}.runTaskTimer(mPlugin, 2 * 20, 1);
			}, 20 * 2);
		});

		// Phase 3.25
		//Summons a Immortal Elemental at 30% HP
		events.put(30, mBoss -> {
			sendDialogue("PRIMORDIAL, RETURN, NOW AS UNDYING AND EVERLASTING AS THE MOUNTAIN.");
			summonImmortal(world);
		});


		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(25, mBoss -> {
			// If we get here really fast, we might not actually have the active spells yet
			if (getActiveSpells().isEmpty()) {
				MMLog.warning(() -> "[Kaul] Kaul somehow had no active spells when attempting to force cast Kaul's Judgement " + kaulsJudgement + " at 25% health! Overriding with phase 3 spells");
				changePhase(phase3Spells, phase3PassiveSpells, null);
			}
			forceCastSpell(SpellKaulsJudgement.class);
		});

		events.put(10, mBoss -> {
			MMLog.info(() -> "[Kaul] A Kaul fight got to 10% health");
			sendDialogue("THE VALLEY RUNS RED WITH BLOOD TODAY. LET THIS BLASPHEMY END. PREDATORS, FACE THE FULL WILL OF THE JUNGLE. COME.");
			changePhase(phase4Spells, phase4PassiveSpells, null);
			// Force casting Volcanic Demise teleports Kaul back to his camp spot on top of the shrine and whatnot
			// See bossCastAbility() for more info
			forceCastSpell(SpellVolcanicDemise.class);
		});
		BossBarManager bossBar = new BossBarManager(boss, detectionRange + 30, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);

		//Construct the boss with a delay to prevent the passives from going off during the dialogue
		new BukkitRunnable() {
			@Override
			public void run() {
				constructBoss(phase1Spells, phase1PassiveSpells, detectionRange, bossBar, 20 * 10);

				// Advancements listeners

				mAdvancements.forEach(KaulAdvancementHandler::onBossSpawn);
				new BukkitRunnable() {
					@Override
					public void run() {
						mAdvancements.forEach(KaulAdvancementHandler::onTick);
						if (mDefeated || !mBoss.isValid()) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}.runTaskLater(mPlugin, DIALOGUE_DELAY * 3);
	}

	private void phase2(Location shrineLoc, SpellManager activeSpells, List<Spell> passiveSpells) {
		Location loc = shrineLoc.clone().subtract(0, 0.5, 0);
		changePhase(SpellManager.EMPTY, passiveSpells, null);

		new BukkitRunnable() {
			int mRadius = 0;

			@Override
			public void run() {
				mRadius++;
				new PartialParticle(Particle.SPELL_WITCH, shrineLoc.clone().add(0, 3, 0), 20, 8, 5, 8, 0).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_NORMAL, shrineLoc.clone().add(0, 3, 0), 10, 8, 5, 8, 0).spawnAsBoss();
				new PPCircle(Particle.SPELL_WITCH, loc, mRadius)
					.countPerMeter(4)
					.delta(0.4)
					.spawnAsBoss();
				new PPCircle(Particle.BLOCK_CRACK, loc, mRadius)
					.countPerMeter(3)
					.data(Material.COARSE_DIRT.createBlockData())
					.delta(0.4)
					.extra(0.25)
					.spawnAsBoss();
				for (Block block : BlockUtils.getEdge(loc, mRadius)) {
					if (FastUtils.RANDOM.nextInt(6) == 1 && block.getType() == Material.SMOOTH_SANDSTONE
						&& block.getLocation().add(0, 1.5, 0).getBlock()
						.getType() == Material.AIR
					) {
						block.setType(Material.SMOOTH_RED_SANDSTONE);
					}
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		for (Player player : getArenaParticipants()) {
			player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0.75f);
		}
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.setInvulnerable(false);
			mBoss.setAI(true);
			teleport(mSpawnLoc);

			changePhase(activeSpells, passiveSpells, null, 20 * 10);
		}, 20 * 2);
	}

	private void phase3(Location shrineLoc, SpellManager activeSpells, List<Spell> passiveSpells) {
		new PartialParticle(Particle.SPELL_WITCH, shrineLoc.clone().add(0, 3, 0), 25, 6, 5, 6, 1).spawnAsBoss();
		new PartialParticle(Particle.FLAME, shrineLoc.clone().add(0, 3, 0), 40, 6, 5, 6, 0.1).spawnAsBoss();

		new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsBoss();

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0.9f);
		world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5, 0f);

		changePhase(SpellManager.EMPTY, passiveSpells, null);
		EntityUtils.replaceAttribute(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("Phase3Speed", 0.02, AttributeModifier.Operation.ADD_NUMBER));
		EntityUtils.replaceAttribute(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("Phase3Damage", 3.0, AttributeModifier.Operation.ADD_NUMBER));

		new BukkitRunnable() {
			final Location mLoc = shrineLoc.clone().subtract(0, 0.5, 0);
			int mRadius = 0;

			@Override
			public void run() {
				mRadius++;
				new PPCircle(Particle.FLAME, mLoc, mRadius)
					.countPerMeter(4)
					.delta(0.4)
					.spawnAsBoss();
				new PPCircle(Particle.BLOCK_CRACK, mLoc, mRadius)
					.countPerMeter(3)
					.data(Material.COARSE_DIRT.createBlockData())
					.delta(0.4)
					.extra(0.25)
					.spawnAsBoss();
				for (Block block : BlockUtils.getEdge(mLoc, mRadius)) {
					if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
						block.setType(Material.NETHERRACK);
						if (FastUtils.RANDOM.nextInt(3) == 1) {
							block.setType(Material.MAGMA_BLOCK);
						}
					} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
						block.setType(Material.SMOOTH_RED_SANDSTONE);
					}
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.setInvulnerable(false);
			mBoss.setAI(true);
			teleport(mSpawnLoc);
			changePhase(activeSpells, passiveSpells, null, 20 * 10);
		}, 20 * 3);
	}

	private void summonImmortal(World world) {
		int delay = 20;

		new PPSpiral(Particle.SPELL_WITCH, mSpawnLoc, 10)
			.countPerBlockPerCurve(20)
			.delta(0.25)
			.ticks(delay)
			.curves(5)
			.curveAngle(360)
			.reversed(true)
			.spawnAsBoss();

		new PPSpiral(Particle.BLOCK_CRACK, mSpawnLoc, 10)
			.countPerBlockPerCurve(20)
			.delta(0.25)
			.extra(0.25)
			.data(Material.DIRT.createBlockData())
			.ticks(delay)
			.curves(5)
			.curveAngle(360)
			.reversed(true)
			.spawnAsBoss();

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(mSpawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0);
			world.playSound(mSpawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
			new PartialParticle(Particle.CRIT_MAGIC, mSpawnLoc, 150, 0.1, 0.1, 0.1, 1).spawnAsBoss();

			mImmortal = (LivingEntity) LibraryOfSoulsIntegration.summon(mSpawnLoc, immortal);
			MMLog.info(() -> "[Kaul] Kaul has summoned the Immortal Elemental");
		}, delay);
	}

	private void knockbackPlayers() {
		World world = mBoss.getWorld();
		Location bossLocation = mBoss.getLocation();
		world.playSound(bossLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(bossLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		world.playSound(bossLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 0f);

		for (Player player : PlayerUtils.playersInRange(bossLocation, 10, true)) {
			MovementUtils.knockAway(bossLocation, player, 0.55f, false);
			EffectType.applyEffect(EffectType.SLOW, player, 5 * 20, 0.3, "KaulKnockbackSlowness", false);
		}
		new BukkitRunnable() {
			final Location mLoc = mBoss.getLocation().add(0, 2.5, 0);
			final double mLowestY = mBoss.getY();
			double mRadius = 0;
			double mYminus = 0.35;

			@Override
			public void run() {
				mRadius += 1;
				new PPCircle(Particle.SMOKE_LARGE, mLoc, mRadius)
					.countPerMeter(4)
					.delta(0.1)
					.spawnAsBoss();
				new PPCircle(Particle.BLOCK_CRACK, mLoc, mRadius)
					.countPerMeter(6)
					.data(Material.COARSE_DIRT.createBlockData())
					.delta(0.2)
					.extra(0.25)
					.spawnAsBoss();
				mLoc.setY(Math.max(mLowestY, mLoc.getY() - mYminus));
				mYminus += 0.02;
				if (mYminus >= 1) {
					mYminus = 1;
				}
				if (mRadius >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void teleport(Location loc) {
		World world = loc.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);

		new PartialParticle(Particle.SPELL_WITCH, bossLoc.clone().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, bossLoc.clone().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, bossLoc, 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();

		mBoss.teleport(loc);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);

		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();

	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		/* Boss deals AoE damage when melee'ing a player */
		Location bossLocation = mBoss.getLocation();
		if (event.getType() == DamageType.MELEE && damagee.getLocation().distance(bossLocation) <= 2) {
			if (!mCooldown) {
				mCooldown = true;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, 20);
				UUID uuid = damagee.getUniqueId();
				for (Player player : PlayerUtils.playersInRange(bossLocation, 4, true)) {
					if (!player.getUniqueId().equals(uuid)) {
						BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				world.playSound(bossLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 0);
				new PartialParticle(Particle.DAMAGE_INDICATOR, bossLocation, 30, 2, 2, 2, 0.1).spawnAsBoss();
				new PartialParticle(Particle.SWEEP_ATTACK, bossLocation, 10, 2, 2, 2, 0.1).spawnAsBoss();
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setFlatDamage(event.getFlatDamage() / mDefenseScaling);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		mAdvancements.forEach(a -> a.onPlayerDeath(event.getPlayer()));
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		Spell spell = event.getSpell();
		if (spell != null && spell.castTicks() > 0) {
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			new BukkitRunnable() {
				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						// If the Primordial Elemental is active, don't allow other abilities to turn Kaul's AI back on
						if (!mPrimordialPhase) {
							mBoss.setInvulnerable(false);
							mBoss.setAI(true);
							teleport(mSpawnLoc);
							List<Player> players = getArenaParticipants();
							if (!players.isEmpty()) {
								Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
								((Mob) mBoss).setTarget(newTarget);
							}
						}
					}, spell.castTicks());
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		MMLog.info(() -> "[Kaul] Kaul's death method has been called. If the logs don't contain messages for health events and Primordial/Immortal Elemental, this is a problem!");
		List<Player> players = getArenaParticipants();
		if (players.isEmpty()) {
			return;
		}
		mDefeated = true;
		String[] dio = new String[]{
			"AS ALL RETURNS TO ROT, SO TOO HAS THIS ECHO FALLEN.",
			"DO NOT THINK THIS ABSOLVES YOUR BLASPHEMY. RETURN HERE AGAIN, AND YOU WILL PERISH.",
			"NOW... THE JUNGLE... MUST SLEEP..."
		};
		knockbackPlayers();
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}
		BossUtils.endBossFightEffects(mBoss, players, 20 * 20, true, true);
		mAdvancements.forEach(KaulAdvancementHandler::onBossDeath);

		if (mImmortal != null) {
			mImmortal.remove();
		}

		new BukkitRunnable() {
			final Location mLoc = mShrineLoc.clone().subtract(0, 0.5, 0);
			int mRadius = 0;

			@Override
			public void run() {
				mRadius++;
				new PPCircle(Particle.CLOUD, mLoc, mRadius)
					.countPerMeter(4)
					.delta(0.25)
					.extra(0.025)
					.spawnAsBoss();
				new PPCircle(Particle.VILLAGER_HAPPY, mLoc, mRadius)
					.countPerMeter(FLOOR_Y)
					.delta(0.4)
					.extra(0.25)
					.spawnAsBoss();
				for (Block block : BlockUtils.getEdge(mLoc, mRadius)) {
					if (block.getType() == Material.MAGMA_BLOCK) {
						block.setType(Material.OAK_LEAVES);
						if (FastUtils.RANDOM.nextInt(5) == 1) {
							block.setType(Material.GLOWSTONE);
						}
					} else if (block.getType() == Material.SMOOTH_RED_SANDSTONE || block.getType() == Material.NETHERRACK) {
						block.setType(Material.GRASS_BLOCK);
						if (FastUtils.RANDOM.nextInt(3) == 1) {
							Block b = block.getLocation().add(0, 1.5, 0).getBlock();
							if (!b.getType().isSolid()) {
								b.setType(Material.SHORT_GRASS);
							}
						}
					}
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
		sendDialogue(20 * 6, dio, () -> {
			teleport(mSpawnLoc);
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT <= 0) {
						World world = mBoss.getWorld();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 10, 1);
					}
					if (mT <= 3 * 20) {
						mBoss.teleport(mBoss.getLocation().subtract(0, 0.05, 0));
						new PartialParticle(Particle.BLOCK_CRACK, mSpawnLoc, 7, 0.3, 0.1, 0.3, 0.25,
							Material.COARSE_DIRT.createBlockData()).spawnAsBoss();
					}
					if (mT == 3 * 20) {
						EntityEquipment equipment = mBoss.getEquipment();
						if (equipment != null) {
							equipment.clear();
						}
						mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 27, 0));
						mBoss.setAI(false);
						mBoss.setSilent(true);
						mBoss.setInvulnerable(true);
					} else if (mT >= 5 * 20) {
						this.cancel();

						for (Player player : getArenaParticipants()) {
							MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.GREEN), Component.text("Kaul, Soul of the Jungle", NamedTextColor.DARK_GREEN));
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
						}
						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						mBoss.remove();
					}

					mT++;
				}
			}.runTaskTimer(mPlugin, 30, 1);
		}, 0);
	}

	@Override
	public void init() {
		MMLog.info(() -> "[Kaul] A Kaul fight has been initialized.");
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(MAX_HEALTH);

		GlowingManager.startGlowing(mBoss, NamedTextColor.DARK_GREEN, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);

		EntityEquipment equips = mBoss.getEquipment();
		if (equips == null) {
			MMLog.severe("[Kaul] Kaul has no equipment!");
			return;
		}
		ItemStack[] armorc = equips.getArmorContents();
		ItemStack m = equips.getItemInMainHand();
		ItemStack o = equips.getItemInOffHand();

		// Disable White Tesseract for the duration of the fight. The tag is cleared in SQ login/death files and the win mcfunction
		getArenaParticipants().forEach(player -> player.addScoreboardTag("WhiteTessDisabled"));
		new BukkitRunnable() {

			@Override
			public void run() {
				Objects.requireNonNull(mBoss.getEquipment()).clear();
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 100, 0));
				mBoss.setAI(false);
				mBoss.setSilent(true);
				mBoss.setInvulnerable(true);

				World world = mBoss.getWorld();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 0f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0f);
				new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();

				String[] dio = new String[]{
					"THE JUNGLE'S WILL IS UNASSAILABLE, YET YOU SCURRY ACROSS MY SHRINE LIKE " + (mPlayerCount > 1 ? "ANTS." : "AN ANT."),
					"IS THE DEFILEMENT OF THE DREAM NOT ENOUGH!?"
				};

				sendDialogue(DIALOGUE_DELAY, dio, () -> {
					this.cancel();
					mBoss.setAI(true);
					mBoss.setSilent(false);
					mBoss.setInvulnerable(false);
					mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);

					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 0f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5, 0f);
					new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsBoss();

					mBoss.getEquipment().setArmorContents(armorc);
					mBoss.getEquipment().setItemInMainHand(m);
					mBoss.getEquipment().setItemInOffHand(o);

					for (Player player : getArenaParticipants()) {
						MessagingUtils.sendBoldTitle(player, Component.text("Kaul", NamedTextColor.DARK_GREEN), Component.text("Soul of the Jungle", NamedTextColor.GREEN));
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
					}
				}, DIALOGUE_DELAY);
			}
		}.runTaskLater(mPlugin, 1);
	}

	public static List<Player> getArenaParticipants(Location center) {
		List<Player> result = PlayerUtils.playersInRange(center, detectionRange, true);
		result.removeIf(player -> player.getLocation().getY() >= ARENA_MAX_Y);
		return result;
	}

	public List<Player> getArenaParticipants() {
		return getArenaParticipants(mShrineLoc);
	}

	public Collection<Player> getArenaSpectators() {
		List<Player> result = PlayerUtils.playersInXZRange(mShrineLoc, ARENA_RADIUS, true);
		result.removeIf(player -> player.getLocation().getY() < ARENA_MAX_Y);
		return result;
	}

	public static ChargeUpManager defaultChargeUp(LivingEntity boss, int chargeTime, String spellName) {
		return new ChargeUpManager(boss, chargeTime, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(spellName + "...", NamedTextColor.DARK_GREEN)), BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, detectionRange);
	}

	public void sendDialogue(int delay, String[] messages, Runnable runAfterDialogue, int runnableDelay) {
		new BukkitRunnable() {
			int mIndex = 0;

			@Override
			public void run() {
				// safety check
				if (mIndex >= messages.length) {
					this.cancel();
					return;
				}

				// send message
				sendDialogue(messages[mIndex]);

				mIndex++;
				if (mIndex >= messages.length) {
					this.cancel();
					Bukkit.getScheduler().runTaskLater(mPlugin, runAfterDialogue, runnableDelay);
				}
			}
		}.runTaskTimer(mPlugin, 0, delay);
	}

	public void sendDialogue(String message) {
		Component component = Component.text(message, NamedTextColor.DARK_GREEN);
		for (Player player : getArenaParticipants()) {
			player.sendMessage(component);
		}
	}

	public void judgementSuccess(Player p) {
		mAdvancements.forEach(a -> a.onJudgementSuccess(p));
	}

	private final List<KaulAdvancementHandler> mAdvancements = Arrays.asList(
		// MLOCI
		new KaulAdvancementHandler() {
			final HashSet<UUID> mHs = new HashSet<>();
			int mTick = 0;

			boolean hasFedora(Player p) {
				ItemStack helmet = p.getInventory().getHelmet();
				if (helmet == null) {
					return false;
				}
				return ItemUtils.getPlainName(helmet).equals("Fedora");
			}

			@Override
			void onBossSpawn() {
				getArenaParticipants().forEach(p -> {
					if (hasFedora(p)) {
						mHs.add(p.getUniqueId());
					}
				});
			}

			@Override
			void onTick() {
				mTick++;
				if (mTick % 10 != 0) {
					return;
				}
				getArenaParticipants().forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && !hasFedora(p)) {
						mHs.remove(p.getUniqueId());
					}
				});
			}

			@Override
			void onBossDeath() {
				getArenaParticipants().forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && hasFedora(p)) {
						AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/mloci");
					}
				});
			}
		},

		// CELEBRITY
		new KaulAdvancementHandler() {
			int mTick = 0;

			@Override
			void onTick() {
				mTick++;
				if (mTick % 10 == 0 && getArenaSpectators().size() >= 10) {
					getArenaParticipants().forEach(p -> AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/celebrity"));
				}
			}
		},

		// SO CLOSE
		new KaulAdvancementHandler() {
			final HashSet<UUID> mHs = new HashSet<>();

			@Override
			void onBossSpawn() {
				getArenaParticipants().forEach(p -> mHs.add(p.getUniqueId()));
			}

			@Override
			void onPlayerDeath(Player p) {
				if (mHs.remove(p.getUniqueId()) && mBoss.getHealth() / MAX_HEALTH <= 0.1) {
					AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/soclose");
				}
			}
		},

		// UNLUCKY
		new KaulAdvancementHandler() {
			private final HashSet<UUID> mSucceededPlayersForAdvancement = new HashSet<>();

			@Override
			void onJudgementSuccess(Player p) {
				// If this is the player's second success, grant achievement. Otherwise mark that they have succeeded once.
				if (mSucceededPlayersForAdvancement.remove(p.getUniqueId())) {
					AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/unlucky");
				} else {
					mSucceededPlayersForAdvancement.add(p.getUniqueId());
				}
			}
		},

		// Vanilla
		new KaulAdvancementHandler() {
			final HashSet<UUID> mHs = new HashSet<>();
			int mTick = 0;

			@Override
			void onBossSpawn() {
				getArenaParticipants().forEach(p -> {
					if (AbilityUtils.isClassless(p)) {
						mHs.add(p.getUniqueId());
					}
				});
			}

			@Override
			void onTick() {
				mTick++;
				if (mTick % 10 != 0) {
					return;
				}
				getArenaParticipants().forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && !AbilityUtils.isClassless(p)) {
						mHs.remove(p.getUniqueId());
					}
				});
			}

			@Override
			void onBossDeath() {
				getArenaParticipants().forEach(p -> {
					if (mHs.contains(p.getUniqueId()) && AbilityUtils.isClassless(p)) {
						AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r1/kaul/vanilla");
					}
				});
			}
		});

	private static class KaulAdvancementHandler {
		void onBossSpawn() {
		}

		void onBossDeath() {
		}

		void onPlayerDeath(Player p) {
		}

		void onTick() {
		}

		void onJudgementSuccess(Player p) {
		}
	}
}
