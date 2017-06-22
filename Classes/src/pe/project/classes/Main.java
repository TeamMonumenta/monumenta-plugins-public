package pe.project.classes;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import pe.project.classes.Classes.*;	//	We want all the stuff in this folder
import pe.project.classes.Timers.CooldownTimers;
import pe.project.classes.Timers.ProjectileEffectTimers;
import pe.project.classes.Timers.PulseEffectTimers;
import pe.project.classes.Utils.ScoreboardUtil;

public class Main extends JavaPlugin {
	static Integer tickPerSecond = 20;
	
	public enum Classes {
		NONE(0),
		MAGE(1),
		WARRIOR(2),
		CLERIC(3),
		ROGUE(4),
		ALCHEMIST(5),
		SCOUT(6),
		
		COUNT (6);	//	Please update when new classes are added!
		
		private int value;
		private Classes(int value)	{	this.value = value;	}
		public int getValue()		{	return this.value;	}
	}
	
	public enum Times {
		ONE(1),
		TWO(2),
		FOURTY(40),
		SIXTY(60),
		ONE_TWENTY(120);
		
		private int value;
		private Times(int value)	{	this.value = value;	}
		public int getValue()		{	return this.value;	}
	}
	
	//	This will be our hashmap of player score to Classes
	public HashMap<Integer, BaseClass> mClassMap = new HashMap<Integer, BaseClass>();
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public PulseEffectTimers mPulseEffectTimers = null;
	private Random mRandom = null;
	int mPeriodicTimer = -1;
	
	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new ServerListener(this), this);
		
		//	Initialize Variables.
		mRandom = new Random();
		mTimers = new CooldownTimers(this);
		mPulseEffectTimers = new PulseEffectTimers(this);
		
		World world = Bukkit.getWorlds().get(0);
		mProjectileEffectTimers = new ProjectileEffectTimers(world);
		
		//	Initialize Classes.
		mClassMap.put(Classes.NONE.getValue(), new BaseClass(this, mRandom));
		mClassMap.put(Classes.MAGE.getValue(), new MageClass(this, mRandom));
		mClassMap.put(Classes.WARRIOR.getValue(), new WarriorClass(this, mRandom));
		mClassMap.put(Classes.CLERIC.getValue(), new ClericClass(this, mRandom));
		mClassMap.put(Classes.ROGUE.getValue(), new RogueClass(this, mRandom));
		mClassMap.put(Classes.ALCHEMIST.getValue(), new AlchemistClass(this, mRandom));
		mClassMap.put(Classes.SCOUT.getValue(), new ScoutClass(this, mRandom));
		
		//	Schedule ability cooldowns.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				//	Update cooldowns.
				mTimers.UpdateCooldowns(tickPerSecond);
				mPulseEffectTimers.Update(tickPerSecond);
				
				//	Update periodic timers.
				mPeriodicTimer++;

				for(Player player : getServer().getOnlinePlayers()) {
					BaseClass pClass = Main.this.getClass(player);
					
					boolean two = (mPeriodicTimer % Times.TWO.getValue()) == 0;
					boolean fourty = (mPeriodicTimer % Times.FOURTY.getValue()) == 0;
					boolean sixty = (mPeriodicTimer % Times.SIXTY.getValue()) == 0;
					pClass.PeriodicTrigger(player, two, fourty, sixty, mPeriodicTimer);
				}
				
				mPeriodicTimer %= Times.ONE_TWENTY.getValue();
			}
		}, 0L, tickPerSecond);
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				//	Update cooldowns.
				mProjectileEffectTimers.update();
			}
		}, 0L, 1L);
	}
	
	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}
	
	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}
	
	public BaseClass getClass(Player player) {
		int playerClass = ScoreboardUtil.getScoreboardValue(player, "Class");
		if (playerClass >= 0 && playerClass <= Classes.COUNT.getValue()) {
			return mClassMap.get(playerClass);
		}
		
		//	We Seem to be missing a class.
		return mClassMap.get(Classes.NONE.getValue());
	}
}
