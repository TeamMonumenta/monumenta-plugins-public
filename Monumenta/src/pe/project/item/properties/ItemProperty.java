package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;
import pe.project.utils.InventoryUtils;

public interface ItemProperty {
	public String getProperty();

	public EnumSet<ItemSlot> validSlots();

	/*
	 * Computes what level the given item is for this particular ItemProperty.
	 * If the item does not have this property, it should return 0
	 */
	default public int getLevelFromItem(ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, getProperty());
	}

	/*
	 * applyProperty will be called every time the player changes their inventory
	 * and the player matches this ItemProperty
	 */
	default public void applyProperty(Plugin plugin, Player player, int level) { }

	/*
	 * removeProperty will be called every time the player changes their inventory
	 * and they previously had this property active, even if it is still active
	 * (in which case applyProperty will be called again immediately afterwards)
	 *
	 * TODO: Modify this so it is only called when the item effect should actually
	 * be removed
	 */
	default public void removeProperty(Plugin plugin, Player player) { }


	/* If hasTickingEffect returns true, the tick() method will be called once per second */
	default public boolean hasTickingEffect() {
		return false;
	}
	default public void tick(Plugin plugin, World world, Player player, int level) { }

	/*
	 * If hasOnAttack() returns true, the onAttack() method will be called whenever the
	 * player damages something while they have any levels of this property
	 */
	default public boolean hasOnAttack() {
		return false;
	}
	default public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, int level, DamageCause cause) {
		return damage;
	}

	/*
	 * TODO: Add an onRightClick() method so you can make items that cast spells
	 */
}
