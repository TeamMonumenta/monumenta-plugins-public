package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.utils.InventoryUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;




//TODO update method naming from when this class used to be ItemProperty.
// Update event method naming to be more accurate/precise.
// Standardise param order
public interface BaseEnchantment {
	/*
	 * Returns the name of this enchant.
	 *
	 * InventoryUtils.getCustomEnchantLevel() is moving away from legacy
	 * ChatColors, such as .GRAY or .RED.
	 * It processes plain text and does not use Adventure Components,
	 * so just provide a plain name, eg "Mainhand Regeneration".
	 */
	String getProperty();

	/*
	 * By default, this enchant will not be tested for in any slot!
	 * Return the slots of a player's inventory in which this enchant should be
	 * tested for.
	 *
	 * See ItemSlot's definition for what they represent & which cannot be
	 * duplicated!
	 */
	default EnumSet<ItemSlot> getValidSlots() {
		// Empty by default - no slots supported
		return EnumSet.noneOf(ItemSlot.class);
	}

	/*
	 * By default, this enchant can have > level 1,
	 * eg "Regeneration IV" is level 4.
	 * Return false instead if this enchant should not support levels other than
	 * 1, eg "Gilded" is always level 1;
	 * there is no "Gilded II" (still counts as level 1).
	 * Note that this enchant's total level for all the player's items may still
	 * be > 1, from multiple items having this enchant at level 1.
	*/
	default boolean isMultiLevel() {
		return true;
	}

	/* TODO
	 * There are no negative level custom enchants.
	 * This method is here for BaseAbilityEnchant,
	 * which can calculate & return negative levels in getItemLevel().
	 * Perhaps make ability "enchants" true BaseAttributes that really follow
	 * their "When in Off Hand" etc lore, rather than having fixed valid slots in
	 * the code like custom enchants do?
	 * Could BaseEnchant and BaseAttribute extend a same ItemProperty and call
	 * shared events together in PlayerInventory?
	 */
	/*
	 * By default, this enchant's total level for all the player's items needs
	 * to be > 0 to be considered present.
	 * Return true instead to also allow levels < 0.
	 */
	default boolean canNegativeLevel() {
		return false;
	}

	/*
	 * Returns the level of this enchant for the specified item,
	 * when wielded by the specified Player in the specified ItemSlot.
	 * Returns 0 when the enchant is not considered present on the item.
	 */
	default int getPlayerItemLevel(
		ItemStack itemStack,
		Player player,
		ItemSlot itemSlot
	) {
		if (getValidSlots().contains(itemSlot)) {
			return getItemLevel(itemStack);
		}

		return 0;
	}

	/*
	 * Returns the level of this enchant for the specified item.
	 * No particular Player & ItemSlot is applicable.
	 *
	 * If you have those args,
	 * call getPlayerItemLevel() instead!
	 * Enchants may override that method but not this one with different logic,
	 * such as Radiant only being considered present when the wielding player
	 * is the item's soulbound owner.
	 */
	default int getItemLevel(ItemStack itemStack) {
		return InventoryUtils.getCustomEnchantLevel(
			itemStack,
			getProperty(),
			isMultiLevel()
		);
	}



	/* [Custom Events] */
	/* TODO
	 * Don't call this multiple times when changing levels.
	 * EnchantmentManager seems to call this when changing level for the old
	 * item no longer being counted,
	 * then again if the new item does change level.
	 */
	/*
	 * Called whenever part or all of the player's inventory equipment
	 * gets updates triggered by events, with this enchant involved.
	 * Also called to remove & reapply most custom enchant effects whenever the
	 * player logs on to/switches back to each shard.
	 */
	default void applyProperty(
		Plugin plugin,
		Player player,
		int newLevel
	) {}

	/* TODO
	 * Only call this when this enchantment is actually getting removed;
	 * don't call it if level is simply changing.
	 * Perhaps applyProperty() with old level vs new level?
	 * Then removeProperty() could also take in old level if it needs it.
	 */
	/*
	 * Called whenever part or all of the player's inventory equipment
	 * gets updates triggered by events, with this enchant involved.
	 * Also called to remove & reapply most custom enchant effects whenever the
	 * player logs on to/switches back to each shard,
	 * or when the player stops being tracked by PlayerTracking.
	 */
	default void removeProperty(
		Plugin plugin,
		Player player
	) {}

	/*
	 * Called whenever part or all of the player's inventory equipment
	 * gets updates triggered by events,
	 * regardless of this enchant's involvement.
	 */
	default void onEquipmentUpdate(
		Plugin plugin,
		Player player
	) {}

	/*
	 * When this enchant is considered present,
	 * called every 5 ticks - 4 times a second.
	 * Same rate as class Abilities.
	 */
	default void tick(
		Plugin plugin,
		Player player,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player triggers any evasion enchant via
	 * EvasionInfo.triggerEvasion().
	 *
	 * This is listened for by PlayerListener.
	 */
	default void onEvade(
		Plugin plugin,
		Player player,
		int level,
		EvasionEvent evasionEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player deals custom damage with a valid Spell
	 * (class ability) via EntityUtils.damageEntity().
	 */
	default void onAbility(
		Plugin plugin,
		Player player,
		int level,
		LivingEntity enemy,
		CustomDamageEvent customDamageEvent
	) {}



	/* [Bukkit Events] */
	/*
	 * When this enchant is considered present,
	 * called when the player damages a non-villager LivingEntity,
	 * except if done indirectly via a Projectile.
	 *
	 * onAttack() may be called after this if applicable!
	 */
	default void onDamage(
		Plugin plugin,
		Player player,
		int level,
		LivingEntity enemy,
		EntityDamageByEntityEvent entityDamageByEntityEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player damages a non-villager LivingEntity via a melee
	 * attack (DamageCause.ENTITY_ATTACK).
	 */
	default void onAttack(
		Plugin plugin,
		Player player,
		int level,
		LivingEntity enemy,
		EntityDamageByEntityEvent entityDamageByEntityEvent
	) {}

	/* TODO
	 * Due to both EntityListener & CrossbowListener appearing to call this,
	 * listening for the same event at the same priority,
	 * test if/how many times this method gets called,
	 * especially when using double main + off hand bows together,
	 * or double crossbows that do/don't change their shot arrow with things
	 * like custom Flame
	 */
	/*
	 * When this enchant is considered present,
	 * called when the player launches a Projectile.
	 */
	default void onLaunchProjectile(
		Plugin plugin,
		Player player,
		int level,
		Projectile projectile,
		ProjectileLaunchEvent projectileLaunchEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player kills a LivingEntity that is considered a hostile
	 * enemy (EntityUtils.isHostileMob()).
	 */
	default void onKill(
		Plugin plugin,
		Player player,
		int level,
		Entity enemy,
		EntityDeathEvent entityDeathEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player breaks a block.
	 */
	default void onBlockBreak(
		Plugin plugin,
		Player player,
		BlockBreakEvent blockBreakEvent,
		ItemStack itemStack,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player interacts with an object or air,
	 * potentially once for each hand.
	 *
	 * Event may already be in the cancelled state when called,
	 * if vanilla behaviour is to do nothing, eg interacting with air.
	*/
	default void onPlayerInteract(
		Plugin plugin,
		Player player,
		PlayerInteractEvent playerInteractEvent,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player consumes an item.
	 */
	default void onConsume(
		Plugin plugin,
		Player player,
		PlayerItemConsumeEvent playerItemConsumeEvent,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player regains health.
	 */
	default void onRegain(
		Plugin plugin,
		Player player,
		int level,
		EntityRegainHealthEvent entityRegainHealthEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player is damaged.
	 *
	 * onFatalHurt() may be called after this if applicable!
	 *
	 * onHurtByEntity() may be called if applicable!
	 * These are separately listened for but both are EventPriority.LOW,
	 * so order is not guaranteed.
	 *
	 * This is listened for by EntityListener.
	 */
	default void onHurt(
		Plugin plugin,
		Player player,
		int level,
		EntityDamageEvent entityDamageEvent
	) {}

	/* TODO
	 * Bug #5914, this is called even if damage is blocked by a shield,
	 * causing Evasion etc to misfire.
	 * Look into PlayerInventory#onFatalHurt() logic.
	 *
	 * Related:
	 * Bug #8353, PrismaticShield uses similar logic as PlayerInventory,
	 * it will misfire even if damage is blocked by a shield.
	 */
	/*
	 * When this enchant is considered present,
	 * called when the player takes damage that is going to kill them.
	 *
	 * The entityDamageEvent param can sometimes more precisely be an
	 * EntityDamageByEntityEvent.
	 *
	 * This is listened for by EntityListener.
	 */
	default void onFatalHurt(
		Plugin plugin,
		Player player,
		int level,
		EntityDamageEvent entityDamageEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player is damaged by an Entity.
	 *
	 * onEvade() may be called after this if applicable!
	 *
	 * This is listened for by PlayerListener.
	 */
	default void onHurtByEntity(
		Plugin plugin,
		Player player,
		int level,
		EntityDamageByEntityEvent entityDamageByEntityEvent
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player has died.
	 */
	default void onDeath(
		Plugin plugin,
		Player player,
		PlayerDeathEvent playerDeathEvent,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when one of the player's items takes damage.
	 */
	default void onItemDamage(
		Plugin plugin,
		Player player,
		PlayerItemDamageEvent playerItemDamageEvent,
		int level
	) {}

	/*
	 * When this enchant is considered present,
	 * called when the player's experience changes.
	 */
	default void onExpChange(
		Plugin plugin,
		Player player,
		PlayerExpChangeEvent playerExpChangeEvent,
		int level
	) {}
}
