package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellEngulfingPsyche extends Spell {

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final Location mCenter;

	//Each ring layer removed, increment to keep track of location
	private int mRingIncrement = 0;
	private final Set<Player> mHitPlayers = new HashSet<>();

	private final Set<Block> mChangedBlocks = new HashSet<>();
	private final List<TextDisplay> mAnnoys = new ArrayList<>();
	private static final List<String> ANNOYS = List.of(
		"SUCCUMB", "YIELD", "CONCEDE", "RELENT", "STOP TRYING", "GIVE UP"
	);
	private static final int HEIGHT = 18;
	private static final int SCALE = 4;
	private static final double DAMAGE_PERCENT = 0.25;
	private static final String SPELL_NAME = "Engulfing Psyche";

	public SpellEngulfingPsyche(Plugin plugin, LivingEntity boss, Location centerLocation) {
		mBoss = boss;
		mPlugin = plugin;
		mCenter = centerLocation;
	}

	public void prepareBorder() {
		mActiveTasks.add(new BukkitRunnable() {

			@Override
			public void run() {
				if (FastUtils.RANDOM.nextDouble() <= 0.25) {
					new PPCircle(Particle.DUST_COLOR_TRANSITION, mCenter.clone().add(new Vector(0, HEIGHT / 3.0, 0)), 25 - mRingIncrement + 2)
						.count(200)
						.data(new Particle.DustTransition(Color.MAROON, Color.BLACK, 5.0f))
						.delta(1, HEIGHT / 2.0, 1)
						.spawnAsBoss();
				}
				List<Player> players = IntruderBoss.playersInRange(mCenter);
				players.forEach(player -> {
					Location l = player.getLocation();
					l.setY(mCenter.getY());
					if (l.distance(mCenter) >= 25 - mRingIncrement && !mHitPlayers.contains(player)) {
						mHitPlayers.add(player);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mHitPlayers.remove(player), 10);
						BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT, SPELL_NAME);
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, true, false));
						EffectManager.getInstance().addEffect(player, "EngulfmentSpeed", new PercentSpeed(20, -0.1, "PsychicEngulfmentSpeed"));
						EffectManager.getInstance().addEffect(player, "EngulfmentHeal", new PercentHeal(20, -0.2));
						MovementUtils.pullTowards(mCenter.clone().add(new Vector(0, 1, 0)), player, 0.1f);
					}
				});
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	@Override
	public void run() {
		BukkitRunnable runnable;
		runnable = new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % 10 == 0) {
					mBoss.getWorld().playSound(LocationUtils.randomLocationInCircle(mBoss.getLocation(), 5), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.4f, 0.66f);
					mBoss.getWorld().playSound(LocationUtils.randomLocationInCircle(mBoss.getLocation(), 5), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.HOSTILE, 3.0f, 0.1f);
					mBoss.getWorld().playSound(LocationUtils.randomLocationInCircle(mBoss.getLocation(), 5), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 1.6f, 15);

					Vector vec;
					Location l = mCenter.clone();
					//The degree range is 60 degrees for 30 blocks radius
					int r = 25 - mRingIncrement;
					for (double degree = 0; degree < 360; degree += 0.5) {
						double radian1 = Math.toRadians(degree);
						vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);

						//Deletes rings/layers (like an onion)
						l.set(mCenter.getX(), mCenter.getY() - 1, mCenter.getZ()).add(vec);

						Block b = l.getBlock();
						TemporaryBlockChangeManager.INSTANCE.changeBlock(b, Material.CRIMSON_NYLIUM, 60 * 60 * 20);
						mChangedBlocks.add(b);

						if (FastUtils.RANDOM.nextDouble() < 0.02) {
							TextDisplay spawn = l.getWorld().spawn(l.clone().add(new Vector(0, FastUtils.randomDoubleInRange(0, HEIGHT), 0)), TextDisplay.class);
							mAnnoys.add(spawn);
							EntityUtils.setRemoveEntityOnUnload(spawn);
							spawn.setAlignment(TextDisplay.TextAlignment.CENTER);
							spawn.setBillboard(Display.Billboard.CENTER);
							spawn.setTransformation(new Transformation(
								new Vector3f(),
								new AxisAngle4f(),
								new Vector3f(SCALE, SCALE, SCALE),
								new AxisAngle4f()
							));
							spawn.setBrightness(new Display.Brightness(15, 15));
							spawn.setBackgroundColor(Color.fromARGB(0x00000000));
							setText(spawn);
							mActiveTasks.add(new BukkitRunnable() {
								@Override
								public void run() {
									setText(spawn);
								}
							}.runTaskTimer(mPlugin, 0, 10));
						}
					}
					mRingIncrement++;

					if (mTicks >= 50) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.5f, 0.01f);
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 0.7f, 0.5f);
						this.cancel();
					}
				}
				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private static void setText(TextDisplay spawn) {
		if (FastUtils.RANDOM.nextDouble() < 0.1) {
			spawn.getWorld().playSound(spawn.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 0.6f, 1.5f, 31);
		}
		Component textComponent = MessagingUtils.fromMiniMessage(
			String.format("<bold><color:#6b0000><obfuscated>#</obfuscated>%s<obfuscated>#</obfuscated>", FastUtils.getRandomElement(ANNOYS)
			));
		spawn.text(textComponent);
		spawn.setTransformation(new Transformation(
			new Vector3f(FastUtils.randomFloatInRange(-1, 1), FastUtils.randomFloatInRange(-1, 1), FastUtils.randomFloatInRange(-1, 1)),
			new AxisAngle4f(),
			new Vector3f(SCALE, SCALE, SCALE),
			new AxisAngle4f()
		));
	}

	@Override
	public void cancel() {
		super.cancel();
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.CRIMSON_NYLIUM);
		mChangedBlocks.clear();
		mAnnoys.forEach(Entity::remove);
		mAnnoys.clear();
	}

	public double getSafeRadius() {
		return 25 - mRingIncrement;
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
