package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
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
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DivineAura implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Divine Aura";
	private static final String TAG_TO_DISABLE = "NoDivineAura";
	private static final Random rand = new Random();
	/* This is shared by all instances */
	private static int STATIC_TICKS = 0;
	private static final Set<Player> NO_SELF_PARTICLES = new HashSet<Player>();

	// A little easter egg for my friends.

	//UUIDS
	private UUID neodymeowm = UUID.fromString("58be4bdf-c21d-402e-81db-93de5a89e8af");
	public String[] neo_lines = new String[] {
		"Thank you, my friends...",
		"You're Neo... The youngling who brings innocence to them...",
		"Waaaaaaaaaaaaaaaaaaaaarrrrrrrrrrrr...",
		"I will destroy you... wuv u too bud...",
		"What a dorl...",
		":WeaponryNeoHuggle:..."
	};
	private UUID corpe_ = UUID.fromString("391a5ea1-1145-4df1-b8a2-ca23f36ddb9f");
	public String[] corpe_lines = new String[] {
		"Thank you, my friends...",
		"Corpe, right...? Your comedic craziness is more loved by the group than you think...",
		"GANG GANG...",
		"If you say no u one more time *chews food* I'ma succ u...",
		"TR1 vid wen... It's in the makings...",
		"I save all my females... underground... for later use..."
	};
	private UUID spy21dd = UUID.fromString("13ac1c8f-bdf4-4d8b-ba62-66ade197d031");
	public String[] spy_lines = new String[] {
		"Thank you, my friends...",
		"You're Spy... A true memelord and friend, you are to them...",
		"WELCOME TO THE RICE FIELDS...!!!!!!",
		"*Anyone posts something weird* :DeletThis:...",
		"Shuriken Toss when... That's a good question...",
		"Your memes are trash. Please consider the following: Delete Account..."
	};
	private UUID redvam = UUID.fromString("ddeb7bca-36f1-46b0-a2eb-2fac0851e238");
	public String[] red_lines = new String[] {
		"Thank you, my friends...",
		"You must be Red... They appreciate you being with them, you know...?",
		"Firebutt...!",
		"You're such a cutie... Gosh damnit I'm not...!",
		"If I'm gonna die I'm gonna die on a nice cold day.",
		"Hehe... \nFire: U-Uh, Corpe help me out here..."
	};

	private UUID fire = UUID.fromString("b2ace1aa-4ecd-4f15-a4df-c95801db21f7");
	public String[] fire_lines = new String[] {
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
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
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

	@Override
	public int getLevelFromItem(ItemStack item, Player player) {
		if (getLevelFromItem(item) == 0) {
			return 0;
		}
		if (player.getScoreboardTags().contains(TAG_TO_DISABLE)) {
			return 1000;
		}
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player);
		} else {
			NO_SELF_PARTICLES.remove(player);
		}
		return 1;
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
					if (NO_SELF_PARTICLES.contains(player)) {
						player.sendMessage(ChatColor.GRAY + "Note: You have self-particles disabled");
					}
					player.removeScoreboardTag(TAG_TO_DISABLE);
				}
				player.setCooldown(item.getType(), 20);
				plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);
			}
		}
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		if (isActive(level)) {
			STATIC_TICKS += 5;
			if (STATIC_TICKS >= 20 * 300) {
				STATIC_TICKS = 0;

				UUID uuid = player.getUniqueId();
				if (uuid.equals(neodymeowm)) {
					String m = neo_lines[rand.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(corpe_)) {
					String m = corpe_lines[rand.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(spy21dd)) {
					String m = spy_lines[rand.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(redvam)) {
					String m = red_lines[rand.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				} else if (uuid.equals(fire)) {
					String m = fire_lines[rand.nextInt(6)];
					player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + m);
				}
			}
			final Location loc = player.getLocation().add(0, 1, 0);
			if (NO_SELF_PARTICLES.contains(player)) {
				for (Player other : PlayerUtils.getNearbyPlayers(player, 30, false)) {
					other.spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.4, 0.4, 0.4, 0);
				}
			} else {
				world.spawnParticle(Particle.SPELL_INSTANT, loc, 5, 0.4, 0.4, 0.4, 0);
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
