package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Mitosis implements Infusion {

	private static final int DURATION = 3 * 20;
	private static final int RADIUS = 5;
	private static final double PERCENT_WEAKEN_PER_LEVEL = 0.0375;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(214, 148, 181), 1.0f);

	@Override
	public String getName() {
		return "Mitosis";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.MITOSIS;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		double percentWeaken = PERCENT_WEAKEN_PER_LEVEL * modifiedLevel;
		ItemStack item = player.getInventory().getItemInMainHand();
		//If we break a spawner with a pickaxe
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.7f);
			new PartialParticle(Particle.REDSTONE, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5, COLOR).spawnAsPlayerActive(player);
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);
			for (LivingEntity mob : mobs) {
				EntityUtils.applyWeaken(plugin, DURATION, percentWeaken, mob);
			}
		}
	}

}
