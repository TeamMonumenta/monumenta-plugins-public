package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRiptideEvent;

public interface ItemStat {

	/**
	 * The plain text name of the ItemStat. Will be given standardized formatting when displayed on items.
	 *
	 * @return the name of the ItemStat
	 */
	String getName();

	/**
	 * Priority order in event handling, with lower values being handled earlier than higher ones.
	 * <p>
	 * Some references:
	 * <ul>
	 * <li>Stats that must run first are around -10000
	 * <li>Most item stats with a very specific order are around 0-100
	 * <li>Default is 1000
	 * <li>Items needing final damage dealt/received are around 5000
	 * <li>Resurrection and related are around 10000
	 * </ul>
	 *
	 * @return the priority order
	 */
	default double getPriorityAmount() {
		return 1000;
	}

	/**
	 * Runs action every 5 ticks.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player running the action
	 * @param value  the value of ItemStat possessed by the Player
	 * @param twoHz  true every 10 ticks
	 * @param oneHz  true every 20 ticks
	 */
	default void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {

	}

	/**
	 * Player dealt damage (including Projectiles) to a LivingEntity.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player damaging the LivingEntity
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated DamageEvent
	 * @param enemy  the LivingEntity being damaged
	 */
	default void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {

	}

	/**
	 * Player received damage.
	 *
	 * @param plugin  monumenta plugin
	 * @param player  the Player receiving damage
	 * @param value   the value of ItemStat possessed by the Player
	 * @param event   the associated DamageEvent
	 * @param damager the Entity directly dealing damage
	 * @param source  the LivingEntity directly or indirectly dealing damage
	 */
	default void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {

	}

	/**
	 * Player received damage which was fatal.
	 * <br>
	 * This is called in addition to onHurt() and related methods.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player receiving damage
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated DamageEvent
	 */
	default void onHurtFatal(Plugin plugin, Player player, double value, DamageEvent event) {

	}

	// TODO: convert everything in CrossbowListener over to the custom enchants system

	/**
	 * Player launched a Projectile (e.g. arrow, potion).
	 *
	 * @param plugin     monumenta plugin
	 * @param player     the Player launching Projectile
	 * @param value      the value of ItemStat possessed by the Player
	 * @param event      the associated ProjectileLaunchEvent
	 * @param projectile the Projectile being launched
	 */
	default void onLaunchProjectile(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {

	}

	/**
	 * Player Projectile hit something.
	 *
	 * @param plugin     monumenta plugin
	 * @param player     the Player launching Projectile
	 * @param value      the value of ItemStat possessed by the Player
	 * @param event      the associated ProjectileLaunchEvent
	 * @param projectile the Projectile being launched
	 */
	default void onProjectileHit(Plugin plugin, Player player, double value, ProjectileHitEvent event, Projectile projectile) {

	}

	/**
	 * Player loads a Crossbow (e.g. arrow, fireworks)
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player loading projectile
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated EntityLoadCrossbowEvent
	 */
	default void onLoadCrossbow(Plugin plugin, Player player, double value, EntityLoadCrossbowEvent event) {

	}

	/**
	 * Player killed a hostile LivingEntity.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player killing LivingEntity
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated EntityDeathEvent
	 * @param enemy  the LivingEntity being killed
	 */
	default void onKill(Plugin plugin, Player player, double value, EntityDeathEvent event, LivingEntity enemy) {

	}

	/**
	 * Player broke a block.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player breaking a block
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated BlockBreakEvent
	 */
	default void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {

	}

	/**
	 * Player interacted with an object or air.
	 * <br>
	 * May be called twice, once for each hand. Event may start in cancelled state if the vanilla behavior is to do nothing (e.g. interacting with air).
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player interacting
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerInteractEvent
	 */
	default void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {

	}

	/**
	 * Player consumed food item or potion.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player consuming
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerItemConsumeEvent
	 */
	default void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {

	}

	/**
	 * Player regained health.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player regaining health
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated EntityRegainHealthEvent
	 */
	default void onRegain(Plugin plugin, Player player, double value, EntityRegainHealthEvent event) {

	}

	/**
	 * Player took durability damage.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player taking durability damage
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerItemDamageEvent
	 */
	default void onItemDamage(Plugin plugin, Player player, double value, PlayerItemDamageEvent event) {

	}

	/**
	 * Player gained or lost experience.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player changing experience
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerExpChangeEvent
	 */
	default void onExpChange(Plugin plugin, Player player, double value, PlayerExpChangeEvent event) {

	}

	/**
	 * Called whenever part or all of the player's inventory equipment
	 * gets updates triggered by events,
	 * regardless of this enchant's involvement.
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player changing equipment
	 */
	default void onEquipmentUpdate(Plugin plugin, Player player) {

	}

	/**
	 * Called whenever the player dies a non-safe death
	 * i.e. a grave was formed and exp was dropped
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player who died
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerDeathEvent
	 */
	default void onDeath(Plugin plugin, Player player, double value, PlayerDeathEvent event) {

	}

	/**
	 * Called whenever an item spawns or is loaded into an existing chunk.
	 *
	 * @param plugin monumenta plugin
	 * @param item   the Item spawned
	 * @param value  the value of ItemStat possessed by the Item
	 */
	default void onSpawn(Plugin plugin, Item item, double value) {

	}

	/**
	 * Called when an entity is ignited
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player changing experience
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerExpChangeEvent
	 */
	default void onCombust(Plugin plugin, Player player, double value, EntityCombustEvent event) {

	}

	/**
	 * Called when a player riptides (hopefully???)
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player riptiding
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PlayerRiptdieEvent
	 */
	default void onRiptide(Plugin plugin, Player player, double value, PlayerRiptideEvent event) {

	}

	/**
	 * Called when a player is splashed by a potion
	 *
	 * @param plugin monumenta plugin
	 * @param player the Player being splashed
	 * @param value  the value of ItemStat possessed by the Player
	 * @param event  the associated PotionSplashEvent
	 */
	default void onPlayerPotionSplash(Plugin plugin, Player player, double value, PotionSplashEvent event) {

	}
}
