package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.libraryofsouls.SoulGroup;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PlayerHallucinationBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class BanishSporeHallucination extends Spell {
	private static final int BANISH_DURATION = 20 * 15;
	private static final int EXTRA_TIME_PER_MOB = 20 * 2;
	private static final int MOB_AMOUNT = 4;
	private static final int MAX_DURATION = BANISH_DURATION + EXTRA_TIME_PER_MOB * MOB_AMOUNT;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SporousAmalgam mSporeBeast;
	private final Player mTargetPlayer;

	private @Nullable BossBar mBossBar;
	private final NamedTextColor mGlowingColor = NamedTextColor.DARK_GREEN;

	//This spell isn't instanced in the boss but gets created in the SporeLasers class when needed
	public BanishSporeHallucination(Plugin plugin, SporousAmalgam sporeBeast, Player player) {
		mPlugin = plugin;
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
		mTargetPlayer = player;
	}

	@Override
	public void run() {
		if (mSporeBeast.getPlayersInBanish().contains(mTargetPlayer)) {
			return;
		}
		Component component = Component.text("Hallucinations left: " + MOB_AMOUNT, TextColor.color(0, 175, 0));
		mBossBar = BossBar.bossBar(component, (float) BANISH_DURATION / MAX_DURATION, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
		MessagingUtils.sendTitle(mTargetPlayer, Component.text(""), Component.text(Quarry.BANISH_CHARACTER, TextColor.color(0, 175, 0)));
		mTargetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, false, false));
		mTargetPlayer.showBossBar(mBossBar);

		mSporeBeast.addPlayerInBanish(mTargetPlayer);
		List<LivingEntity> hallucinations = new ArrayList<>();
		List<String> souls = LibraryOfSoulsIntegration.getPool(SporousAmalgam.NORMAL_POOL_NAME).keySet().stream().map(SoulGroup::getLabel).toList();
		String soul = souls.get(FastUtils.RANDOM.nextInt(souls.size()));
		for (int i = 0; i < MOB_AMOUNT; i++) {
			hallucinations.add((LivingEntity) LibraryOfSoulsIntegration.summon(mSporeBeast.getRandomLocationInArena(10, 0, 0), soul));
			hallucinations.get(i).removeScoreboardTag("SporeBeastSummon");
			EntityUtils.setMaxHealthAndHealth(hallucinations.get(i), hallucinations.get(i).getHealth() / 100 * 80);
			BossManager.getInstance().manuallyRegisterBoss(hallucinations.get(i), new PlayerHallucinationBoss(mPlugin, hallucinations.get(i), mTargetPlayer));
			GlowingManager.startGlowing(hallucinations.get(i), mGlowingColor, BANISH_DURATION * 2, GlowingManager.BOSS_SPELL_PRIORITY);
		}
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (int i = 0; i < hallucinations.size(); i++) {
					LivingEntity check = hallucinations.get(i);
					if (check.isDead()) {
						hallucinations.remove(check);
						mTicks -= EXTRA_TIME_PER_MOB;
						i--;
					} else if (check.getLocation().distanceSquared(mBoss.getLocation()) >= SporousAmalgam.SPELL_INNER_RADIUS * SporousAmalgam.SPELL_INNER_RADIUS) {
						check.setVelocity(LocationUtils.getDirectionTo(mBoss.getLocation(), check.getLocation()).multiply(1.5));
					}
				}

				if (hallucinations.isEmpty()) {
					mTargetPlayer.hideBossBar(mBossBar);
					mSporeBeast.removePlayerInBanish(mTargetPlayer);
					this.cancel();
				}

				if (mTicks >= BANISH_DURATION) {
					if (!hallucinations.isEmpty()) {
						mSporeBeast.banish(mTargetPlayer);
					}
					for (LivingEntity e : hallucinations) {
						e.remove();
					}
					mTargetPlayer.hideBossBar(mBossBar);
					mSporeBeast.removePlayerInBanish(mTargetPlayer);
					this.cancel();
				}

				updateBossBar(hallucinations.size(), BANISH_DURATION - mTicks);

				if (mTargetPlayer.isDead() || mBoss.isDead()) {
					for (LivingEntity e : hallucinations) {
						e.remove();
					}
					mTargetPlayer.hideBossBar(mBossBar);
					mSporeBeast.removePlayerInBanish(mTargetPlayer);
					this.cancel();
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void updateBossBar(int mobs, int tickLeft) {
		Component name = Component.text("Hallucinations remaining: " + mobs, SporousAmalgam.TEXT_COLOR);
		if (mBossBar != null) {
			mBossBar.name(name);
			mBossBar.progress(Math.max((float) tickLeft / MAX_DURATION, 0));
		}
	}
}
