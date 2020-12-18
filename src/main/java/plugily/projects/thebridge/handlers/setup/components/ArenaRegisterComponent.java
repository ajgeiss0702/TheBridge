/*
 * The Bridge - Protect villagers from hordes of zombies
 * Copyright (C) 2020  Plugily Projects - maintained by 2Wild4You, Tigerpanzer_02 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugily.projects.thebridge.handlers.setup.components;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.plajerlair.commonsbox.minecraft.compat.XMaterial;
import pl.plajerlair.commonsbox.minecraft.configuration.ConfigUtils;
import pl.plajerlair.commonsbox.minecraft.item.ItemBuilder;
import pl.plajerlair.commonsbox.minecraft.serialization.LocationSerializer;
import plugily.projects.thebridge.Main;
import plugily.projects.thebridge.arena.Arena;
import plugily.projects.thebridge.arena.ArenaRegistry;
import plugily.projects.thebridge.arena.ArenaUtils;
import plugily.projects.thebridge.handlers.setup.SetupInventory;
import plugily.projects.thebridge.handlers.sign.ArenaSign;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 08.06.2019
 */
public class ArenaRegisterComponent implements SetupComponent {

  private SetupInventory setupInventory;

  @Override
  public void prepare(SetupInventory setupInventory) {
    this.setupInventory = setupInventory;
  }

  @Override
  public void injectComponents(StaticPane pane) {
    FileConfiguration config = setupInventory.getConfig();
    Main plugin = setupInventory.getPlugin();
    ItemStack registeredItem;
    if (!setupInventory.getArena().isReady()) {
      registeredItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
          .name(plugin.getChatManager().colorRawMessage("&e&lRegister Arena - Finish Setup"))
          .lore(ChatColor.GRAY + "Click this when you're done with configuration.")
          .lore(ChatColor.GRAY + "It will validate and register arena.")
          .build();
    } else {
      registeredItem = new ItemBuilder(Material.BARRIER)
          .name(plugin.getChatManager().colorRawMessage("&a&lArena Registered - Congratulations"))
          .lore(ChatColor.GRAY + "This arena is already registered!")
          .lore(ChatColor.GRAY + "Good job, you went through whole setup!")
          .lore(ChatColor.GRAY + "You can play on this arena now!")
          .build();
    }
    pane.addItem(new GuiItem(registeredItem, e -> {
      Arena arena = setupInventory.getArena();
      if (arena == null) {
        return;
      }
      if (arena.isReady()) {
        e.getWhoClicked().sendMessage(ChatColor.GREEN + "This arena was already validated and is ready to use!");
        return;
      }
      //todo write arena register component
      for (String s : new String[] {"lobbylocation", "Startlocation", "Endlocation"}) {
        if (!config.isSet("instances." + arena.getId() + "." + s) || config.getString("instances." + arena.getId() + "." + s)
            .equals(LocationSerializer.locationToString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
          e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✘ &cArena validation failed! Please configure following spawns properly: " + s + " (cannot be world spawn location)"));
          return;
        }
      }
      if (config.getConfigurationSection("instances." + arena.getId() + ".doors") == null) {
        e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✘ &cArena validation failed! Please configure doors properly!"));
        return;
      }
      e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&a&l✔ &aValidation succeeded! Registering new arena instance: " + arena.getId()));
      config.set("instances." + arena.getId() + ".isdone", true);
      ConfigUtils.saveConfig(plugin, config, "arenas");
      List<Sign> signsToUpdate = new ArrayList<>();
      ArenaRegistry.unregisterArena(arena);

      for (ArenaSign arenaSign : plugin.getSignManager().getArenaSigns()) {
        if (arenaSign.getArena().equals(setupInventory.getArena())) {
          signsToUpdate.add(arenaSign.getSign());
        }
      }
      arena = ArenaUtils.initializeArena(arena.getId());
      arena.setReady(true);
      arena.setMinimumPlayers(config.getInt("instances." + arena.getId() + ".minimumplayers"));
      arena.setMaximumPlayers(config.getInt("instances." + arena.getId() + ".maximumplayers"));
      arena.setMapName(config.getString("instances." + arena.getId() + ".mapname"));
      arena.setLobbyLocation(LocationSerializer.getLocation(config.getString("instances." + arena.getId() + ".lobbylocation")));
      arena.setEndLocation(LocationSerializer.getLocation(config.getString("instances." + arena.getId() + ".Endlocation")));
      ArenaRegistry.registerArena(arena);
      arena.start();
      for (Sign s : signsToUpdate) {
        plugin.getSignManager().getArenaSigns().add(new ArenaSign(s, arena));
      }
    }), 2, 1);
  }

}