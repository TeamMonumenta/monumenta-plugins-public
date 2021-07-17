package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class Sundrops extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Sundrops";
	public static final double[] DROP_CHANCE = {20, 25, 30, 35, 40};
	private static final int LINGER_TIME = 10 * 20;
	private static final int DURATION = 8 * 20;
	private static final double PERCENT_SPEED = .2;

	public Sundrops(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.HONEYCOMB_BLOCK;
		mTree = DepthsTree.SUNLIGHT;
	}

	public static void summonSundrop(Location loc) {
		World world = loc.getWorld();
		ItemStack itemStack = new ItemStack(Material.HONEYCOMB_BLOCK);
		ItemUtils.setPlainName(itemStack, "Sundrop");
		ItemMeta sundropMeta = itemStack.getItemMeta();
		sundropMeta.displayName(Component.text("Sundrop", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		itemStack.setItemMeta(sundropMeta);
		Item item = world.dropItemNaturally(loc, itemStack);
		item.setGlowing(true);
		item.setPickupDelay(Integer.MAX_VALUE);

		new BukkitRunnable() {
			int mT = 0;
			BlockData mFallingDustData = Material.HONEYCOMB_BLOCK.createBlockData();
			@Override
			public void run() {
				mT++;
				world.spawnParticle(Particle.FALLING_DUST, item.getLocation(), 1, 0.2, 0.2, 0.2, mFallingDustData);
				//Other player
				for (Player p : PlayerUtils.playersInRange(item.getLocation(), 1.25, true)) {

					//Give speed and resistance
					Plugin.getInstance().mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
						new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION, 0, true, true));
					Plugin.getInstance().mEffectManager.addEffect(p, ABILITY_NAME, new PercentSpeed(DURATION, PERCENT_SPEED, ABILITY_NAME));

					item.remove();

					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
					world.spawnParticle(Particle.BLOCK_CRACK, item.getLocation(), 30, 0.15, 0.15, 0.15, 0.75F, Material.HONEYCOMB_BLOCK.createBlockData());
					world.spawnParticle(Particle.TOTEM, item.getLocation(), 20, 0, 0, 0, 0.35F);

					this.cancel();
					break;
				}
				if (mT >= LINGER_TIME || item.isDead()) {
					this.cancel();
					item.remove();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}


	@Override
	public String getDescription(int rarity) {
		return "Whenever a player in your party breaks a spawner, there is a " + DepthsUtils.getRarityColor(rarity) + DROP_CHANCE[rarity - 1] + "%" + ChatColor.WHITE + " chance of spawning a sundrop. Picking up a sundrop gives " + DepthsUtils.roundPercent(PERCENT_SPEED) + "% speed and Resistance I for " + DURATION / 20 + " seconds. Spawn chance stacks with other players in your party who have the skill, up to 100%.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SPAWNER;
	}
}

