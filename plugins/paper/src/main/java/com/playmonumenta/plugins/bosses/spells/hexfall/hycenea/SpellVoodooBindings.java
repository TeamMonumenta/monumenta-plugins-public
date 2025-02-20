package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPSpiral;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellVoodooBindings extends Spell {

	private static final String ABILITY_NAME = "Voodoo Bindings";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final List<String> mCustomSet;
	private final ChargeUpManager mChargeUp;

	public SpellVoodooBindings(Plugin plugin, LivingEntity boss, int range, int castTime, int cooldown, Location spawnLoc, List<String> customSet) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mCustomSet = customSet;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mChargeUp.getTime() % 20 == 0) {
					new PPSpiral(Particle.SPELL_WITCH, mBoss.getLocation(), 2)
						.distanceFalloff(50)
						.count(20)
						.spawnAsBoss();

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player, Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 0.2f, 1f);
						player.playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 0.8f, 1f);
						player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1f, 1f);
					}
				}

				if (mChargeUp.nextTick()) {
					List<String> currentSet = mCustomSet;
					Collections.shuffle(currentSet);

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {

						String effectTag = "";
						if (currentSet.size() > 0) {
							effectTag = currentSet.get(0);
							currentSet.remove(0);
						}

						Queue<VoodooBindings.VoodooBinding> queue = new ArrayBlockingQueue<>(10);

						for (int i = 0; i <= effectTag.length() - 1; i += 2) {
							queue.add(VoodooBindings.VoodooBinding.stringToEnum(effectTag.substring(i, i + 2)));
						}

						mPlugin.mEffectManager.addEffect(player, VoodooBindings.GENERIC_NAME, new VoodooBindings(20 * 600, queue));

						player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1, 0f);
						player.playSound(player, Sound.ENTITY_WARDEN_LISTENING, SoundCategory.HOSTILE, 1, 0.5f);
					}

					new PPSpiral(Particle.SPELL_WITCH, mBoss.getLocation(), 7)
						.distanceFalloff(50)
						.count(120)
						.spawnAsBoss();

					mChargeUp.remove();
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
