package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public class SpellLightningStorm extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final List<Player> mWarnedPlayers = new ArrayList<>();
	private final Set<Player> mCooldownPlayers = new HashSet<>();

	private static final long LIGHTNING_COOLDOWN = Constants.TICKS_PER_SECOND;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	public SpellLightningStorm(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public void run() {
		// This should be 8.0 based on the armour stand placement
		for (Player player : Kaul.getArenaParticipants(mCenter)) {
			// If standing on heightened ground or going up a climbable to cheese,
			// enforce stricter 2-block threshold.
			int yThreshold = PlayerUtils.isFreeFalling(player) ? 8 : 2;

			if (player.getLocation().getY() > mCenter.getY() + yThreshold && !mCooldownPlayers.contains(player)) {
				mCooldownPlayers.add(player);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldownPlayers.remove(player), LIGHTNING_COOLDOWN);
				lightning(player);
			}
		}
	}

	private void lightning(Player player) {
		World world = player.getWorld();
		Location strike = player.getLocation().add(0, 10, 0);
		Location loc = player.getLocation();
		for (int i = 0; i < 10; i++) {
			strike.subtract(0, 1, 0);
			new PartialParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_1_COLOR).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_2_COLOR).spawnAsEntityActive(mBoss);
		}
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 15, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.9f);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1, 1);

		/* Too-high lightning hits you directly and can not be blocked */
		BossUtils.bossDamagePercent(mBoss, player, 0.5, "Lightning Storm");
		if (!mWarnedPlayers.contains(player)) {
			mWarnedPlayers.add(player);
			player.sendMessage(Component.text("That hurt! There must be a lightning storm above you. Staying close to the ground might help to not get struck again.", NamedTextColor.AQUA));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
