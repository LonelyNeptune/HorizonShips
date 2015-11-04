package com.gmail.Rhisereld.HorizonShips;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.regions.RegionOperationException;

@SuppressWarnings("deprecation")
public class Ship 
{	
	ConfigAccessor data;
	Plugin plugin;
	
	HashMap<String, SchematicManager> schemManagers = new HashMap<String, SchematicManager>();
	
	public Ship(ConfigAccessor data, Plugin plugin) 
	{
		this.data = data;
		this.plugin = plugin;
	}

	/**
	 * createShip() creates a new ship to be recorded in the data.yml file. It will have a name, current destination,
	 * a list of destinations (containing only the current destination), and a list of pilots (empty).
	 * It will have a fuel level of zero and will not be broken.
	 * 
	 * @param shipName
	 * @param player
	 * @param destinationName
	 * @throws DataException
	 * @throws IOException
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	public void createShip(String shipName, Player player, String destinationName) throws DataException, IOException, NullPointerException, IllegalArgumentException
	{
		//Check a ship doesn't already exist by that name.
		Set<String> shipNames = data.getConfig().getConfigurationSection("ships.").getKeys(false);
		for (String sh : shipNames)
			if (sh.equalsIgnoreCase(shipName))
				throw new IllegalArgumentException("A ship already exists by that name.");	
				
		SchematicManager sm = new SchematicManager(player.getWorld());
		Selection s = sm.getPlayerSelection(player);
		
		Location min = s.getMinimumPoint();
		Location max = s.getMaximumPoint();
		double length = max.getX() - min.getX();
		double width = max.getZ() - min.getZ();
		double height = max.getY() - min.getY();
	
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".world", min.getWorld().getName());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".x", min.getX());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".y", min.getY());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".z", min.getZ());
		data.getConfig().set("ships." + shipName + ".currentDestination", destinationName);
		data.getConfig().set("ships." + shipName + ".fuel", 0);
		data.getConfig().set("ships." + shipName + ".broken", false);
		data.getConfig().set("ships." + shipName + ".partRequired", null);
		data.getConfig().set("ships." + shipName + ".consumePart", false);
		data.getConfig().set("ships." + shipName + ".pilots", new ArrayList<String>());
		data.getConfig().set("ships." + shipName + ".length", length);
		data.getConfig().set("ships." + shipName + ".width", width);
		data.getConfig().set("ships." + shipName + ".height", height);

		data.saveConfig();
		
		sm.saveSchematic(s, shipName, shipName + "\\ship"); //TODO: Fix how this is called.
	}

	/**
	 * deleteShip() removes all saved information on the given ship.
	 * 
	 * @param sender
	 * @param shipName
	 * @throws IllegalArgumentException
	 */
	public void deleteShip(CommandSender sender, String shipName) throws IllegalArgumentException, IOException
	{
		Set<String> shipNames = data.getConfig().getConfigurationSection("ships.").getKeys(false);
		boolean shipFound = false;
		
		//Check that the ship exists
		for (String s : shipNames)
			if (s.equalsIgnoreCase(shipName))
				shipFound = true;
		if (!shipFound)
			throw new IllegalArgumentException("Ship not found.");
		
		//Check that the person has permission
		if (!sender.hasPermission("horizonships.admin.delete") && sender instanceof Player)
			throw new IllegalArgumentException("You don't have permission to delete that ship.");
		
		//Delete all information on the ship.
		data.getConfig().getConfigurationSection("ships.").set(shipName, null);
		data.saveConfig();
		
		//Delete ship schematic
		File file = new File(plugin.getDataFolder() + "\\schematics\\" + shipName + "\\ship.schematic");
		file.delete();
		
		//Delete all dock schematics
		//TODO: When dock schematics are implemented.
		
		//Delete directory
		file = new File(plugin.getDataFolder() + "\\schematics\\" + shipName);
		file.delete();	
	}

	/**
	 * testDestination() pastes the named ship inside the current WorldEdit selection of the player for visualisation purposes.
	 * Checks that there isn't already a destination by that name.
	 * Checks that the player has permission.
	 * 
	 * @param sm
	 * @param player
	 * @param shipName
	 * @param destinationName
	 * @throws MaxChangedBlocksException
	 * @throws DataException
	 * @throws IOException
	 */
	public void testDestination(Player player, String shipName, String destinationName) throws MaxChangedBlocksException, DataException, IOException
	{
		Selection s;
		player.getWorld();
		new SchematicManager(player.getWorld());
		SchematicManager sm = new SchematicManager(player.getWorld());
		schemManagers.put(player.getName(), sm);

		//Check that the player has permission.
		if (!player.hasPermission("horizonShips.admin.destination"))
			throw new IllegalArgumentException("You don't have permission to create a destination!");
		
		//Check that there isn't already a destination by that name.
		if (data.getConfig().getConfigurationSection("ships." + shipName + ".destinations").getKeys(false).contains(shipName))
			throw new IllegalArgumentException("This ship already has a destination by that name.");
		
		s = sm.getPlayerSelection(player);
		sm.loadSchematic(shipName, s, shipName + "\\ship");
	}
	
	/**
	 * adjustDestination() undoes the previous changes made stored in the SchematicManager object, and pastes the ship
	 * in the new location one block in the direction given.
	 * Checks that the direction is valid.
	 * 
	 * @param sm
	 * @param player
	 * @param direction
	 * @param shipName
	 * @throws MaxChangedBlocksException
	 * @throws DataException
	 * @throws IOException
	 * @throws RegionOperationException
	 * @throws IncompleteRegionException
	 */
	public void adjustDestination(Player player, String direction, String shipName) throws MaxChangedBlocksException, DataException, IOException, RegionOperationException, IncompleteRegionException
	{
		Vector dir;
		SchematicManager sm = schemManagers.get(player.getName());
		Selection s = sm.getPlayerSelection(player);
		
		switch (direction)
		{
			case "north": 	dir = new Vector(0, 0, -1);
							break;
			case "south": 	dir = new Vector(0, 0, 1);
							break;
			case "east": 	dir = new Vector(1, 0, 0);
							break;
			case "west": 	dir = new Vector(-1, 0, 0);
							break;
			case "up": 		dir = new Vector(0, 1, 0);
							break;
			case "down": 	dir = new Vector(0, -1, 0);
							break;
			default:		throw new IllegalArgumentException("That is not a valid direction.");		
		}
		
		//Undo previous paste.
		sm.undoSession();
		
		//Shift 1 block in direction specified.
		sm.shiftSelection(player, s, dir);
		
		//Paste
		sm.loadSchematic(shipName, s, shipName + "\\ship");
	}
	
	/**
	 * cancelDestination() removes the ship previously pasted for visualisation purposes.
	 * 
	 * @param sm
	 */
	public void cancelDestination(Player player)
	{
		SchematicManager sm = schemManagers.get(player.getName());
		sm.undoSession();
	}
	
	/**
	 * addDestination() officially adds the destination by storing its location and name. It also removes the 
	 * ship previously pasted for visualisation purposes.
	 * 
	 * @param sm
	 * @param player
	 * @param shipName
	 * @param destinationName
	 */
	public void addDestination(Player player, String shipName, String destinationName)
	{
		SchematicManager sm = schemManagers.get(player.getName());
		
		//Save all the information about the new destination.
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".world", sm.getPlayerSelection(player).getWorld().getName());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".x", sm.getPlayerSelection(player).getMinimumPoint().getX());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".y", sm.getPlayerSelection(player).getMinimumPoint().getY());
		data.getConfig().set("ships." + shipName + ".destinations." + destinationName + ".z", sm.getPlayerSelection(player).getMinimumPoint().getZ());
		data.saveConfig();
		
		//Undo paste.
		sm.undoSession();
	}
	
	/**
	 * listShips() sends the sender a list of names of all the existing ships.
	 * 
	 * @param sender
	 */
	public void listShips(CommandSender sender)
	{
		Set<String> ships = data.getConfig().getConfigurationSection("ships.").getKeys(false);
		String list = "Ships currently saved: ";
		
		for (String ship: ships)
		{
			list = list.concat(ship + ", ");
		}
		
		if (ships.size() == 0)
			list = list.concat("None.");
		else if (list.length() > 0)
		{
			list = list.substring(0, list.length() - 2);
			list = list.concat(".");
		}
		
		sender.sendMessage(ChatColor.YELLOW + list);
	}
	
	//TODO
	public void moveShip(Player player, String destination) throws DataException, IOException, MaxChangedBlocksException, IllegalArgumentException
	{
		//Determine the ship the player is in.
		Set<String> ships = data.getConfig().getConfigurationSection("ships.").getKeys(false);
		World shipWorld;
		double shipX;
		double shipY;
		double shipZ;
		double shipL;
		double shipW;
		double shipH;
		String shipCurrentDestination;
		String ship = null;
		
		for (String s: ships)
		{			
			shipCurrentDestination = data.getConfig().getString("ships." + s + ".currentDestination");
	
			shipWorld = Bukkit.getWorld(data.getConfig().getString("ships." + s + ".destinations." + shipCurrentDestination + ".world"));
			shipX = data.getConfig().getDouble("ships." + s + ".destinations." + shipCurrentDestination + ".x");
			shipY = data.getConfig().getDouble("ships." + s + ".destinations." + shipCurrentDestination + ".y");
			shipZ = data.getConfig().getDouble("ships." + s + ".destinations." + shipCurrentDestination + ".z");
			shipL = data.getConfig().getDouble("ships." + s + ".length");
			shipW = data.getConfig().getDouble("ships." + s + ".width");
			shipH = data.getConfig().getDouble("ships." + s + ".height");
				
			if (player.getWorld().equals(shipWorld)
					&& player.getLocation().getX() >= shipX && player.getLocation().getX() <= shipX + shipL
					&& player.getLocation().getY() >= shipY && player.getLocation().getY() <= shipY + shipH
					&& player.getLocation().getZ() >= shipZ && player.getLocation().getZ() <= shipZ + shipW)
				ship = s;		
		}
		
		if (ship == null)
			throw new IllegalArgumentException("You are not inside a ship!");
		
		//Make sure the player is a permitted pilot
		List <String> pilots = data.getConfig().getStringList("ships." + ship + ".pilots");
		boolean isPilot = false;
		
		for (String p : pilots)
			if (player.getUniqueId().toString().equalsIgnoreCase(p))
				isPilot = true;
		
		if (!isPilot && !player.hasPermission("horizonships.admin.ship.move"))
			throw new IllegalArgumentException("You don't have permission to pilot this ship.");
		
		//Make sure the ship isn't broken
		if (data.getConfig().getBoolean("ships." + ship + ".broken"))
			throw new IllegalArgumentException("The ship has broken down.");
		
		//Make sure the ship has enough fuel
		if (data.getConfig().getInt("ships." + ship + ".fuel") == 0)
			throw new IllegalArgumentException("The ship is out of fuel.");
		
		//Make sure the destination is valid. TODO
		
		//Save schematic at current location
		SchematicManager sm = new SchematicManager(player.getWorld());
		String currentDestination = data.getConfig().getString("ships." + ship + ".currentDestination");
		World world = Bukkit.getWorld(data.getConfig().getString("ships." + ship + ".destinations." + currentDestination + ".world"));
		double x = data.getConfig().getDouble("ships." + ship + ".destinations."  + currentDestination + ".x");
		double y = data.getConfig().getDouble("ships." + ship + ".destinations."  + currentDestination + ".y");
		double z = data.getConfig().getDouble("ships." + ship + ".destinations."  + currentDestination + ".z");
		Location loc1 = new Location(world, x, y, z);
		
		double length = data.getConfig().getDouble("ships." + ship + ".length");
		double width = data.getConfig().getDouble("ships." + ship + ".width");
		double height = data.getConfig().getDouble("ships." + ship + ".height");
		Location loc2 = new Location(world, x + length, y + height, z + width);

		sm.saveSchematic(loc1, loc2, "ship", ship + "\\");
		
		//Paste schematic at new location
		sm.loadSchematic("ship", loc1, ship + "\\");
		
		//Teleport all players from old to new location
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		double newX;
		double newY;
		double newZ;
		
		for (Player p: onlinePlayers)
			if (p.getWorld().equals(world)
					&& p.getLocation().getX() >= x && p.getLocation().getX() <= x + length
					&& p.getLocation().getY() >= y && p.getLocation().getY() <= y + height
					&& p.getLocation().getZ() >= z && p.getLocation().getZ() <= z + width)
			{
				World newWorld = Bukkit.getWorld(data.getConfig().getString("ships." + ship + ".destinations." + destination + ".world"));
				newX = data.getConfig().getDouble("ships." + ship + ".destinations." + destination + ".x");
				newY = data.getConfig().getDouble("ships." + ship + ".destinations." + destination + ".y");
				newZ = data.getConfig().getDouble("ships." + ship + ".destinations." + destination + ".z");

				p.teleport(new Location(newWorld, newX, newY, newZ));
			}		
		


		//Erase old location
		sm.eraseArea(world, loc1, loc2);
		
		//Reduce fuel by one
		data.getConfig().set("ships." + ship + ".fuel", data.getConfig().getInt("ships." + ship + ".fuel") - 1);
		
		//Change current destination
		data.getConfig().set("ships." + ship + ".currentDestination", destination);
		data.saveConfig();
		
		//TODO: Event trigger
	}
}
