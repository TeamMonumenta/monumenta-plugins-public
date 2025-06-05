package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SpellAntiCheese extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	private static final double DAMAGE_PERCENT = 0.25;
	private static final int IMMUNITY_TICKS = 10;

	public SpellAntiCheese(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	Set<Player> mDamagedPlayers = new HashSet<>();
	private boolean mAmalgamatingDreamscape = false;

	@Override
	public void run() {
		EntityUtils.getNearbyMobs(mCenter, IntruderBoss.DETECTION_RANGE)
			.stream().filter(this::inStillwater)
			.forEach(this::throwEntity);

		IntruderBoss.playersInRange(mCenter)
			.stream().filter(player -> inStillwater(player) && !mDamagedPlayers.contains(player))
			.forEach(player -> {
				mDamagedPlayers.add(player);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mDamagedPlayers.remove(player), IMMUNITY_TICKS);
				BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT, "Stillwater");
				throwEntity(player);
			});
	}

	private boolean inStillwater(LivingEntity entity) {
		return !mAmalgamatingDreamscape && entity.getLocation().getBlock().getType() == Material.WATER;
	}

	private void throwEntity(LivingEntity entity) {
		Location location = entity.getLocation();
		location.setY(mCenter.getY() + 1);
		entity.teleport(location);
		entity.setVelocity(new Vector(0, 0.9, 0));

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> entity.setVelocity(LocationUtils.getDirectionTo(mCenter, entity.getLocation()).multiply(2)), 10);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 1));
	}

	public void setAmalgamatingDreamscape(boolean value) {
		mAmalgamatingDreamscape = value;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
