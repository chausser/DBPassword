/**
 *
 * @author chausser
 */
package me.chausser.dbpassword;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class DBPasswordCommandExecutor implements CommandExecutor {

    private DBPassword plugin;

    Logger log = Logger.getLogger("Minecraft");

    public DBPasswordCommandExecutor(DBPassword plugin) {
            this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (!this.plugin.dbConnSuccess) {
            //shouldnt be able to get here but just in case
            player.sendMessage("Error: Cannot connect to the database to save your password, Try again later.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("dbp")) {
            if (args.length == 1) {
                if(args[0].equalsIgnoreCase("reload")){
                    if (!this.plugin.checkPlayerHasPermission(player, "dbp.reload")) {
                        player.sendMessage("Error: You do not have permission.");
                        return true;
                    }
                    boolean reload = this.plugin.loadConfig(this.plugin.plug_config_ver);
                    if(reload){
                        this.log.info("[DBPassword] Configuration Reloaded.");
                        sender.sendMessage("[DBPassword] Configuration Reloaded.");
                    }
                    else{
                        this.log.info("[DBPassword] Failed to Reload Configuration.");
                        sender.sendMessage("[DBPassword] Failed to Reload Configuration.");
                    }
                }
                else{
                    player.sendMessage("Use /dbp set [password] - to set your password.");
                    player.sendMessage("Use /dbp update [password] - to update your password.");
                    player.sendMessage("Use /dbp reload - reloads the config");
                }
            }
            if (args.length == 2) {
                if(args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("update")){
                    if (!this.plugin.checkPlayerHasPermission(player, "dbp.set") || !this.plugin.checkPlayerHasPermission(player, "dbp.update")) {
                        player.sendMessage("Error: You do not have permission.");
                        return true;
                    }
                    if (player == null) {
                        sender.sendMessage("[DBPassword] this command can only be run by a player");
                    } else {
                        String passString = this.plugin.sec_salt + args[1];
                        String passHash = null;
                        if (this.plugin.sec_encrypt.equalsIgnoreCase("md5")) {
                            passHash = this.plugin.MD5(passString);
                        } else if (this.plugin.sec_encrypt.equalsIgnoreCase("sha1")) {
                            passHash = this.plugin.SHA1(passString);
                        } else {
                            //assume md5
                            this.log.info("[DBPassword] Unable to determine the encryption type assuming md5.");
                            passHash = this.plugin.MD5(passString);
                        }
                        try {
                            this.plugin.stmt = this.plugin.conn.createStatement();
                            ResultSet rs = this.plugin.stmt.executeQuery("SELECT * FROM " + this.plugin.db_tabl + " WHERE username = '" + player.getName() + "'");
                            if (!rs.first()) {
                                try {
                                    this.plugin.stmt = this.plugin.conn.createStatement();
                                    this.plugin.stmt.execute("INSERT INTO " + this.plugin.db_tabl + "(`id`, `username`, `password`) VALUES(null, '" + player.getName() + "', '" + passHash + "')");
                                    player.sendMessage("[DBPassword] Your password has been succesfully set.");
                                    this.log.info("[DBPassword] " + player.getDisplayName() + " has set his/her password.");
                                } catch (SQLException e) {
                                    this.log.info("[DBPassword] " + e.toString());
                                    player.sendMessage("[DBPassword] Error: Could not connect to database.");
                                }
                            } else {
                                try {
                                    this.plugin.stmt = this.plugin.conn.createStatement();
                                    this.plugin.stmt.execute("UPDATE `" + this.plugin.db_tabl + "` SET `password` = '" + passHash + "' WHERE `username` = '" + player.getName() + "'");
                                    player.sendMessage("[DBPassword] Your password has been succesfully updated.");
                                    this.log.info("[DBPassword] " + player.getDisplayName() + " has updated his/her password.");
                                } catch (SQLException e) {
                                    this.log.info("[DBPassword] " + e.toString());
                                    player.sendMessage("[DBPassword] Error: Could not connect to database.");
                                }
                            }
                        } catch (SQLException e) {
                            this.log.info(e.toString());
                        }
                    }
                }
                else{
                    player.sendMessage("Use /dbp set [password] - to set your password.");
                    player.sendMessage("Use /dbp update [password] - to update your password.");
                    player.sendMessage("Use /dbp reload - reloads the config");
                }
            }
            if (args.length == 0) {
                if (player == null) {
                    sender.sendMessage("[DBPassword] this command is used in game to allow a player to set / update his or her password in the database");
                } else {
                    player.sendMessage("Use /dbp set [password] - to set your password.");
                    player.sendMessage("Use /dbp update [password] - to update your password.");
                    player.sendMessage("Use /dbp reload - reloads the config");
                }
            }
            return true;
        }
        return false;
    }

}
