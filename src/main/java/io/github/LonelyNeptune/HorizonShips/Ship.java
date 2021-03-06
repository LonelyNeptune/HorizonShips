package io.github.LonelyNeptune.HorizonShips;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldedit.data.DataException;

// This class contains all the information pertaining to a ship. It is controlled by a ShipHandler.
class Ship
{
	private FileConfiguration data;
	private String name;
	private Destination destination;
	private int dock;
	private int fuel;
	private boolean broken;
	private String repairItem;
	private boolean consumePart;
	private List<UUID> pilots = new ArrayList<>();
	private int length;
	private int width;
	private int height;
	private UUID owner;
	
	// Constructor for fetching an existing ship from the data file.
	Ship(FileConfiguration data, String name)
	{
		String path = "ships." + name + ".";
		if (data.getConfigurationSection("ships." + name) == null) //Ship doesn't exist
			throw new IllegalArgumentException("Ship does not exist.");
		this.data = data;	
		this.name = name;		
		destination = new Destination(data, data.getString(path + "dock.destination"), true);
		dock = data.getInt(path + "dock.id");
		fuel = data.getInt(path + "fuel");
		broken = data.getBoolean(path + "broken");
		repairItem = data.getString(path + "repairItem");
		consumePart = data.getBoolean(path + "consumePart");
		List<String> pilotsString = data.getStringList(path + "pilots");
		for (String s: pilotsString)
			pilots.add(UUID.fromString(s));
		length = data.getInt(path + "length");
		width = data.getInt(path + "width");
		height = data.getInt(path + "height");
		if (data.getString(path + "owner") != null)
			owner = UUID.fromString(data.getString(path + "owner"));
	}

	// Constructor for creating an entirely new ship.
	Ship(FileConfiguration data, String name, Location min, Location max) throws DataException, IOException
	{
		this.data = data;
		String path = "ships." + name + ".";
		
		setName(name);
		setFuel(10);
		setBroken(false);
		setLength(max.getBlockX() - min.getBlockX());
		setWidth(max.getBlockZ() - min.getBlockZ());
		setHeight(max.getBlockY() - min.getBlockY());
		
		//Try to make a new temp destination. If it already exists that's fine.
		try { destination = new Destination(data, "temp", false); }
		catch (IllegalArgumentException e) { destination = new Destination(data, "temp", true); }
		Dock tempDock = destination.addDock(min, length, height, width);
		tempDock.updateShipName(name);
		data.set(path + ".dock.destination", destination.getName());
		data.set(path + "dock.id", tempDock.getID());
		
		SchematicManager sm = new SchematicManager(min.getWorld());
		sm.saveSchematic(min, max, name + "\\ship");
	}
	
	// deleteShip() removes all the information about the ship and its destinations. The instance or its destinations
	// should NOT continue to be used after this is called.
	void deleteShip()
	{
		data.set("ships." + name, null);
		
		//Delete ship schematic
		SchematicManager sm = new SchematicManager(Bukkit.getWorld("world"));
		sm.deleteSchematic(name + "\\");
	}
	
	// getDock() returns the dock that the ship is currently inhabiting.
	Dock getDock()
	{
		return destination.getDock(dock);
	}
	
	// setDock() sets the dock that the ship is currently inhabiting.
	void setDock(Dock dock) throws IllegalArgumentException
	{
		Dock oldDock = destination.getDock(this.dock);
		
		//Keep the ship name updated on the dock so the ship belonging to it can easily be found.
		oldDock.updateShipName(null);
		dock.updateShipName(name);
		
		//If the old dock was temporary it should be removed.
		if (oldDock.getDestination().equalsIgnoreCase("temp"))
		{
			Destination destination;
			try { destination = new Destination(data, "temp", false); }
			catch (IllegalArgumentException e) { destination = new Destination(data, "temp", true); }
			destination.getDocks().remove(oldDock);
			oldDock.delete();
		}
			
		//Set the dock
		this.dock = dock.getID();
		this.destination = new Destination(data, dock.getDestination(), true);
		data.set("ships." + name + ".dock.destination", this.destination.getName());
		data.set("ships." + name + ".dock.id", this.dock);
	}
	
	// getDestination() returns the destination at which the ship is currently located.
	Destination getDestination()
	{
		return destination;
	}
	
	// setDestination() sets the name of the destination at which the ship is currently located.
	void setDestination(Destination destination)
	{
		this.destination = destination;
	}
	
	// isBroken() returns true if the ship is broken, false otherwise.
	boolean isBroken()
	{
		return broken;
	}
	
	// setBroken() sets the broken status of the ship.
	void setBroken(boolean broken)
	{
		this.broken = broken;
		data.set("ships." + name + ".broken", broken);
	}

	// getLength() returns the length of the ship.
	int getLength()
	{
		return length;
	}
	
	// setLength() sets the length of the ship.
	private void setLength(int length)
	{
		this.length = length;
		data.set("ships." + name + ".length", length);
	}
	
	// getWidth() returns the width of the ship.
	int getWidth()
	{
		return width;
	}
	
	// setWidth() sets the length of the ship.
	private void setWidth(int width)
	{
		this.width = width;
		data.set("ships." + name + ".width", width);
	}
	
	// getHeight() returns the height of the ship.
	int getHeight()
	{
		return height;
	}
	
	// setHeight() sets the height of the ship.
	private void setHeight(int height)
	{
		this.height = height;
		data.set("ships." + name + ".height", height);
	}
	
	// getFuel() returns the fuel level of the ship.
	int getFuel()
	{
		return fuel;
	}
	
	// setFuel() sets the fuel level of the ship.
	void setFuel(int fuel)
	{
		this.fuel = fuel;
		data.set("ships." + name + ".fuel", fuel);
	}
	
	// reduceFuel() reduces the fuel level by one.
	void reduceFuel()
	{

		data.set("ships." + name + ".fuel", --fuel);
	}
	
	// getName() returns the name of the ship.
	String getName()
	{
		return name;
	}
	
	// setName() sets the name of the ship. To be used only while renaming ships. Setting the name without transferring
	// all data will result in lost ship data.
	private void setName(String newName)
	{
		name = newName;
	}
	
	// getRepairItem() returns the item required to repair the ship.
	String getRepairItem()
	{
		return repairItem;
	}
	
	// setRepairItem() sets the item required to repair the ship.
	void setRepairItem(String item)
	{
		this.repairItem = item;
		data.set("ships." + name + ".repairItem", item);
	}
	
	// getConsumePart() returns whether the item required to repair the ship is consumed upon repair
	boolean getConsumePart()
	{
		return consumePart;
	}
	
	// setConsumePart() sets whether the item required to repair the ship is consumed upon repair.
	void setConsumePart(boolean consumePart)
	{
		this.consumePart = consumePart;
		data.set("ships." + name + ".consumePart", consumePart);
	}
	
	// getPilots() returns a list of the UUIDs of the players who are permitted to fly the ship.
	List<UUID> getPilots()
	{
		return pilots;
	}
	
	// isPilot() checks if the Player provided is allowed to pilot the ship.
	boolean isPilot(UUID player)
	{
		List <String> pilots = data.getStringList("ships." + name + ".pilots");
		
		for (String p : pilots)
			if (player.toString().equalsIgnoreCase(p))
				return true;
		return false;
	}
	
	// addPilot() adds the uuid given to the list of permitted pilots for the ship.
	void addPilot(UUID uuid)
	{
		pilots.add(uuid);
		
		List<String> pilotStrings = new ArrayList<>();
		
		for (UUID u: pilots)
			pilotStrings.add(u.toString());
		
		data.set("ships." + name + ".pilots", pilotStrings);
	}
	
	void removePilot(UUID uuid)
	{
		pilots.remove(uuid);
		
		List<String> pilotStrings = new ArrayList<>();
		
		for (UUID u: pilots)
			pilotStrings.add(u.toString());
		
		data.set("ships." + name + ".pilots", pilotStrings);
	}
	
	// getOwner() returns the UUID of the player who is considered to own the ship.
	UUID getOwner()
	{
		return owner;
	}
	
	void setOwner(UUID uuid)
	{
		owner = uuid;
		data.set("ships." + name + ".owner", uuid.toString());
	}
	
	// rename() takes all of the data under the old ship name and transfers it to a new ship name.
	void rename(String newName)
	{
		String path = "ships." + newName + ".";
		
		data.set(path + "fuel", fuel);
		data.set(path + "broken", broken);
		data.set(path + "length", length);
		data.set(path + "width", width);
		data.set(path + "height", height);
		data.set(path + "consumePart", consumePart);
		data.set(path + "repairItem", repairItem);
		data.set(path + "dock.destination", destination.getName());
		data.set(path + "dock.id", dock);
		if (owner != null)
			data.set(path + "owner", owner.toString());
		List<String> pilotStrings = new ArrayList<>();
		for (UUID p: pilots)
			pilotStrings.add(p.toString());
		data.set(path + "pilots", pilotStrings);
	
		data.getConfigurationSection("ships.").set(name, null);
		name = newName;
	}
	
	// exists() returns true if the ship exists on file, false otherwise.
	boolean exists()
	{
		return name != null;
	}
}
