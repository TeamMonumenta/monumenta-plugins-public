package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class Mudquake extends Spell {
	private static final int MUD_REQUIREMENT = 80;

	private static final int WINDUP_DURATION = 2 * 20;

	private static final int ATTACK_DAMAGE = 100;

	// how far to check above and below the player to see if they are standing on a block
	private static final int CHECK_BELOW = 8;
	private static final int CHECK_ABOVE = 2;

	private static final int ANTIJUMP_DURATION = 4 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperiment;
	private final ChargeUpManager mChargeUp;

	public Mudquake(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experiment) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperiment = experiment;
		mChargeUp = new ChargeUpManager(mBoss, WINDUP_DURATION, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text("Mudquake", NamedTextColor.GRAY, TextDecoration.BOLD)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 60);
	}

	@Override
	public boolean canRun() {
		return mExperiment.canRunSpell(this) && (mExperiment.getMudBlocks().size() > MUD_REQUIREMENT);
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, WINDUP_DURATION);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mChargeUp.update();
				mChargeUp.setProgress((float) mTicks / WINDUP_DURATION);

				if (mTicks % 10 == 0 && mTicks < WINDUP_DURATION) {
					List<Block> mudBlocks = getSurfaceMudBlocks();
					int count = (int) (4.5 - (2.5 * Math.min(mudBlocks.size(), 100)) / 100);
					for (Block block : mudBlocks) {
						new PartialParticle(Particle.BLOCK_CRACK, block.getLocation().clone().add(0, 1, 0))
							.data(Material.MUD.createBlockData())
							.delta(0.5, 0, 0.5)
							.count(count)
							.distanceFalloff(25)
							.spawnAsBoss();
					}

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 10, 0.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 10, 0.8f);
				}

				if (mTicks == WINDUP_DURATION) {
					List<Block> mudBlocks = getSurfaceMudBlocks();
					int count = (int) (4.5 - (2.5 * Math.min(mudBlocks.size(), 100)) / 100);
					for (Block block : mudBlocks) {
						new PartialParticle(Particle.CRIT, block.getLocation().clone().add(FastUtils.randomDoubleInRange(-0.5, 0.5), 1, FastUtils.randomDoubleInRange(-0.5, 0.5)))
							.delta(0, 0.5, 0)
							.extra(2)
							.directionalMode(true)
							.count(count)
							.distanceFalloff(25)
							.spawnAsBoss();
					}

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 10.5f, 2);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 10.5f, 1);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_TURTLE_EGG_BREAK, SoundCategory.HOSTILE, 10, 0.6f);

					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 60, true)) {
						for (int i = -CHECK_BELOW; i < CHECK_ABOVE; i++) {
							Block testBlock = player.getLocation().clone().add(0, i, 0).getBlock();
							if (mExperiment.getMudBlocks().contains(testBlock)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, ATTACK_DAMAGE, null, false, true, "Mudquake");
								MovementUtils.knockAway(testBlock.getLocation(), player, 0f, 1.1f, false);
								player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ANTIJUMP_DURATION, -4));

								break;
							}
						}
					}
				}

				mTicks++;
				if (mTicks > WINDUP_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private List<Block> getSurfaceMudBlocks() {
		return mExperiment.getMudBlocks().stream().filter(b -> b.getRelative(BlockFace.UP).isEmpty()).toList();
	}

	@Override
	public int cooldownTicks() {
		return 15 + WINDUP_DURATION;
	}
}
