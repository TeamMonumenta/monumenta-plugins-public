package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import javax.annotation.Nullable;

import java.util.List;



public class BrewingListener implements Listener {
	@EventHandler(ignoreCancelled = true)
	public void brewEvent(BrewEvent brewEvent) {
		BrewerInventory brewerInventory = brewEvent.getContents();
		@Nullable ItemStack ingredient = brewerInventory.getIngredient();
		if (ingredient != null) {
			boolean malfunction = false;
			// Individual items may be null
			// https://papermc.io/javadocs/paper/1.16/org/bukkit/inventory/Inventory.html#getStorageContents--
			for (@Nullable ItemStack potentialPotion : brewerInventory.getStorageContents()) {
				if (potentialPotion != null && ItemUtils.isSomePotion(potentialPotion)) {
					Material ingredientMaterial = ingredient.getType();
					if (
						// Slow falling + Recoil enchant essentially allows flight,
						// huge nope for Monumenta
						ingredientMaterial == Material.PHANTOM_MEMBRANE
						// Disallow high resistance
						|| ingredientMaterial == Material.TURTLE_HELMET
						// Disallow home-brewed fire resistance
						|| ingredientMaterial == Material.MAGMA_CREAM
					) {
						malfunction = true;
						break;
					} else if (ingredientMaterial == Material.FERMENTED_SPIDER_EYE) {
						ItemMeta itemMeta = potentialPotion.getItemMeta();
						if (itemMeta instanceof PotionMeta) {
							PotionData potionData
								= ((PotionMeta) itemMeta).getBasePotionData();
							if (potionData.getType() == PotionType.NIGHT_VISION) {
								malfunction = true;
								break;
							}
							// Allow if would not result in brewing invisibility
						}
					}
				}
			}

			if (malfunction) {
				brewEvent.setCancelled(true);

				//TODO scrap knockback, change effects (bubbling), add player grey + grey-italic messages

				// Knock players back
				Block block = brewEvent.getBlock();
				Location blockCentre = LocationUtils.getLocationCentre(block);
				List<Player> nearbyPlayers = PlayerUtils.playersInRange(blockCentre, 3, true);
				for (Player player : nearbyPlayers) {
					MovementUtils.knockAway(blockCentre, player, 1, false);
				}

				// Eject ingredient & refund
				brewerInventory.setIngredient(null);
				World world = blockCentre.getWorld();
				world.dropItemNaturally(blockCentre, ingredient);

				// Effects
				PartialParticle partialParticle = new PartialParticle(
					Particle.EXPLOSION_LARGE,
					blockCentre,
					2,
					0,
					0
				).spawnFull();
				partialParticle.mParticle = Particle.FLAME;
				partialParticle.mCount = 30;
				partialParticle.mExtra = 0.25;
				partialParticle.spawnFull();
				partialParticle.mParticle = Particle.SMOKE_LARGE;
				partialParticle.mExtra = 0.1;
				partialParticle.spawnFull();

				world.playSound(
					blockCentre,
					Sound.ENTITY_ENDERMAN_DEATH,
					SoundCategory.BLOCKS,
					1,
					0.2f
				);
				world.playSound(
					blockCentre,
					Sound.ENTITY_GENERIC_EXPLODE,
					SoundCategory.BLOCKS,
					1,
					1
				);
			}
		}
	}
}
