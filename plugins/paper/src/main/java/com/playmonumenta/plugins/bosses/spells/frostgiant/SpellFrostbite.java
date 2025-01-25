package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpellFrostbite extends Spell {
	private static final String SPELL_NAME = "Frostbite";

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final List<UUID> mWarned = new ArrayList<>();

	private int mTicks;

	public SpellFrostbite(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mTicks = 0;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, Constants.TICKS_PER_SECOND * 10);
	}

	@Override
	public void run() {
		mTicks += BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
		if (mTicks < (3 * Constants.TICKS_PER_SECOND / 4)) {
			return;
		}
		mTicks = 0;

		final World world = mBoss.getWorld();
		final Collection<Player> players = mFrostGiant.getArenaParticipants();
		players.forEach(player -> {
			final Location playerLoc = player.getLocation();
			final boolean tooHigh = playerLoc.getY() - FrostGiant.ARENA_FLOOR_Y >= 4.2;
			final boolean tooLow = playerLoc.getY() - FrostGiant.ARENA_FLOOR_Y <= -2;

			if (tooHigh || tooLow) {
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 2.0 + EntityUtils.getMaxHealth(player) * 0.1, null, true, false, SPELL_NAME);
				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 1);
				new PartialParticle(Particle.FIREWORKS_SPARK, playerLoc.add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPIT, playerLoc, 6, 0.4, 0.4, 0.4, 0.2).spawnAsEntityActive(mBoss);

				if (!mWarned.contains(player.getUniqueId())) {
					final String msg = tooHigh ? "The upper air is freezing!" : "The lower air is freezing!";
					player.sendMessage(Component.text(msg, NamedTextColor.RED));
					mWarned.add(player.getUniqueId());
				}
			}
		});
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
