package com.playmonumenta.plugins.enchantments.cosmetic;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DivineAura implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Divine Aura";
	private static final String TAG_TO_DISABLE = "NoDivineAura";
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();
	private static int STATIC_TICKS = 0;

	// A little easter egg for my friends.

	//UUIDS
	private UUID mNeodymeowm = UUID.fromString("58be4bdf-c21d-402e-81db-93de5a89e8af");
	public String[] mNeoLines = new String[] {
		"Thank you, my friends...",
		"You're Neo... The youngling who brings innocence to them...",
		"Waaaaaaaaaaaaaaaaaaaaarrrrrrrrrrrr...",
		"I will destroy you... wuv u too bud...",
		"What a dorl...",
		":WeaponryNeoHuggle:..."
	};
	private UUID mCorpe = UUID.fromString("391a5ea1-1145-4df1-b8a2-ca23f36ddb9f");
	public String[] mCorpeLines = new String[] {
		"Thank you, my friends...",
		"Corpe, right...? Your comedic craziness is more loved by the group than you think...",
		"GANG GANG...",
		"If you say no u one more time *chews food* I'ma succ u...",
		"TR1 vid wen... It's in the makings...",
		"I save all my females... underground... for later use..."
	};
	private UUID mSpy21dd = UUID.fromString("13ac1c8f-bdf4-4d8b-ba62-66ade197d031");
	public String[] mSpyLines = new String[] {
		"Thank you, my friends...",
		"You're Spy... A true memelord and friend, you are to them...",
		"WELCOME TO THE RICE FIELDS...!!!!!!",
		"*Anyone posts something weird* :DeletThis:...",
		"Shuriken Toss when... That's a good question...",
		"Your memes are trash. Please consider the following: Delete Account..."
	};
	private UUID mRedvam = UUID.fromString("ddeb7bca-36f1-46b0-a2eb-2fac0851e238");
	public String[] mRedLines = new String[] {
		"Thank you, my friends...",
		"You must be Red... They appreciate you being with them, you know...?",
		"Firebutt...!",
		"You're such a cutie... Gosh damnit I'm not...!",
		"If I'm gonna die I'm gonna die on a nice cold day.",
		"Hehe... \nFire: U-Uh, Corpe help me out here..."
	};

	private UUID mFire = UUID.fromString("b2ace1aa-4ecd-4f15-a4df-c95801db21f7");
	public String[] mFireLines = new String[] {
		"Thank you, my friends...",
		"Fire, huh...? And why are you making lines for yourself in the code...?",
		"*Sharply inhales* BA-...",
		"God I'm so hungry I could eat Corpe rn...",
		"Time to beat some sabers...",
		"*Insert TR1 completion screaming here*"
	};

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	/* Helper function to make code easier to read
	 *
	 * When player has the TAG_TO_DISABLE tag, level is > 1000 to indicate disabled
	 * Otherwise player will have 1-999 to indicate how many items they have with the ability
	 *
	 * Player needs to always have at least 1 level if they have a divine aura item regardless of
	 * whether it is active or not to have the onPlayerInteract() run when they right click
	 */
	private static boolean isActive(int level) {
		if (level > 0 && level < 1000) {
			return true;
		} else {
			return false;
		}
	}

	//TODO use PlayerData like with other enchants under WIP conversion
	// If MonumentaItem system wants accurate levels, this part will need updating to return actual level of the item
	// (and would then support the toggle instantly taking effect without waiting on level recalculation).
	@Override
	public int getPlayerItemLevel(ItemStack itemStack, Player player, ItemSlot itemSlot) {
		if (player.getScoreboardTags().contains(TAG_TO_DISABLE)) {
			return 1000;
		}
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player.getUniqueId());
		} else {
			NO_SELF_PARTICLES.remove(player.getUniqueId());
		}
		return BaseEnchantment.super.getPlayerItemLevel(itemStack, player, itemSlot);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (InventoryUtils.testForItemWithLore(item, "To my friends,") && player.getCooldown(item.getType()) <= 0) {
				World world = player.getWorld();

				if (isActive(level)) {
					world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
					world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
					world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0.25f);
					player.sendMessage(ChatColor.AQUA + "You feel the Divine Aura around you fall dormant...");
					player.addScoreboardTag(TAG_TO_DISABLE);
				} else {
					world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
					world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
					world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1.25f);
					player.sendMessage(ChatColor.AQUA + "You feel a Divine Aura envelop you.");
					if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
						player.sendMessage(ChatColor.GRAY + "Note: You have self-particles disabled");
					}
					player.removeScoreboardTag(TAG_TO_DISABLE);
				}
				player.setCooldown(item.getType(), 20);
				InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, null);
			}
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		if (isActive(level)) {
			STATIC_TICKS += 5;
			if (STATIC_TICKS >= 20 * 300) {
				STATIC_TICKS = 0;

				UUID uuid = player.getUniqueId();
				if (uuid.equals(mNeodymeowm)) {
					String m = mNeoLines[FastUtils.RANDOM.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(mCorpe)) {
					String m = mCorpeLines[FastUtils.RANDOM.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(mSpy21dd)) {
					String m = mSpyLines[FastUtils.RANDOM.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(mRedvam)) {
					String m = mRedLines[FastUtils.RANDOM.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(mFire)) {
					String m = mFireLines[FastUtils.RANDOM.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				}
			}
			final Location loc = player.getLocation().add(0, 1, 0);
			if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
				for (Player other : PlayerUtils.otherPlayersInRange(player, 30, true)) {
					other.spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.4, 0.4, 0.4, 0);
				}
			} else {
				player.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.4, 0.4, 0.4, 0);
			}
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (isActive(level)) {
			World world = target.getWorld();
			world.spawnParticle(Particle.SPELL_INSTANT, target.getLocation().add(0, target.getHeight() / 2, 0), 6, target.getWidth(), target.getHeight() / 2, target.getWidth(), 1);
			world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, target.getHeight() / 2, 0), 4, 0, 0, 0, 0.15);
		}
	}
}
