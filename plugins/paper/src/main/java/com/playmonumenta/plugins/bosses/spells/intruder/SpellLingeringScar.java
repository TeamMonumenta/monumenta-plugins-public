package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellLingeringScar extends Spell {
	private final double mYLevel;
	private final LivingEntity mBoss;
	private final Plugin mPlugin;

	private static final double RADIUS = 3;
	private static final double RADIUS_FINAL_STAND = 2;
	private static final double DAMAGE = 10;
	private static final int CHARGE_TIME = 6 * 20;
	private static final String SPELL_NAME = "Lingering Scar";

	private final Set<Block> mChangedBlocks = new HashSet<>();
	private final Set<Player> mWarnedPlayers = new HashSet<>();
	private final SpellCooldownManager mSpellCooldownManager;

	private boolean mLastStand = false;

	public SpellLingeringScar(Plugin plugin, LivingEntity boss, double yLevel) {
		mPlugin = plugin;
		mBoss = boss;
		mYLevel = yLevel;
		mSpellCooldownManager = new SpellCooldownManager(10 * 20, mBoss::isValid, mBoss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();
		List<Player> players = IntruderBoss.playersInRange(mBoss.getLocation());
		perpareScar(FastUtils.getRandomElement(players), mLastStand ? RADIUS_FINAL_STAND : RADIUS);
	}

	private void perpareScar(Player player, double radius) {
		if (!mWarnedPlayers.contains(player)) {
			mWarnedPlayers.add(player);
			player.sendMessage(Component.text("The ground beneath you starts to mutate into a weird red substance...", NamedTextColor.GRAY, TextDecoration.ITALIC));
		}
		player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 3.0f, 1.6f);
		player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_DEATH, SoundCategory.HOSTILE, 0.6f, 0.1f);
		ChargeUpManager chargeUpManager = new ChargeUpManager(mBoss, CHARGE_TIME, Component.text("Charging ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.GRAY)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
		mActiveTasks.add(new BukkitRunnable() {
			Location mLocation = player.getLocation();

			@Override
			public void run() {
				if (chargeUpManager.nextTick()) {
					chargeUpManager.remove();
					player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_DEATH, SoundCategory.HOSTILE, 3.6f, 0.88f);
					player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 2.8f, 0.666f);
					spawnScar(mLocation, radius);
					this.cancel();
				} else if (chargeUpManager.getTime() % 10 == 0) {
					player.playSound(mLocation, Sound.AMBIENT_CRIMSON_FOREST_ADDITIONS, SoundCategory.HOSTILE, 2.0f, 2.0f, 2);
					player.playSound(mLocation, Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, SoundCategory.HOSTILE, 2.0f, 1.0f);
					player.playSound(mLocation, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 2.0f, 1.4f);

					new PPCircle(Particle.DUST_COLOR_TRANSITION, mLocation, radius)
						.data(new Particle.DustTransition(Color.WHITE, Color.RED, 1.75f))
						.delta(0.2)
						.count(35)
						.spawnAsBoss();
				}
				if (CHARGE_TIME - chargeUpManager.getTime() >= 30) {
					mLocation = player.getLocation();
				}
				if (CHARGE_TIME - chargeUpManager.getTime() == 30) {
					chargeUpManager.setTitle(Component.text("Channeling ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void spawnScar(Location location, double radius) {
		Location loc = location.clone();
		loc.setY(mYLevel);

		Set<Location> blockLocations = new HashSet<>();

		for (int degree = 0; degree < 360; degree++) {
			for (double r = 0; r < radius; r += 1) {
				Vector vec = VectorUtils.rotateYAxis(new Vector(1, 0, 0), degree).multiply(r);
				Location locBlock = loc.clone().add(vec);
				blockLocations.add(locBlock.toBlockLocation());
				Block block = locBlock.subtract(new Vector(0, 1, 0)).getBlock();
				TemporaryBlockChangeManager.INSTANCE.changeBlock(block, Material.CRIMSON_NYLIUM, 60 * 60 * 20);
				mChangedBlocks.add(block);
			}
		}
		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
				}
				PlayerUtils.playersInRange(loc, radius * 2, true, false).forEach(player -> {
					if (blockLocations.stream().anyMatch(locTest -> locTest.distance(player.getLocation()) < 1)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE);
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, true, false));
						player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 10, true, false));
						EffectManager.getInstance().addEffect(player, "LiminalScarSpeed", new PercentSpeed(20, -0.15, "LiminalScarSpeed"));
						EffectManager.getInstance().addEffect(player, "LiminalScarHeal", new PercentHeal(20, -0.3));
					}
				});
			}
		}.runTaskTimer(mPlugin, 0, 10));
	}

	public void setLastStand() {
		mLastStand = true;
	}

	@Override
	public int cooldownTicks() {
		return 2 * 20;
	}

	@Override
	public void cancel() {
		super.cancel();
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.CRIMSON_NYLIUM);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}
}
