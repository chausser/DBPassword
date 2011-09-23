/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.chausser.dbpassword;

import java.io.File;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
// org.bukkit.plugin.PluginManager;
//import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 *
 * @author Chase
 */
public class DBPassword extends JavaPlugin {

    String db_host = null;
    String db_port = null;
    String db_name = null;
    String db_tabl = null;
    String db_user = null;
    String db_pass = null;
    String sec_encrypt = null;
    String sec_salt = null;
    Connection conn = null;
    Statement stmt = null;
    boolean dbConnSuccess = false;
    Logger log = Logger.getLogger("Minecraft");
    protected static Configuration config;
    public static PermissionHandler permissionHandler;

    public void onEnable() {
        log.info("[DBPassword] Initalizing Plugin");
        setupPermissions();
        if (!this.loadConfig()) {
            log.info("[DBPassword] First time setup, Disabling plugin to allow for value changes in config file");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.conn = DriverManager.getConnection("jdbc:mysql://" + this.db_host + ":" + this.db_port + "/" + this.db_name + "?user=" + this.db_user + "&password=" + this.db_pass);
            this.stmt = this.conn.createStatement();
            this.stmt.execute("CREATE TABLE IF NOT EXISTS `" + this.db_name + "`.`" + this.db_tabl + "` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY ,`username` VARCHAR(16) NOT NULL ,`password` VARCHAR(42) NULL )");
            this.log.info("[DBPassword] Successfully connected to MySQL.");
            this.dbConnSuccess = true;
        } catch (SQLException e) {
            this.log.info("[DBPassword] Cannot connect to MySQL. Disabling plugin");
            this.log.info(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //PluginManager pm = this.getServer().getPluginManager();

        PluginDescriptionFile pdfFile = getDescription();

        this.log.info("[" + pdfFile.getName() + "]" + " version " + pdfFile.getVersion() + " is enabled!");
    }

    public void onDisable() {
        log.info("[DBPassword] has been disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        
        if (!this.dbConnSuccess) {
            //shouldnt be able to get here but just in case
            player.sendMessage("Error: Cannot connect to the database to save your password, Try again later.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("dbp")) {
            if (args.length == 1) {
                if(args[0].equalsIgnoreCase("reload")){
                    if (!permissionHandler.has(player, "dbp.reload")) {
                        player.sendMessage("Error: You do not have permission.");
                        return true;
                    }
                    boolean reload = this.loadConfig();
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
                    if (!permissionHandler.has(player, "dbp.set") || !permissionHandler.has(player, "dbp.update")) {
                        player.sendMessage("Error: You do not have permission.");
                        return true;
                    }
                    if (player == null) {
                        sender.sendMessage("[DBPassword] this command can only be run by a player");
                    } else {
                        String passString = this.sec_salt + args[1];
                        String passHash = null;
                        if (this.sec_encrypt.equalsIgnoreCase("MD5")) {
                            passHash = this.MD5(passString);
                        } else if (this.sec_encrypt.equalsIgnoreCase("SHA1")) {
                            passHash = this.SHA1(passString);
                        } else {
                            //assume md5
                            this.log.info("[DBPassword] Unable to determine the encryption type assuming md5.");
                            passHash = this.MD5(passString);
                        }
                        try {
                            this.stmt = this.conn.createStatement();
                            ResultSet rs = this.stmt.executeQuery("SELECT * FROM " + this.db_tabl + " WHERE username = '" + player.getName() + "'");
                            if (!rs.first()) {
                                try {
                                    this.stmt = this.conn.createStatement();
                                    this.stmt.execute("INSERT INTO " + this.db_tabl + "(`id`, `username`, `password`) VALUES(null, '" + player.getName() + "', '" + passHash + "')");
                                    player.sendMessage("[DBPassword] Your password has been succesfully set.");
                                    this.log.info("[DBPassword] " + player.getDisplayName() + " has set his/her password.");
                                } catch (SQLException e) {
                                    this.log.info("[DBPassword] " + e.toString());
                                    player.sendMessage("[DBPassword] Error: Could not connect to database.");
                                }
                            } else {
                                try {
                                    this.stmt = this.conn.createStatement();
                                    this.stmt.execute("UPDATE `" + this.db_tabl + "` SET `password` = '" + passHash + "' WHERE `username` = '" + player.getName() + "'");
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

    private boolean loadConfig() {
        File yml = new File("plugins/DBPassword/config.yml");
        if (!yml.exists()) {
            log.info("[DBPassword] Didnt find the config file, buidling default values");
            config = getConfiguration();
            config.setProperty("mysql.host", "'localhost'");
            config.setProperty("mysql.port", "'3306'");
            config.setProperty("mysql.database", "'minecraft'");
            config.setProperty("mysql.table", "'users'");
            config.setProperty("mysql.user", "'root'");
            config.setProperty("mysql.password", "''");
            config.setProperty("security.encryption", "'MD5 or SHA1'");
            config.setProperty("security.salt", "'s0m3 s@lT StriNg'");
            config.save();
            return false;
        } else {
            config = getConfiguration();
            this.db_host = config.getString("mysql.host", "localhost");
            this.db_port = config.getString("mysql.port", "3306");
            this.db_name = config.getString("mysql.database", "minecraft");
            this.db_tabl = config.getString("mysql.table", "users");
            this.db_user = config.getString("mysql.user", "root");
            this.db_pass = config.getString("mysql.password", "");
            this.sec_encrypt = config.getString("security.encryption", "MD5");
            this.sec_salt = config.getString("security.salt", "");
            
            /*
            Debugging Info
            log.info("[DBPassword] Host:" + this.db_host); 
            log.info("[DBPassword] port:" + this.db_port);
            log.info("[DBPassword] database:" + this.db_name);
            log.info("[DBPassword] table:" + this.db_tabl);
            log.info("[DBPassword] user:" + this.db_user);
            log.info("[DBPassword] password:" + this.db_pass);
            log.info("[DBPassword] encryption:" + this.sec_encrypt);
            log.info("[DBPassword] salt:" + this.sec_salt);
             */
            return true;
        }
    }

    private void setupPermissions() {
        if (permissionHandler != null) {
            return;
        }
        Plugin permissionsPlugin = getServer().getPluginManager().getPlugin("Permissions");

        if (permissionsPlugin == null) {
            this.log.info("[DBPassword] Unable to locate a Permission system, Using OPs");
            return;
        }

        permissionHandler = ((Permissions) permissionsPlugin).getHandler();
        this.log.info("[DBPassword] Found and will use the permission plugin " + ((Permissions) permissionsPlugin).getDescription().getFullName());
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] sha1hash = new byte[40];
        try {
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static String MD5(String str) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(str.getBytes());

        byte[] byteData = md.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xFF & byteData[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}