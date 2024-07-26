package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.IndependentIframeTracker;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LightningBottle extends DepthsAbility {
	public static final String ABILITY_NAME = "Lightning Bottle";
	public static final String POTION_NAME_BASE = ABILITY_NAME + " [%d]";
	public static final double[] DAMAGE = {6, 7.5, 9, 10.5, 12, 15};
	public static final double[] VULNERABILITY = {0.1, 0.13, 0.16, 0.19, 0.22, 0.28};
	public static final double SLOWNESS = 0.2;
	public static final int MAX_STACK = 12;
	public static final int KILLS_PER = 4;
	public static final int BOTTLES_GIVEN = 2;
	public static final int DURATION = 3 * 20;
	public static final int DEATH_RADIUS = 32;
	public static final double RADIUS = 4;
	private static final int IFRAME_BETWEEN_POT = 10;

	public static final DepthsAbilityInfo<LightningBottle> INFO =
		new DepthsAbilityInfo<>(LightningBottle.class, ABILITY_NAME, LightningBottle::new, DepthsTree.DAWNBRINGER, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.LIGHTNING_BOTTLE)
			.displayItem(Material.BREWING_STAND)
			.descriptions(LightningBottle::getDescription);

	private final int mKillsPer;
	private final int mBottlesGiven;
	private final double mDeathRadius;
	private final int mMaxStack;
	private final double mSlow;
	private final double mVuln;
	private final double mDamage;
	private final int mDuration;
	private final double mRadius;
	private final IndependentIframeTracker mIframeTracker;

	private final WeakHashMap<ThrownPotion, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;
	private int mCount = 0;
	private @Nullable ProjectileHitEvent mLastHitEvent;

	public LightningBottle(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mIframeTracker = new IndependentIframeTracker(IFRAME_BETWEEN_POT);

		mPlayerItemStatsMap = new WeakHashMap<>();
		mKillsPer = KILLS_PER + (int) CharmManager.getLevel(mPlayer, CharmEffects.LIGHTNING_BOTTLE_KILLS_PER_BOTTLE.mEffectName);
		mBottlesGiven = BOTTLES_GIVEN;
		mDeathRadius = DEATH_RADIUS;
		mMaxStack = MAX_STACK + (int) CharmManager.getLevel(mPlayer, CharmEffects.LIGHTNING_BOTTLE_MAX_STACKS.mEffectName);
		mSlow = SLOWNESS + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.LIGHTNING_BOTTLE_SLOW_AMPLIFIER.mEffectName);
		mVuln = VULNERABILITY[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.LIGHTNING_BOTTLE_VULN_AMPLIFIER.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.LIGHTNING_BOTTLE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.LIGHTNING_BOTTLE_DURATION.mEffectName, DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.LIGHTNING_BOTTLE_RADIUS.mEffectName, RADIUS);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion, ProjectileLaunchEvent event) {
		int amt;
		final var item = potion.getItem();
		if (isLightningBottle(item)) {
			event.setCancelled(true); // regardless, we don't allow the normal event to go through.
		}
		if (0 != (amt = getLightningBottleAmount(item))) {
			// we copy the alchemist bag mech here

			ThrownPotion potionClone = (ThrownPotion) potion.getWorld().spawnEntity(potion.getLocation(), EntityType.SPLASH_POTION);
			ItemStack newPotion = item.clone();
			ItemUtils.setPlainTag(newPotion);
			potionClone.setItem(newPotion);
			potionClone.setShooter(mPlayer);
			potionClone.setVelocity(potion.getVelocity());
			event.setCancelled(true);
			mPlugin.mProjectileEffectTimers.addEntity(potionClone, Particle.SPELL);
			mPlayerItemStatsMap.put(potionClone, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

			// Trying to use setLightningBottleAmount(potion.getItem(), amt - 1); for some reason doesn't work by the way.
			final var contents = mPlayer.getInventory().getContents(); // here we iterate through the hotbar to find the lightning bottle(s) and update them.
			for (int i = 0; i < 9; i++) {
				final var itemstack = contents[i];
				if (null != itemstack && isLightningBottle(itemstack)) {
					setLightningBottleAmount(itemstack, amt - 1);
				}
			}

			mPlayer.updateInventory();

		}
		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		// Prevent the potion from splashing on players, summons, and other non-hostile mobs
		// Most importantly, this prevents the potion from instantly splashing on the throwing player with certain combinations of projectile speed and throw direction.
		// (same code as alchemist's potions)
		if (proj instanceof ThrownPotion potion
			&& mPlayerItemStatsMap.containsKey(potion)
			&& event.getHitEntity() != null) {
			if (!EntityUtils.isHostileMob(event.getHitEntity())) {
				event.setCancelled(true);
			}
		}
		mLastHitEvent = event;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(potion);

		if (playerItemStats != null) {
			// Get the real splash location of the potion, which is ahead of where it currently is.
			// While a PotionSplashEvent is also a ProjectileHitEvent, it does not have all the data of that event, so cannot be relied upon.
			// Thus, we store the last ProjectileHitEvent and use that if possible.
			Location loc = mLastHitEvent != null && mLastHitEvent.getEntity() == potion ? EntityUtils.getProjectileHitLocation(mLastHitEvent) : potion.getLocation();

			// Sometimes, the potion just randomly splashes several blocks away from where it actually lands. Force it to splash at the correct location.
			potion.teleport(loc.clone().add(0, 0.25, 0));
			potion.setVelocity(new Vector(0, 0, 0));

			Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
			for (LivingEntity entity : hitbox.getHitMobs()) {
				mIframeTracker.damage(entity, () -> DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, true, false));

				EntityUtils.applyVulnerability(mPlugin, mDuration, mVuln, entity);
				EntityUtils.applySlow(mPlugin, mDuration, mSlow, entity);
			}
		}

		return true;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mCount++;
		if (mCount >= mKillsPer) {
			mCount = 0;
			Inventory inv = mPlayer.getInventory();
			boolean flag = true;
			for (ItemStack item : inv.getContents()) {
				if (item != null && isLightningBottle(item)) { // if the player has a bottle.
					int amt;
					if ((amt = getLightningBottleAmount(item)) >= mMaxStack) {
						return; // no need to give any more.
					}
					int newAmount;
					setLightningBottleAmount(item, newAmount = Math.min(mMaxStack, amt + mBottlesGiven)); // give it a new amount.
					mPlayer.sendActionBar(Component.text("+%d Lightning Bottle Charges [%d/%d]!".formatted(newAmount - amt, newAmount, mMaxStack), NamedTextColor.GOLD)); // inform the player
					flag = false;
					break;
				}
			}
			// otherwise give them a new one.
			if (flag) {
				ItemStack newPotions = getBaseLightningBottle();
				setLightningBottleAmount(newPotions, mBottlesGiven);
				inv.addItem(newPotions);
				mPlayer.sendActionBar(Component.text("You have received a Lightning Bottle!", NamedTextColor.GOLD));
			}
			mPlayer.updateInventory();
		}
	}

	@Override
	public double entityDeathRadius() {
		return mDeathRadius;
	}


	final Style STYLE = Style.style().color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).build();
	public ItemStack getBaseLightningBottle() {
		ItemStack itemStack = new ItemStack(Material.SPLASH_POTION, 1);
		PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

		potionMeta.setBasePotionData(new PotionData(PotionType.MUNDANE));
		potionMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS); // Hide "No Effects" vanilla potion effect lore
		potionMeta.setColor(Color.YELLOW);
		potionMeta.lore(List.of(Component.text("A unique potion used by Dawnbringers.", STYLE),
			Component.text("Taking this item outside of the dungeon", STYLE),
			Component.text("will instead drop it in the dungeon.", STYLE)));
		itemStack.setItemMeta(potionMeta);
		setLightningBottleAmount(itemStack, 1);

		ItemUtils.setPlainTag(itemStack); // Support for resource pack textures like with other items & mechanisms
		return itemStack;
	}

	/*
	 * Also updates the displayName.
	 * Sets the amount contained in a lightning bottle.
	 */
	private void setLightningBottleAmount(ItemStack item, int amount) {
		final var itemMeta = Objects.requireNonNull(item.getItemMeta()); // this really isn't needed; it's to appease the dog. This is never null since Lightning Bottles always have ItemMeta.

		itemMeta.displayName(DepthsTree.DAWNBRINGER.color(POTION_NAME_BASE.formatted(amount)).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(itemMeta);
		item.setAmount(Math.max(1, amount));
		NBT.modify(item, nbt -> {
			nbt.setInteger("bottle_amount", amount);
		});
	}



	private int getLightningBottleAmount(ItemStack item) {
		return NBT.get(item, (ReadableItemNBT n) -> n.getInteger("bottle_amount"));
	}


	public static boolean isLightningBottle(ItemStack item) {
		return item.getType() == Material.SPLASH_POTION && ItemUtils.hasPlainName(item) && ItemUtils.getPlainName(item).contains(ABILITY_NAME);
	}

	private static Description<LightningBottle> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<LightningBottle>(color)
			.add("For every ")
			.add(a -> a.mKillsPer, KILLS_PER, true)
			.add(" mobs that die within ")
			.add(a -> a.mDeathRadius, DEATH_RADIUS)
			.add(" blocks of you, gain ")
			.add(a -> a.mBottlesGiven, BOTTLES_GIVEN)
			.add(" lightning bottles, which stack up to ")
			.add(a -> a.mMaxStack, MAX_STACK)
			.add(". Throwing a lightning bottle deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and applies ")
			.addPercent(a -> a.mSlow, SLOWNESS)
			.add(" slowness and ")
			.addPercent(a -> a.mVuln, VULNERABILITY[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.");
	}

}
