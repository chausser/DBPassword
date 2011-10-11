/**
 *
 * @author Chausser
 * @server mcserver.nkings.com
 */
package me.chausser.dbpassword;

import java.io.File;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;


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
    String plug_config_ver = null;
    String plug_actual_ver = null;
    Statement stmt = null;
    boolean dbConnSuccess = false;
    Logger log = Logger.getLogger("Minecraft");
    public static PermissionHandler permissionHandler;
    private DBPasswordCommandExecutor myExecutor;

    @Override
    public void onEnable() {
        log.info("[DBPassword] Initalizing Plugin");
        setupPermissions();
        PluginDescriptionFile pdfFile = getDescription();
        if (!this.loadConfig(pdfFile.getVersion())) {
            log.info("[DBPassword] First time setup, Disabling plugin to allow for value changes in config file");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.plug_actual_ver = pdfFile.getVersion();

        if(!this.plug_actual_ver.equals(plug_config_ver)){
            this.runUpdateProcedures(plug_actual_ver);
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

        myExecutor = new DBPasswordCommandExecutor(this);
	getCommand("dbp").setExecutor(myExecutor);

        this.log.info("[" + pdfFile.getName() + "]" + " version " + pdfFile.getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        log.info("[DBPassword] has been disabled.");
    }

    public boolean loadConfig(String currentVersion) {
        File yml = new File("plugins/DBPassword/config.yml");
        if (!yml.exists()) {
            log.info("[DBPassword] Didnt find the config file, buidling default values");
            getConfig().options().copyDefaults(true);
            saveConfig();
            return false;
        } else {
            this.db_host = getConfig().getString("mysql.host");
            this.db_port = getConfig().getString("mysql.port");
            this.db_name = getConfig().getString("mysql.database");
            this.db_tabl = getConfig().getString("mysql.table");
            this.db_user = getConfig().getString("mysql.user");
            this.db_pass = getConfig().getString("mysql.password");
            this.sec_encrypt = getConfig().getString("security.encryption");
            this.sec_salt = getConfig().getString("security.salt");
            this.plug_config_ver = getConfig().getString("plugin.version");

            return true;
        }
    }

    public void setupPermissions() {
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

    public boolean checkPlayerHasPermission(Player player, String permissionNode){
        if(player.hasPermission(permissionNode)){
            return true;
        }
        if(permissionHandler.has(player, permissionNode)){
            return true;
        }

        return false;
    }

    public static String convertToHex(byte[] data) {
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

    private boolean runUpdateProcedures(String plug_actual_ver) {
        if(this.plug_config_ver.equals(plug_actual_ver)){
            return true;
        }
        else{
            String[] plug_major_minor = plug_config_ver.split(",");



            //this.log.info("[DBPassword] Successfully updated from version "+ plug_ver +" to version "+ pdfFile.getVersion());
        }
        return false;
    }

    /*** UPDATE FUNCTIONS ***/

    private void majorVer0MinorVer1(){

    }
    private void majorVer0MinorVer2(){

    }
    private void majorVer0MinorVer3(){

    }
    private void majorVer0MinorVer4(){

    }
    private void majorVer0MinorVer5(){

    }
}
