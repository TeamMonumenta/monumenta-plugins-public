package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Nutcracker extends Ability {
	private static final String SCOREBOARD = "Nutcracker";
	private static final int POINT_COST = 2;
	private static final double SPEED = 0.1;
	private static final int DURATION = 5 * 20;

	public static final AbilityInfo<Nutcracker> INFO =
		new SnowPerkGui.SnowPerkInfo<>(Nutcracker.class, "Nutcracker", Nutcracker::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.NETHERITE_PICKAXE)
			.description(getDescription());

	public Nutcracker(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(final BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SPAWNER) {
			mPlugin.mEffectManager.addEffect(mPlayer, "NutcrackerSpeed", new PercentSpeed(DURATION, SPEED, "NutcrackerSpeed"));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, DURATION, 0, true));

			Location loc = BlockUtils.getCenterBlockLocation(event.getBlock());
			for (int i = 0; i < 15; i++) {
				new PartialParticle(Particle.SPELL_MOB_AMBIENT, LocationUtils.varyInUniform(loc, 0.5), 1).extra(1).directionalMode(true)
					.delta(new Vector(0.8, 0.8, 0)).spawnAsPlayerActive(mPlayer);
			}
		}
		return true;
	}

	public static Description<Nutcracker> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Breaking a spawner grants you")
			.addLine("+%p *Speed* and *Haste 1* for %t.").styles(DescriptionUtils.WHITE, DescriptionUtils.WHITE)
				.statValues(stat(SPEED), stat(DURATION))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
