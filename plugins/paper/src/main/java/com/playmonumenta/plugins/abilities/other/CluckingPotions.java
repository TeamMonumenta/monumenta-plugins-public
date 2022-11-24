package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

/* TODO:
 * There is no reason this should be a player ability - it doesn't need a unique object per player
 *
 * It's still the most convenient place to put it for now, but it should eventually be refactored
 * into something like EntityListener
 */
public class CluckingPotions extends Ability {

	public static final AbilityInfo<CluckingPotions> INFO =
		new AbilityInfo<>(CluckingPotions.class, null, CluckingPotions::new)
			.canUse(player -> true)
			.ignoresSilence(true);

	public CluckingPotions(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Jar of Clucks")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.CLOUD);
			potion.setMetadata("CluckingPotion", new FixedMetadataValue(mPlugin, 0));
		}
		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("CluckingPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (entity instanceof Player player) {
						List<ItemStack> cluckingCandidates = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
						cluckingCandidates.add(player.getInventory().getItemInOffHand());
						cluckingCandidates.removeIf(item -> item.getType() == Material.AIR || ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.CLUCKING) > 0);
						if (!cluckingCandidates.isEmpty()) {
							ItemStack item = cluckingCandidates.get(FastUtils.RANDOM.nextInt(cluckingCandidates.size()));
							ItemStatUtils.addEnchantment(item, ItemStatUtils.EnchantmentType.CLUCKING, 1);
							ItemStatUtils.generateItemStats(item);
							mPlugin.mItemStatManager.updateStats(player);
							new PartialParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
						}
					}
				}
			}
		}
		return true;
	}
}
