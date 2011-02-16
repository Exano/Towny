package com.shade.bukkit.towny.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import com.shade.bukkit.towny.AlreadyRegisteredException;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownyException;
import com.shade.bukkit.towny.object.Nation;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.Town;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.TownyUniverse;
import com.shade.bukkit.towny.object.TownyWorld;
import com.shade.util.FileMgmt;
import com.shade.util.KeyValueFile;

// TODO: Make sure the lack of a particular value doesn't error out the entire file

public class TownyFlatFileSource extends TownyDataSource {
	protected final String newLine = System.getProperty("line.separator");
	protected String rootFolder = "";
	protected String dataFolder = "/data";

	@Override
	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
		this.rootFolder = universe.getRootFolder();

		// Create files and folders if non-existent
		try {
			FileMgmt.checkFolders(new String[]{ rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + "/residents",
					rootFolder + dataFolder + "/towns",
					rootFolder + dataFolder + "/nations",
					rootFolder + dataFolder + "/worlds",
					rootFolder + dataFolder + "/townblocks"});
			FileMgmt.checkFiles(new String[]{
					rootFolder + dataFolder + "/residents.txt",
					rootFolder + dataFolder + "/towns.txt",
					rootFolder + dataFolder + "/nations.txt",
					rootFolder + dataFolder + "/worlds.txt"});
		} catch (IOException e) {
			System.out.println("[Towny] Error: Could not create flatfile default files and folders.");
		}
	}
	
	@Override
	public void backup() throws IOException {
		long t = System.currentTimeMillis();
		String newBackupFolder = rootFolder + "/backup/" + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + Long.toString(t);
		FileMgmt.checkFolders(new String[]{ rootFolder,
				rootFolder + "/backup",
				newBackupFolder});
		FileMgmt.copyDirectory(new File(rootFolder + dataFolder), new File(newBackupFolder));
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadResidentList() {
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/residents.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newResident(line);
			fin.close();
		} catch (Exception e) {
			try {
				fin.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean loadTownList() {
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/towns.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newTown(line);
			fin.close();
		} catch (Exception e) {
			try {
				fin.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean loadNationList() {
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/nations.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newNation(line);
			fin.close();
		} catch (Exception e) {
			try {
				fin.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return false;
		}
		return true;
	}
	
	
	@Override
	public boolean loadWorldList() {
		if (plugin != null)
			return loadServerWorldsList();
		else {
			String line;
			BufferedReader fin;

			try {
				fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/worlds.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			}
			try {
				while ((line = fin.readLine()) != null)
					if (!line.equals(""))
						universe.newWorld(line);
				fin.close();
			} catch (Exception e) {
				try {
					fin.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				return false;
			}
			return true;
		}
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResident(Resident resident) {
		String line;
		String path = rootFolder + dataFolder + "/residents/" + resident.getName() + ".txt";
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				resident.setLastOnline(Long.parseLong(kvFile.get("lastOnline")));
				
				line = kvFile.get("registered");
				if (line != null)
					resident.setRegistered(Long.parseLong(line));
				else
					resident.setRegistered(resident.getLastOnline());
				
				
				line = kvFile.get("town");
				if (line != null)
					resident.setTown(universe.getTown(line));

				line = kvFile.get("friends");
				if (line != null) {
					String[] tokens = line.split(",");
					for (String token : tokens) {
						Resident friend = universe.getResident(token);
						if (friend != null)
							resident.addFriend(friend);
					}
				}
				
				line = kvFile.get("protectionStatus");
				if (line != null)
					resident.setPermissions(line);

				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, null, resident);

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading resident file " + resident.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadTown(Town town) {
		String line;
		String[] tokens;
		String path = rootFolder + dataFolder + "/towns/" + town.getName() + ".txt";
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident resident = universe.getResident(token);
						if (resident != null)
							town.addResident(resident);
					}
				}

				line = kvFile.get("mayor");
				if (line != null)
					town.setMayor(universe.getResident(line));

				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null)
							town.addAssistant(assistant);
					}
				}

				town.setTownBoard(kvFile.get("townBoard"));

				line = kvFile.get("protectionStatus");
				if (line != null)
					town.setPermissions(line);

				line = kvFile.get("bonusBlocks");
				if (line != null)
					try {
						town.setBonusBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setBonusBlocks(0);
					}

				line = kvFile.get("plotPrice");
				if (line != null)
					try {
						town.setPlotPrice(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPlotPrice(0);
					}

				line = kvFile.get("taxes");
				if (line != null)
					try {
						town.setTaxes(Integer.parseInt(line));
					} catch (Exception e) {
						town.setTaxes(0);
					}
				
				line = kvFile.get("plotTax");
				if (line != null)
					try {
						town.setPlotTax(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPlotTax(0);
					}

				line = kvFile.get("pvp");
				if (line != null)
					try {
						town.setPVP(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("mobs");
				if (line != null)
					try {
						town.setHasMobs(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, town, null);

				line = kvFile.get("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = universe.getWorld(tokens[0]);
							int x = Integer.parseInt(tokens[1]);
							int z = Integer.parseInt(tokens[2]);
							TownBlock homeBlock = world.getTownBlock(x, z);
							town.setHomeBlock(homeBlock);
						} catch (NumberFormatException e) {
							System.out
									.println("[Towny] "
											+ town.getName()
											+ " homeBlock tried to load invalid location.");
						} catch (NotRegisteredException e) {
							System.out
									.println("[Towny] "
											+ town.getName()
											+ " homeBlock tried to load invalid world.");
						}
				}

				line = kvFile.get("spawn");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);
							town.setSpawn(new Location(world, x, y, z));
						} catch (NumberFormatException e) {
						} catch (NotRegisteredException e) {
						} catch (NullPointerException e) {
						}
				}

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading town file " + town.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadNation(Nation nation) {
		String line = "";
		String[] tokens;
		String path = rootFolder + dataFolder + "/nations/" + nation.getName() + ".txt";
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = universe.getTown(token);
						if (town != null)
							nation.addTown(town);
					}
				}

				line = kvFile.get("capital");
				nation.setCapital(universe.getTown(line));

				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null)
							nation.addAssistant(assistant);
					}
				}

				line = kvFile.get("allies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation friend = universe.getNation(token);
						if (friend != null)
							nation.setAliegeance("ally", friend);
					}
				}

				line = kvFile.get("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation enemy = universe.getNation(token);
						if (enemy != null)
							nation.setAliegeance("enemy", enemy);
					}
				}

				line = kvFile.get("taxes");
				if (line != null)
					try {
						nation.setTaxes(Integer.parseInt(line));
					} catch (Exception e) {
						nation.setTaxes(0);
					}
					
				line = kvFile.get("neutral");
				if (line != null)
					try {
						nation.setNeutral(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading nation file " + nation.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadWorld(TownyWorld world) {
		String line = "";
		String[] tokens;
		String path = rootFolder + dataFolder + "/worlds/" + world.getName() + ".txt";
		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = universe.getTown(token);
						if (town != null)
							world.addTown(town);
					}
				}

				// loadTownBlocks(world);

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading world file " + world.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	public boolean loadTownBlocks(TownyWorld world) {
		String line;
		String[] tokens;

		try {
			BufferedReader fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/townblocks/" + world.getName() + ".csv"));
			while ((line = fin.readLine()) != null) {
				tokens = line.split(",");
				if (tokens.length >= 3) {
					Town town;
					try {
						town = universe.getTown(tokens[2]);

						// Towns can't control blocks in more than one world.
						if (town.getWorld() != world)
							continue;

					} catch (TownyException e) {
						// Town can be null
						// since we also check admin only toggle
						town = null;
					}

					int x = Integer.parseInt(tokens[0]);
					int z = Integer.parseInt(tokens[1]);

					world.newTownBlock(x, z);
					TownBlock townblock = world.getTownBlock(x, z);
					townblock.setTown(town);

					if (tokens.length >= 4)
						try {
							Resident resident = universe.getResident(tokens[3]);
							townblock.setResident(resident);
						} catch (TownyException e) {
						}
					if (tokens.length >= 5)
						try {
							townblock.setForSale(Boolean
									.parseBoolean(tokens[4]));
						} catch (Exception e) {
						}
				}
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveResidentList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + "/residents.txt"));
			for (Resident resident : universe.getResidents())
				fout.write(resident.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving residents list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveTownList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + "/towns.txt"));
			for (Town town : universe.getTowns())
				fout.write(town.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving town list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveNationList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + "/nations.txt"));
			for (Nation nation : universe.getNations())
				fout.write(nation.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving nation list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveWorldList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + "/worlds.txt"));
			for (TownyWorld world : universe.getWorlds())
				fout.write(world.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving world list file");
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {
		try {
			String path = rootFolder + dataFolder + "/residents/" + resident.getName() + ".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			// Last Online
			fout.write("lastOnline=" + Long.toString(resident.getLastOnline()) + newLine);
			// Registered
			fout.write("registered=" + Long.toString(resident.getRegistered()) + newLine);
			if (resident.hasTown())
				fout.write("town=" + resident.getTown().getName() + newLine);
			// Friends
			fout.write("friends=");
			for (Resident friend : resident.getFriends())
				fout.write(friend.getName() + ",");
			fout.write(newLine);
			// TownBlocks
			fout.write("townBlocks=" + utilSaveTownBlocks(resident.getTownBlocks()) + newLine);
			// Plot Protection
			fout.write("protectionStatus=" + resident.getPermissions().toString() + newLine);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean saveTown(Town town) {
		BufferedWriter fout;
		String path = rootFolder + dataFolder + "/towns/" + town.getName() + ".txt";
		try {
			fout = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			// Residents
			fout.write("residents=");
			for (Resident resident : town.getResidents())
				fout.write(resident.getName() + ",");
			fout.write(newLine);
			// Mayor
			if (town.hasMayor())
				fout.write("mayor=" + town.getMayor().getName() + newLine);
			// Nation
			if (town.hasNation())
				fout.write("nation=" + town.getNation().getName() + newLine);
			// Assistants
			fout.write("assistants=");
			for (Resident assistant : town.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			// Town Board
			fout.write("townBoard=" + town.getTownBoard() + newLine);
			// Town Protection
			fout.write("protectionStatus=" + town.getPermissions().toString() + newLine);
			// Bonus Blocks
			fout.write("bonusBlocks=" + Integer.toString(town.getBonusBlocks()) + newLine);
			// Taxes
			fout.write("taxes=" + Integer.toString(town.getTaxes()) + newLine);
			// Plot Price
			fout.write("plotPrice=" + Integer.toString(town.getPlotPrice()) + newLine);
			// Plot Tax
			fout.write("plotTax=" + Integer.toString(town.getPlotTax()) + newLine);
			// PVP
			fout.write("pvp=" + Boolean.toString(town.isPVP()) + newLine);
			// Mobs
			fout.write("mobs=" + Boolean.toString(town.hasMobs()) + newLine);
			// TownBlocks
			fout.write("townBlocks=" + utilSaveTownBlocks(town.getTownBlocks()) + newLine);
			// Home Block
			fout.write("homeBlock=" + town.getHomeBlock().getWorld().getName() + ","
					+ Integer.toString(town.getHomeBlock().getX()) + ","
					+ Integer.toString(town.getHomeBlock().getZ()) + newLine);
			// Spawn
			fout.write("spawn=" + town.getSpawn().getWorld().getName() + ","
					+ Double.toString(town.getSpawn().getX()) + ","
					+ Double.toString(town.getSpawn().getY()) + ","
					+ Double.toString(town.getSpawn().getZ()) + newLine);

			fout.close();
		} catch (Exception e) {
			try {
				fout.close();
			} catch (IOException ioe) {
			}
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean saveNation(Nation nation) {
		try {
			String path = rootFolder + dataFolder + "/nations/" + nation.getName() + ".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : nation.getTowns())
				fout.write(town.getName() + ",");
			fout.write(newLine);
			if (nation.hasCapital())
				fout.write("capital=" + nation.getCapital().getName());
			fout.write(newLine);
			fout.write("assistants=");
			for (Resident assistant : nation.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			fout.write("friends=");
			for (Nation allyNation : nation.getAllies())
				fout.write(allyNation.getName() + ",");
			fout.write(newLine);
			fout.write("enemies=");
			for (Nation enemyNation : nation.getEnemies())
				fout.write(enemyNation.getName() + ",");
			fout.write(newLine);
			// Taxes
			fout.write("taxes=" + Integer.toString(nation.getTaxes()) + newLine);
			// Neutral
			fout.write("neutral=" + Boolean.toString(nation.isNeutral()) + newLine);

			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean saveWorld(TownyWorld world) {
		try {
			String path = rootFolder + dataFolder + "/worlds/" + world.getName() + ".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : world.getTowns())
				fout.write(town.getName() + ",");
			fout.write(newLine);

			fout.close();

			// saveTownBlocks(world);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * public boolean saveTownBlocks(TownyWorld world) { try { BufferedWriter
	 * fout = new BufferedWriter(new FileWriter(rootFolder +
	 * dataFolder + "/townblocks/"+world.getName()+".csv")); for (TownBlock townblock :
	 * world.getTownBlocks()) { String line = townblock.getX() + "," +
	 * Long.toString(townblock.getZ()); line += ","; if (townblock.hasTown())
	 * line += townblock.getTown().getName(); line += ","; if
	 * (townblock.hasResident()) line += townblock.getResident().getName(); line
	 * += "," + Boolean.toString(townblock.isForSale()); fout.write(line +
	 * newLine); } fout.close(); return true; } catch (Exception e) {
	 * System.out.println("[Towny] Loading Error: Exception while saving town blocks list file");
	 * e.printStackTrace();
	 * return false; } }
	 */

	/**
	 * Load townblocks according to the given line Townblock: x,y,forSale Eg:
	 * townBlocks=world:10,11;10,12,true;|nether:1,1|
	 * 
	 * @param line
	 * @param town
	 * @param resident
	 */

	public void utilLoadTownBlocks(String line, Town town, Resident resident) {
		String[] worlds = line.split("\\|");
		for (String w : worlds) {
			String[] split = w.split(":");
			if (split.length != 2)
				continue;
			try {
				TownyWorld world = universe.getWorld(split[0]);
				for (String s : split[1].split(";")) {
					String[] tokens = s.split(",");
					if (tokens.length < 2)
						continue;
					try {
						int x = Integer.parseInt(tokens[0]);
						int z = Integer.parseInt(tokens[1]);

						try {
							world.newTownBlock(x, z);
						} catch (AlreadyRegisteredException e) {
						}
						TownBlock townblock = world.getTownBlock(x, z);

						if (town != null)
							townblock.setTown(town);

						if (resident != null)
							townblock.setResident(resident);

						if (tokens.length >= 3)
							townblock.setForSale(true); //Automatically assume the townblock is for sale
					} catch (NumberFormatException e) {
					} catch (NotRegisteredException e) {
					}
				}
			} catch (NotRegisteredException e) {
				continue;
			}
		}
	}

	public String utilSaveTownBlocks(List<TownBlock> townBlocks) {
		HashMap<TownyWorld, ArrayList<TownBlock>> worlds = new HashMap<TownyWorld, ArrayList<TownBlock>>();
		String out = "";

		// Sort all town blocks according to what world its in
		for (TownBlock townBlock : townBlocks) {
			TownyWorld world = townBlock.getWorld();
			if (!worlds.containsKey(world))
				worlds.put(world, new ArrayList<TownBlock>());
			worlds.get(world).add(townBlock);
		}

		for (TownyWorld world : worlds.keySet()) {
			out += world.getName() + ":";
			for (TownBlock townBlock : worlds.get(world))
				out += townBlock.getX() + "," + townBlock.getZ() + (townBlock.isForSale() ? ",true" : "") + ";";
			out += "|";
		}

		return out;
	}
}