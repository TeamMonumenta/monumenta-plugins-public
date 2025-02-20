package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.ChargedDeath;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.DarkRetaliation;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.DeathTouchedBlade;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.DeathlyBombs;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.FearfulSouls;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.GhostlyFlames;
import com.playmonumenta.plugins.bosses.spells.xenotopsis.UmbralCannons;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Xenotopsis extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_xenotopsis";

	public static final int DETECTION_RANGE = 50;

	public static final TextColor DIALOGUE_COLOR = TextColor.color(100, 95, 107);

	// the boss' unscaled health in part 1 and 2
	private static final int BASE_HEALTH_PART1 = 15000;
	private static final int BASE_HEALTH_PART2 = 12500;

	// the percent health for the damage cap and the overflow scaling
	private static final double DAMAGE_CAP_PERCENT = 0.03;
	private static final double DAMAGE_CAP_REDUCTION = 0.9;

	// the boss' movement speed in part 1 and 2
	private static final double MOVEMENT_SPEED_PART1 = 0.24;
	private static final double MOVEMENT_SPEED_PART2 = 0.28;

	// the attack and death damage of the melee attack
	private static final int MELEE_ATTACK_DAMAGE = 65;
	private static final int MELEE_DEATH_DAMAGE = 7;

	// the factor of gallery scaling applied to the boss
	private static final double HEALTH_SCALING_FACTOR = 0.5;
	private static final double DAMAGE_SCALING_FACTOR = 0.3;

	// values for decreasing a player's death value when hit by melee
	private static final int DAMAGE_DECREASE_CUTOFF = 70;
	private static final int DAMAGE_DECREASE_VALUE = 4;

	// how long shields are disabled for when the boss disables them, in ticks
	public static final int SHIELD_STUN_TIME = 5 * 20;

	private int mMeleeDeathDamageOverrideDamage;
	private boolean mMeleeDeathDamageOverride = false;

	private final Map<Player, Integer> mDeathValues = new HashMap<>();
	private final Map<Player, BossBar> mDeathBossBars = new HashMap<>();
	private final Map<Player, Integer> mTicksSinceLastDeathChange = new HashMap<>();

	private final BossBarManager mBossBar;

	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final World mWorld;

	@Nullable private Spell mLastCastedSpell = null;

	private final double mMaxHealthPart1;
	private final double mMaxHealthPart2;

	private double mDamageScale = 1.0;

	private int mPhase = 1;
	private int mCooldownTicks;

	public Xenotopsis(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mWorld = boss.getWorld();
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();

		// load gallery game based on players nearby
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), DETECTION_RANGE, true);
		if (players.size() == 0) {
			MMLog.severe("[Xenotopsis] Boss started with zero detected players nearby, proceeding without gallery game.");

			mMaxHealthPart1 = BASE_HEALTH_PART1;
			mMaxHealthPart2 = BASE_HEALTH_PART2;
		} else {
			GalleryGame galleryGame = GalleryManager.getGame(players.get(0));
			if (galleryGame == null) {
				MMLog.severe("[Xenotopsis] Boss started without a valid gallery game, proceeding.");

				mMaxHealthPart1 = BASE_HEALTH_PART1;
				mMaxHealthPart2 = BASE_HEALTH_PART2;
			} else {
				double healthScale = (galleryGame.getHealthScale() - 1) * HEALTH_SCALING_FACTOR + 1; // get gallery health scale and scale by factor
				mMaxHealthPart1 = BASE_HEALTH_PART1 * healthScale;
				mMaxHealthPart2 = BASE_HEALTH_PART2 * healthScale;

				mDamageScale = (galleryGame.getDamageScale() - 1) * DAMAGE_SCALING_FACTOR + 1; // get gallery damage scale and scale by factor
			}
		}

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		EntityUtils.setMaxHealthAndHealth(mBoss, mMaxHealthPart1);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, getMovementSpeed());
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, DETECTION_RANGE);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, MELEE_ATTACK_DAMAGE);
		mBoss.setPersistent(true);
		mBoss.setInvulnerable(true);

		mCooldownTicks = 20 * 6;

		// opening messages, scheduled with delay
		sendDialogueMessage("THOSE WHO SEEK TO DISRUPT THE MARCH OF DEATH WILL BE CAUGHT IN THE TIDE, CURSED TO DROWN IN THE TAR-DARK DEEP");
		Bukkit.getScheduler().runTaskLater(plugin, () -> sendDialogueMessage("DEATH MARCHES EVER ONWARD, HALTED BY NO MORTAL"), 100);
		Bukkit.getScheduler().runTaskLater(plugin, () -> sendDialogueMessage("YER DISRUPTION IS POINTLESS"), 160);
		Bukkit.getScheduler().runTaskLater(plugin, () -> sendDialogueMessage("FORFEIT YERSELF TO THE CURSE"), 210);

		// make the boss glow a gray color
		GlowingManager.startGlowing(mBoss, NamedTextColor.DARK_GRAY, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);

		// Create spells
		SpellManager emptySpells = SpellManager.EMPTY;
		List<Spell> emptyPassives = Collections.emptyList();

		SpellManager phase1Spells = new SpellManager(List.of(
			new FearfulSouls(plugin, boss, this, mCooldownTicks, 3),
			new DeathlyBombs(plugin, boss, this, mCooldownTicks, 1, 5),
			new GhostlyFlames(plugin, boss, this, mCooldownTicks, 4.0)
		));
		List<Spell> phase1Passives = List.of(
			new SpellShieldStun(SHIELD_STUN_TIME),
			new DarkRetaliation(plugin, boss, this)
		);

		SpellManager phase2Spells = new SpellManager(List.of(
			new FearfulSouls(plugin, boss, this, mCooldownTicks, 3),
			new UmbralCannons(plugin, boss, this, mCooldownTicks),
			new DeathlyBombs(plugin, boss, this, mCooldownTicks, 1, 9),
			new GhostlyFlames(plugin, boss, this, mCooldownTicks, 6.5)
		));
		List<Spell> phase2Passives = List.of(
			new SpellShieldStun(SHIELD_STUN_TIME),
			new DarkRetaliation(plugin, boss, this)
		);

		SpellManager phase3Spells = new SpellManager(List.of(
			new FearfulSouls(plugin, boss, this, mCooldownTicks, 4),
			new UmbralCannons(plugin, boss, this, mCooldownTicks),
			new DeathlyBombs(plugin, boss, this, mCooldownTicks, 2, 13),
			new GhostlyFlames(plugin, boss, this, mCooldownTicks, 11),
			new DeathTouchedBlade(plugin, boss, this, mCooldownTicks, 2.75)
		));
		List<Spell> phase3Passives = List.of(
			new SpellShieldStun(SHIELD_STUN_TIME),
			new ChargedDeath(plugin, boss, this),
			new DarkRetaliation(plugin, boss, this)
		);

		// Create health events
		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(60, (mBoss) -> {
			mPhase = 2;
			mCooldownTicks = 20 * 3;

			sendDialogueMessage("DEATH'S INFLUENCE WILL ONLY EXPAND");

			changePhase(phase2Spells, phase2Passives, null);
		});
		events.put(5, (mBoss) -> {
			mPhase = 3;
			mCooldownTicks = 0;
			EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, getMovementSpeed());

			partTwoTransition();

			// remove spells and add them back when the cutscene is done
			changePhase(emptySpells, emptyPassives, null);
			Bukkit.getScheduler().runTaskLater(plugin, () -> changePhase(phase3Spells, phase3Passives, null), 180);
		});

		mBossBar = new BossBarManager(boss, DETECTION_RANGE, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);
		super.constructBoss(phase1Spells, phase1Passives, DETECTION_RANGE, mBossBar);

		// initialize death values
		PlayerUtils.playersInRange(mBoss.getLocation(), DETECTION_RANGE, true).forEach(player -> {
			mDeathValues.put(player, 0);
			mTicksSinceLastDeathChange.put(player, 0);

			String playerName = player.getName();
			BossBar bar = BossBar.bossBar(Component.text(playerName + "'" + (playerName.toLowerCase(Locale.getDefault()).endsWith("s") ? "" : "s") + " Death", NamedTextColor.DARK_PURPLE), 0, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
			player.showBossBar(bar);
			mDeathBossBars.put(player, bar);
		});

		new BukkitRunnable() {
			@Override
			public void run() {
				// decrease all player's death values depending on range, if they have not been hit in the past two seconds
				PlayerUtils.playersInRange(mBoss.getLocation(), DETECTION_RANGE, true).forEach(player -> {
					Integer lastDeathChangeTicks = mTicksSinceLastDeathChange.get(player);
					if (lastDeathChangeTicks != null) {
						if (lastDeathChangeTicks >= 20 * 2) {
							double range = mBoss.getLocation().distance(player.getLocation());
							int ticks = lastDeathChangeTicks - 20 * 2;

							// check if death should be decreased this tick
							if ((range <= 6 && ticks % 8 == 0)
									|| (range > 6 && range <= 16 && ticks % 16 == 0)
									|| (range > 16 && ticks % 36 == 0)) {
								changePlayerDeathValue(player, -1, true);
							}
						}
					}
				});

				// increment ticks since last death change for each player
				mTicksSinceLastDeathChange.keySet().forEach(player -> {
					if (mTicksSinceLastDeathChange.get(player) != null) {
						mTicksSinceLastDeathChange.replace(player, mTicksSinceLastDeathChange.get(player) + 1);
					}
				});

				// particle effect at the feet of the boss
				if (mPhase >= 3) {
					new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().clone().add(0, 0.2, 0))
						.count(15)
						.data(Material.BLACK_CONCRETE.createBlockData())
						.delta(0.3, 0.05, 0.3)
						.spawnAsBoss();
				}

				// check if every player has died and fight should cancel
				if (PlayerUtils.playersInRange(mBoss.getLocation(), DETECTION_RANGE, true).size() == 0) {
					sendWorldDialogueMessage("DEATH'S HAND HAS ENSNARED YOU IN ITS GRASP. SINK BELOW THE WAVES, LIKE ALL THOSE WHO CAME BEFORE YOU THAT DARE ATTEMPT TO DEFY IT");
					mBoss.remove();
					this.cancel();
					mDeathBossBars.keySet().forEach(player -> player.hideBossBar(mDeathBossBars.get(player)));
					return;
				}

				if (mBoss.isDead()) {
					this.cancel();
					mDeathBossBars.keySet().forEach(player -> player.hideBossBar(mDeathBossBars.get(player)));
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		openingAnimation();
	}

	public boolean canRunSpell(Spell spell) {
		if (mLastCastedSpell == null) {
			return true;
		}

		return !mLastCastedSpell.equals(spell);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		super.bossCastAbility(event);

		mLastCastedSpell = event.getSpell();
	}

	public void sendWorldDialogueMessage(String text) {
		mWorld.getPlayers().forEach(player -> {
			player.sendMessage(Component.text(text.toUpperCase(Locale.getDefault()), DIALOGUE_COLOR));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.9f, 0.8f);
		});
	}

	public void sendDialogueMessage(String text) {
		PlayerUtils.playersInRange(mBoss.getLocation(), DETECTION_RANGE, true).forEach(player -> {
			player.sendMessage(Component.text(text.toUpperCase(Locale.getDefault()), DIALOGUE_COLOR));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.9f, 0.8f);
		});
	}

	public void sendDialogueMessageToPlayer(Player player, String text) {
		player.sendMessage(Component.text(text.toUpperCase(Locale.getDefault()), DIALOGUE_COLOR));
		player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.9f, 0.8f);
	}

	public double getMovementSpeed() {
		return switch (mPhase) {
			case 1, 2 -> MOVEMENT_SPEED_PART1;
			case 3 -> MOVEMENT_SPEED_PART2;
			default -> 0.0;
		};
	}

	public void setMeleeDeathDamageOverride(int damage) {
		mMeleeDeathDamageOverride = true;
		mMeleeDeathDamageOverrideDamage = damage;
	}

	public void removeMeleeDeathDamageOverride() {
		mMeleeDeathDamageOverride = false;
	}

	public double scaleDamage(double damage) {
		return damage * mDamageScale;
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		// check if damage exceeds damage cap
		double eventDamage = event.getDamage();
		double part1Cap = DAMAGE_CAP_PERCENT * mMaxHealthPart1;
		double part2Cap = DAMAGE_CAP_PERCENT * mMaxHealthPart2;
		if (mPhase < 3 && eventDamage > part1Cap) {
			event.setFlatDamage(part1Cap + (eventDamage - part1Cap) * (1 - DAMAGE_CAP_REDUCTION));
		} else if (eventDamage > part2Cap) {
			event.setFlatDamage(part2Cap + (eventDamage - part2Cap) * (1 - DAMAGE_CAP_REDUCTION));
		}

		double newBossHealth = event.getDamagee().getHealth() - event.getDamage();

		if (newBossHealth / mMaxHealthPart1 < 0.05 && mPhase < 3) {
			event.getDamagee().setHealth(mMaxHealthPart1 * 0.05 - 1); // set health to just under 5%, resulting in phase 3 trigger
			event.setFlatDamage(0);
		}

		if (event.getDamager() instanceof Player player) {
			if (event.getType() == DamageEvent.DamageType.MELEE && event.getDamage() > DAMAGE_DECREASE_CUTOFF) {
				changePlayerDeathValue(player, -DAMAGE_DECREASE_VALUE, true);
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);

		if (damagee instanceof Player player) {
			event.setFlatDamage(scaleDamage(event.getDamage()));

			Bukkit.getScheduler().runTask(mPlugin, () -> mMonumentaPlugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.WITHER));

			changePlayerDeathValue(player, mMeleeDeathDamageOverride ? mMeleeDeathDamageOverrideDamage : MELEE_DEATH_DAMAGE, false);
		}
	}

	// ignoreTickChange should only be true if the change is not an attack increasing death
	public void changePlayerDeathValue(Player player, int amount, boolean ignoreTickChange) {
		Integer playerDeathValue = mDeathValues.get(player);
		BossBar playerBossBar = mDeathBossBars.get(player);
		if (playerDeathValue != null && playerBossBar != null) {
			int newAmount = Math.max(0, playerDeathValue + amount);
			mDeathValues.replace(player, newAmount);
			playerBossBar.progress((float) Math.min(playerDeathValue.doubleValue() / 100.0, 1.0));

			if (!ignoreTickChange) {
				mTicksSinceLastDeathChange.replace(player, 0);
			}

			if (newAmount > 100) {
				mDeathValues.replace(player, 0);

				// kill the player
				sendDialogueMessageToPlayer(player, "LET YER SOUL BE CONSUMED BY THE ETERNAL MARCH OF DEATH");
				mMonumentaPlugin.mEffectManager.clearEffects(player, Stasis.GENERIC_NAME);
				mMonumentaPlugin.mEffectManager.clearEffects(player, VoodooBonds.PROTECTION_EFFECT);
				PotionEffect resist = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				if (resist != null && resist.getAmplifier() >= 4) {
					player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				}
				player.setInvulnerable(false);
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 100000, null, true, false, "The March of Death");
			}
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mWorld.playSound(mBoss.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 0.7f);
		mWorld.playSound(mBoss.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 0.9f);
		mWorld.playSound(mBoss.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 1.1f);
		mWorld.playSound(mBoss.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 1.3f);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 5f, 1.2f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 5f, 0.8f);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 3f, 0.53f);

		sendDialogueMessage("THE MARCH OF DEATH HAS FALLEN...RETREAT TO THE DEEP...SHELTER IN THE SHADE");
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> sendDialogueMessage("DEATH APPROACHES ME, I HAVE FAILED"), 100);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK), 140);
	}

	private void openingAnimation() {
		new BukkitRunnable() {
		    int mTicks = 0;

		    @Override
		    public void run() {
		        if (mTicks == 0) {
					mBoss.setAI(false);
					mBoss.setInvulnerable(true);

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2f, 0.63f);
				}
				if (mTicks == 20) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2f, 0.5f);
				}
				if (mTicks == 40) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2f, 0.7f);
				}
				if (mTicks == 60) {
					mBoss.setAI(true);
					mBoss.setInvulnerable(false);

					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2f, 0.9f);
				}

				double m = 0;
				int c = 0;
				if (mTicks < 20) {
					m = 2;
					c = 5;
				} else if (mTicks < 40) {
					m = 4.25;
					c = 10;
				} else if (mTicks < 60) {
					m = 7.5;
					c = 20;
				}
				for (int i = 0; i < c; i++) {
					double t = FastUtils.randomDoubleInRange(0, Math.PI * 2);
					double r = FastUtils.randomDoubleInRange(0, m);
					new PartialParticle(Particle.SMOKE_LARGE,
						mBoss.getLocation().clone().add(FastUtils.cos(t) * r, -0.5, FastUtils.sin(t) * r),
						1,
						0, 1, 0,
						FastUtils.randomDoubleInRange(0.17, 0.32), null, true)
						.spawnAsBoss();
				}

				Vector v1 = new Vector(0, 0, 1);
				Vector v1F = VectorUtils.rotateXAxis(VectorUtils.rotateYAxis(VectorUtils.rotateZAxis(v1, 360 * (double)(mTicks % 60) / 60), 360 * (double)(mTicks % 50) / 50), 360 * (double)(mTicks % 70) / 70).normalize().multiply(2);
				Vector v2 = new Vector(1, 0, 0);
				Vector v2F = VectorUtils.rotateXAxis(VectorUtils.rotateYAxis(VectorUtils.rotateZAxis(v2, 360 * (double)(mTicks % 50) / 50), 360 * (double)(mTicks % 70) / 70), 360 * (double)(mTicks % 60) / 60).normalize().multiply(2);
				new PartialParticle(Particle.END_ROD, mBoss.getLocation().clone().add(0, 1.5, 0).add(v1F), 1).spawnAsBoss();
				new PartialParticle(Particle.END_ROD, mBoss.getLocation().clone().add(0, 1.5, 0).add(v2F), 1).spawnAsBoss();

				if (mTicks == 60) {
					new PPExplosion(Particle.CAMPFIRE_COSY_SMOKE, mBoss.getLocation().clone().add(0, 1.5, 0))
						.extra(0.3)
						.count(150)
						.spawnAsBoss();
				}

		        mTicks++;
				if (mTicks > 60) {
					this.cancel();
				}
		    }
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void partTwoTransition() {
		new BukkitRunnable() {
		    int mTicks = 0;

		    @Override
		    public void run() {
		        if (mTicks == 0) {
					mBoss.setInvulnerable(true);
					mBoss.setGravity(false);
					mBoss.setAI(false);
					mBoss.setVelocity(new Vector(0, 0, 0));
					mBoss.teleport(mSpawnLoc.clone().add(0, 7, 0));
					EntityUtils.setMaxHealthAndHealth(mBoss, mMaxHealthPart2);
					mBoss.setHealth(mMaxHealthPart2 * 0.05);

					sendDialogueMessage("DEATH HAS ALREADY CLAIMED ME, YOU HAVE NO HOPE OF VICTORY...DEATH ITSELF IS UNMOVING, IT IS ONLY INEVITABILITY");
				}

				// start visual & sound effects

				// rune thing in the sky
				if (mTicks == 40) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 3f, 0.87f);
				}
				if (mTicks == 140) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_GATEWAY_SPAWN, 4f, 0.72f);
				}
				if (mTicks > 40 && mTicks < 130) {
					new PPCircle(Particle.SMOKE_LARGE, mBoss.getLocation().clone().add(0, 7, 0), 7)
						.delta(1, 0.1, 1)
						.count(15)
						.ringMode(false)
						.spawnAsBoss();

					double r = FastUtils.randomDoubleInRange(0, 5);
					double t = FastUtils.randomDoubleInRange(0, Math.PI * 2);
					new PartialParticle(Particle.END_ROD,
						mBoss.getLocation().clone().add(FastUtils.cos(t) * r, 7, FastUtils.sin(t) * r),
						1,
						0, -1, 0,
						FastUtils.randomDoubleInRange(0.13, 0.25), null, true)
						.spawnAsBoss();
				}

				// swirling particles
				if (mTicks > 40 && mTicks < 130) {
					Vector v1 = new Vector(0, 0, 1);
					Vector v1F = VectorUtils.rotateXAxis(VectorUtils.rotateYAxis(VectorUtils.rotateZAxis(v1, 360 * (double) (mTicks % 60) / 60), 360 * (double) (mTicks % 50) / 50), 360 * (double) (mTicks % 70) / 70).normalize().multiply(2);
					Vector v2 = new Vector(1, 0, 0);
					Vector v2F = VectorUtils.rotateXAxis(VectorUtils.rotateYAxis(VectorUtils.rotateZAxis(v2, 360 * (double) (mTicks % 50) / 50), 360 * (double) (mTicks % 70) / 70), 360 * (double) (mTicks % 60) / 60).normalize().multiply(2);
					new PartialParticle(Particle.END_ROD, mBoss.getLocation().clone().add(0, 1.5, 0).add(v1F), 1).spawnAsBoss();
					new PartialParticle(Particle.END_ROD, mBoss.getLocation().clone().add(0, 1.5, 0).add(v2F), 1).spawnAsBoss();
				}

				// helix
				if (mTicks == 100) {
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 4f, 0.53f);
				}
				if (mTicks > 100 && mTicks < 140) {
					double progress = (double)(mTicks - 100) / 40;

					new PartialParticle(Particle.END_ROD,
						mSpawnLoc.clone().add(0, -0.5 + (mBoss.getLocation().getY() + 3 - mSpawnLoc.getY()) * progress, 0),
						1,
						FastUtils.cos(Math.PI * 3 * progress), 0, FastUtils.sin(Math.PI * 3 * progress),
						0.13f, null, true)
						.spawnAsBoss();
					new PartialParticle(Particle.END_ROD,
						mSpawnLoc.clone().add(0, -0.5 + (mBoss.getLocation().getY() + 3 - mSpawnLoc.getY()) * progress, 0),
						1,
						FastUtils.cos(Math.PI * 3 * progress + Math.PI), 0, FastUtils.sin(Math.PI * 3 * progress + Math.PI),
						0.13f, null, true)
						.spawnAsBoss();

					new PartialParticle(Particle.CRIT_MAGIC,
						mSpawnLoc.clone().add(0, -0.5 + (mBoss.getLocation().getY() + 3 - mSpawnLoc.getY()) * progress, 0),
						1,
						FastUtils.cos(Math.PI * 3 * progress + Math.PI * 0.5), 0, FastUtils.sin(Math.PI * 3 * progress + Math.PI * 0.5),
						1f, null, true)
						.spawnAsBoss();
					new PartialParticle(Particle.CRIT_MAGIC,
						mSpawnLoc.clone().add(0, -0.5 + (mBoss.getLocation().getY() + 3 - mSpawnLoc.getY()) * progress, 0),
						1,
						FastUtils.cos(Math.PI * 3 * progress + Math.PI * 1.5), 0, FastUtils.sin(Math.PI * 3 * progress + Math.PI * 1.5),
						1f, null, true)
						.spawnAsBoss();
				}

				// lightning effect
				if (mTicks > 140 && mTicks < 160 && mTicks % 2 == 0) {
					mWorld.strikeLightningEffect(mBoss.getLocation());
				}

				// constant smoke
				if (mTicks < 180) {
					new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().clone().add(0, 1, 0), 5, 0.6, 0.3, 0.3)
						.spawnAsBoss();
				}

				// end visual & sound effects

				// heal
				if (mTicks > 40 && mTicks < 140 && mTicks % 2 == 0) {
					mBoss.setHealth((mMaxHealthPart2 * 0.05) + mMaxHealthPart2 * 0.95 * ((double) (mTicks - 40) / 100));
				}
				if (mTicks == 140) {
					mBoss.setHealth(mMaxHealthPart2);
				}

				// boss appearance changes
				if (mTicks == 150) {
					mBossBar.setTitle("§4§lXenotopsis, Captain of Death"); // magic strings are evil, but are the only way to preserve the formatting
				}

				if (mTicks == 180) {
					mBoss.setInvulnerable(false);
					mBoss.setGravity(true);
					mBoss.setAI(true);
					mBoss.teleport(mSpawnLoc);
				}

		        mTicks++;
				if (mTicks > 180 || mBoss.isDead()) {
					this.cancel();
				}
		    }
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
