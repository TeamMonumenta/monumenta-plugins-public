package pe.project.timers;

import java.util.List;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import pe.project.Main;
import pe.project.classes.BaseClass;

public class PulseEffectTimers {
	class EffectInfo {
		public EffectInfo(Main plugin, Player player, BaseClass playerClass, int abilityID, String tagName, int cooldown, Location loc, int radius) {
			mPlugin = plugin;
			mOwner = player;
			mClass = playerClass;
			mAbilityID = abilityID;
			mTagName = tagName;
			mCooldown = cooldown;
			mLocation = loc;
			mRadius = radius;
			
			World world = Bukkit.getWorld(player.getWorld().getName());
			mMarkerEntity = world.spawnEntity(loc, EntityType.ARMOR_STAND);
			ArmorStand armorStand = (ArmorStand)mMarkerEntity;
			armorStand.setInvulnerable(true);
			armorStand.setMarker(true);
			armorStand.setVisible(false);
			armorStand.setGravity(false);
			
			Block block = mLocation.getBlock();
			block.setType(Material.STANDING_BANNER);
			
			if (block.getState() instanceof Banner) {
				Banner banner = (Banner)block.getState();
				banner.setBaseColor(DyeColor.CYAN);
				
				banner.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRAIGHT_CROSS));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.CIRCLE_MIDDLE));
				banner.addPattern(new Pattern(DyeColor.BLACK, PatternType.FLOWER));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM));
				banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP));
				
				banner.update();
			}
		}
		
		public boolean Update(int tickPerSecond) {
			mCooldown -= tickPerSecond;
			
			if (mCooldown > 0) {
				List<Entity> entities = mMarkerEntity.getNearbyEntities(mRadius, mRadius, mRadius);
				for (Entity e : entities) {
					if (e instanceof Player) {
						Player player = (Player)e;
						mClass.PulseEffectApplyEffect(mOwner, mLocation, player, mAbilityID);
						player.setMetadata(mTagName, new FixedMetadataValue(mPlugin, 0));
						mPreviouslyEffected.add(player);
					}
				}
			} else {
				return true;
			}
			
			return false;
		}
		
		public void Cleanup() {
			for (int i = 0; i < mPreviouslyEffected.size(); i++) {
				Player player = mPreviouslyEffected.get(i);
				player.removeMetadata(mTagName, mPlugin);
				mClass.PulseEffectRemoveEffect(mOwner, mLocation, player, mAbilityID);
			}
			
			mPreviouslyEffected.clear();
		}
		
		public void Remove() {
			mMarkerEntity.remove();
			mLocation.getBlock().setType(Material.AIR);
		}
		
		public Entity getMarkerEntity() {
			return mMarkerEntity;
		}
		
		Main mPlugin;
		Player mOwner;
		Entity mMarkerEntity;
		BaseClass mClass;
		int mAbilityID;
		String mTagName;
		int mCooldown;
		Location mLocation;
		int mRadius;
		
		Vector<Player> mPreviouslyEffected = new Vector<Player>();
	}	
	
	private Vector<EffectInfo> mPulseEffects = null;
	private Main mPlugin = null;
	
	public PulseEffectTimers(Main plugin) {
		mPulseEffects = new Vector<>();
		mPlugin = plugin;
	}
	
	public void AddPulseEffect(Player player, BaseClass playerClass, int abilityID, String tagName, int cooldown, Location loc, int radius) {
		EffectInfo info = new EffectInfo(mPlugin, player, playerClass, abilityID, tagName, cooldown, loc, radius);
		mPulseEffects.add(info);
	}
	
	public void Update(int tickPerSecond) {
		int size = mPulseEffects.size();
		for (int i = 0; i < size; i++) {
			EffectInfo info = mPulseEffects.get(i);
			
			//	Loop through and remove the effects from previously effected players.
			info.Cleanup();
			
			//	Loop through and apply the effects to the new people within range.
			boolean remove = info.Update(tickPerSecond);
			if (remove) {
				info.Remove();
				mPulseEffects.remove(i);
				
				i--;
				size--;
			}
		}
	}
}
