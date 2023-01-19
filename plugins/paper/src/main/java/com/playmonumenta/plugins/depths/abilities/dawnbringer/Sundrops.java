package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Sundrops extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Sundrops";
	public static final int[] DROP_CHANCE = {20, 25, 30, 35, 40, 60};
	private static final int LINGER_TIME = 10 * 20;
	private static final int DURATION = 8 * 20;
	private static final double PERCENT_SPEED = .2;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "SundropsPercentDamageReceivedEffect";
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;

	public static final DepthsAbilityInfo<Sundrops> INFO =
		new DepthsAbilityInfo<>(Sundrops.class, ABILITY_NAME, Sundrops::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SPAWNER)
			.displayItem(new ItemStack(Material.HONEYCOMB_BLOCK))
			.descriptions(Sundrops::getDescription);

	public Sundrops(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void summonSundrop(Block block) {
		Location loc = block.getLocation().toCenterLocation();
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
			final BlockData mFallingDustData = Material.HONEYCOMB_BLOCK.createBlockData();
			@Override
			public void run() {
				mT++;
				new PartialParticle(Particle.FALLING_DUST, item.getLocation(), 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsOtherPlayerActive();
				//Other player
				for (Player p : PlayerUtils.playersInRange(item.getLocation(), 1.25, true)) {

					Plugin plugin = Plugin.getInstance();
					//Give speed and resistance
					plugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED));
					plugin.mEffectManager.addEffect(p, ABILITY_NAME, new PercentSpeed(DURATION, PERCENT_SPEED, ABILITY_NAME));

					item.remove();

					world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1f);
					new PartialParticle(Particle.BLOCK_CRACK, item.getLocation(), 30, 0.15, 0.15, 0.15, 0.75F, Material.HONEYCOMB_BLOCK.createBlockData()).spawnAsOtherPlayerActive();
					new PartialParticle(Particle.TOTEM, item.getLocation(), 20, 0, 0, 0, 0.35F).spawnAsOtherPlayerActive();

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

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Whenever a player in your party breaks a spawner, there is a ")
			.append(Component.text(DROP_CHANCE[rarity - 1], color))
			.append(Component.text(" chance of spawning a sundrop. Picking up a sundrop gives " + StringUtils.multiplierToPercentage(PERCENT_SPEED) + "% speed and " + StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RECEIVED) + "% resistance for " + DURATION / 20 + " seconds. Spawn chance stacks with other players in your party who have the skill, up to 100%."));
	}

}

