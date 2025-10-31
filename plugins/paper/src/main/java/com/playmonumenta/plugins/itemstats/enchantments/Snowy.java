package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import de.tr7zw.nbtapi.NBT;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Snowy implements Enchantment {
	private static final SnowballMode[] MODES = new SnowballMode[]{
		new SnowballMode(0, "NORMAL", 0.75, 0.25, 1.5f, 6f),
		new SnowballMode(1, "EMPOWERED", 3, 0.5, 1f, 2f)
	};
	private static final String SNOWBALL_MODE = "SnowballMode";
	private static final int SWAP_COOLDOWN = 20;
	private static final int GLOW_NEARBY_RADIUS = 20;
	private static final int GLOW_DURATION = 20 * 10;

	private final Map<UUID, Integer> mSwapCooldowns;
	private final Map<UUID, Integer> mGlowCooldowns;

	public Snowy() {
		this.mSwapCooldowns = new HashMap<>();
		this.mGlowCooldowns = new HashMap<>();
	}

	@Override
	public String getName() {
		return "Snowy";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SNOWY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	// throw rate has priority of 1000, and it creates a new snowball
	// must add metadata of mode before creating a new snowball since projectileLaunch does not catch the new one
	@Override
	public double getPriorityAmount() {
		return 999;
	}

	// projectile hit event is caught in the WinterListener class
	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		ItemStack snowball = player.getInventory().getItemInMainHand();
		if (!snowball.isEmpty()) {
			if (use(snowball, player)) {
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.75f, 2f);
				SnowballMode mode = getMode(snowball);
				// store mode of snowball in projectile
				MetadataUtils.setMetadata(projectile, SNOWBALL_MODE, mode.mIndex);
			} else {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		if (event.getAction().isLeftClick() && event.getHand() == EquipmentSlot.HAND && player.isSneaking()) {
			// 10 second glow cooldown
			UUID uuid = player.getUniqueId();
			int currentCooldown = mGlowCooldowns.getOrDefault(uuid, 0);
			int currentTick = Bukkit.getCurrentTick();
			if (currentCooldown < currentTick) {
				event.setCancelled(true);
				player.getLocation().getNearbyPlayers(GLOW_NEARBY_RADIUS).stream()
					.filter(p -> !p.equals(player))
					.filter(Snowy::hasEnchantInInventory)
					.forEach(p -> GlowingManager.startGlowing(p, NamedTextColor.AQUA, GLOW_DURATION, GlowingManager.PLAYER_ABILITY_PRIORITY, player::equals, null));
				player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.4f);
				mGlowCooldowns.put(uuid, currentTick + GLOW_DURATION);
			}
		}
	}

	@Override
	public void onPlayerSwapHands(Plugin plugin, Player player, double value, PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);
		ItemStack snowball = player.getInventory().getItemInMainHand();
		if (!snowball.isEmpty() && ItemStatUtils.hasEnchantment(snowball, EnchantmentType.SNOWY)) {
			// 1 second swap cooldown
			UUID uuid = player.getUniqueId();
			int currentCooldown = mSwapCooldowns.getOrDefault(uuid, 0);
			int currentTick = Bukkit.getCurrentTick();
			if (currentCooldown < currentTick) {
				toggleMode(plugin, snowball, player);
				mSwapCooldowns.put(uuid, currentTick + SWAP_COOLDOWN);
			}
		}
	}

	public static boolean hasEnchantInInventory(Player player) {
		return Arrays.stream(player.getInventory().getContents()).anyMatch(is -> ItemStatUtils.hasEnchantment(is, EnchantmentType.SNOWY));
	}

	public static Optional<Integer> getProjectileMode(Projectile projectile) {
		return MetadataUtils.getMetadata(projectile, SNOWBALL_MODE);
	}

	public static void transferProjectileMode(Projectile oldProjectile, Projectile newProjectile) {
		if (oldProjectile.hasMetadata(SNOWBALL_MODE)) {
			Optional<Integer> mode = getProjectileMode(oldProjectile);
			mode.ifPresent(val -> MetadataUtils.setMetadata(newProjectile, SNOWBALL_MODE, val));
		}
	}

	private static SnowballMode getMode(ItemStack snowball) {
		int index = NBT.get(snowball, nbt -> {
			return nbt.getOrDefault(SNOWBALL_MODE, 0);
		});
		return getMode(index);
	}

	public static SnowballMode getMode(int index) {
		return MODES[index % MODES.length];
	}

	private void toggleMode(Plugin plugin, ItemStack snowball, Player player) {
		NBT.modify(snowball, nbt -> {
			int mode = nbt.getOrDefault(SNOWBALL_MODE, 0);
			nbt.setInteger(SNOWBALL_MODE, (mode + 1) % MODES.length);
		});
		SnowballMode mode = getMode(snowball);
		player.sendActionBar(Component.text("Switched to " + mode.mName + " mode!", NamedTextColor.BLUE));
		player.playSound(player, Sound.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.75f, mode.mPitch);
		// refresh throw rate
		ItemStatUtils.addAttribute(snowball, AttributeType.THROW_RATE, mode.mThrowRate, Operation.ADD, Slot.MAINHAND);
		ItemUpdateHelper.generateItemStats(snowball);
		plugin.mItemStatManager.updateStats(player);
	}

	private boolean use(ItemStack snowball, Player player) {
		if (snowball.getAmount() > 1) {
			player.sendActionBar(Component.text("Cannot shoot! Unstack the snowballs!", NamedTextColor.RED));
			return false;
		}
		// With no ammo system, the only check needed is for stacked items.
		return true;
	}

	public static class SnowballMode {
		private final int mIndex;
		private final String mName;
		private final double mPower;
		private final double mYPower;
		private final float mPitch;
		private final double mThrowRate;

		SnowballMode(int index, String name, double power, double yPower, float pitch, double throwRate) {
			mIndex = index;
			mName = name;
			mPower = power;
			mYPower = yPower;
			mPitch = pitch;
			mThrowRate = throwRate;
		}

		public void applyVelocity(Player target, Vector originalVelocity) {
			Vector velocity = originalVelocity.normalize().multiply(mPower);
			velocity.setY(0);
			if (PlayerUtils.isOnGround(target)) {
				velocity.setY(mYPower);
			}
			target.setVelocity(velocity);
		}
	}
}
