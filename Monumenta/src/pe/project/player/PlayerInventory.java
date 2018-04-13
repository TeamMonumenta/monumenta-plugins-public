package pe.project.player;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import pe.project.Plugin;
import pe.project.item.properties.ItemProperty;
import pe.project.item.properties.ItemPropertyManager;

public class PlayerInventory {
	Map<ItemProperty, Integer> mCurrentProperties = new HashMap<ItemProperty, Integer>();
	boolean mHasTickingProperty = false;
	boolean mHasOnAttack = false;

	public PlayerInventory(Plugin plugin, Player player) {
		updateEquipmentProperties(plugin, player);
	}

	public void tick(Plugin plugin, World world, Player player) {
		/* If there is no ticking property on our gear early out */
		if (!mHasTickingProperty) {
			return;
		}

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			if (property.hasTickingEffect()) {
				property.tick(plugin, world, player, level);
			}
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player) {
		/*
		 * Loop through existing equipment properties, remove all of them
		 *
		 * TODO: Modify this so only the removed effects are removed
		 * This is probably hard to do without making any new objects
		 * (which defeats the purpose)
		 */
		removeProperties(plugin, player);

		/* Once that's done, loop through the current players inventory and re-register the properties */
		getAndApplyProperties(plugin, player);
	}

	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, DamageCause cause) {
		/* If there is no onAttack() property on our gear early out */
		if (!mHasOnAttack) {
			return damage;
		}

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			damage = property.onAttack(plugin, world, player, target, damage, level, cause);
		}

		return damage;
	}

	public void removeProperties(Plugin plugin, Player player) {
		mHasTickingProperty = false;
		mHasOnAttack = false;

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();

			property.removeProperty(plugin, player);
		}

		mCurrentProperties.clear();
	}

	private void getAndApplyProperties(Plugin plugin, Player player) {
		ItemPropertyManager.getItemProperties(mCurrentProperties, player);

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.applyProperty(plugin, player, level);
			if (property.hasTickingEffect()) {
				mHasTickingProperty = true;
			}
			if (property.hasOnAttack()) {
				mHasOnAttack = true;
			}
		}
	}
}
