package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.Block;
import org.bukkit.DyeColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class StandardBearer extends Ability {

	private static final int STANDARD_BEARER_ID = 67;
	public static final int STANDARD_BEARER_FAKE_ID = 10078;
	public static final String STANDARD_BEARER_TAG_NAME = "TagBearer";
	private static final double STANDARD_BEARER_ARMOR = 2;
	private static final double STANDARD_BEARER_TRIGGER_RANGE = 2;
	private static final int STANDARD_BEARER_COOLDOWN = 60 * 20;
	private static final int STANDARD_BEARER_DURATION = 30 * 20;
	private static final int STANDARD_BEARER_TRIGGER_RADIUS = 12;
	private static final double STANDARD_BEARER_DAMAGE_MULTIPLIER = 1.25;

	public StandardBearer(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean ProjectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		double range = arrow.getLocation().distance(mPlayer.getLocation());
		if (range <= STANDARD_BEARER_TRIGGER_RANGE) {
//			mPlugin.mPulseEffectTimers.AddPulseEffect(mPlayer, this, STANDARD_BEARER_ID,
//			                                          STANDARD_BEARER_TAG_NAME, STANDARD_BEARER_DURATION, 1, mPlayer.getLocation(),
//			                                          STANDARD_BEARER_TRIGGER_RADIUS, true);

			mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), Spells.STANDARD_BEARER, STANDARD_BEARER_COOLDOWN);

			Location L = mPlayer.getLocation();
			while (L.getBlock().getType() != Material.AIR) {
				L.add(0, 0.25, 0);
			}

			Block block = L.getBlock();
			block.setType(Material.CYAN_BANNER);

			if (block.getState() instanceof Banner) {
				Banner banner = (Banner)block.getState();

				banner.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRAIGHT_CROSS));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.CIRCLE_MIDDLE));
				banner.addPattern(new Pattern(DyeColor.BLACK, PatternType.FLOWER));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP));

				banner.update();
			}
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.getGameMode() != GameMode.ADVENTURE;
	}

}
