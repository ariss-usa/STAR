package org.ariss.star;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ConfigManager {
    private static String globalConfigName = "important.json";
    private static String callsignName = "callsign.json";
    private static UserConfig config;
    public static RobotEntry callsignEntry;

    public static boolean isGlobalModeEnabled(){
        File file = new File(globalConfigName);
        if(!file.exists()){
            return false;
        }
        
        try(FileReader reader = new FileReader(file)){
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            return  obj.has("id") &&
                    obj.has("school") &&
                    obj.has("city") &&
                    obj.has("state");
        }
        catch (IOException | JsonParseException e){
            return false;
        }
    }

    public static boolean dumpConfig(String id, String school, String city, String state){
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("school", school);
        obj.addProperty("city", city);
        obj.addProperty("state", state);

        try(FileWriter writer = new FileWriter(globalConfigName)){
            writer.write(obj.toString());
            return true;
        }
        catch(IOException e){
            return false;
        }
    }

    public static boolean readConfig(){
        File file = new File(globalConfigName);
        if(!file.exists()) return false;

        try(FileReader reader = new FileReader(file)){
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            config = new UserConfig(obj.get("id").getAsString(), 
                                    obj.get("school").getAsString(),
                                    obj.get("city").getAsString(),
                                    obj.get("state").getAsString());
            return true;
        }
        catch(IOException | JsonParseException e){
            return false;
        }
    }

    public static boolean hasCallsignConfig(){
        File file = new File(callsignName);
        if(!file.exists()){
            return false;
        }
        
        try(FileReader reader = new FileReader(file)){
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            return  obj.has("callsign") &&
                    obj.has("destination_callsign");
        }
        catch (IOException | JsonParseException e){
            return false;
        }
    }

    public static RobotEntry readCallsignConfig(){
        File file = new File(callsignName);
        if(!file.exists()){
            return null;
        }

        try(FileReader reader = new FileReader(file)){
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            callsignEntry = new RobotEntry(obj.get("callsign").getAsString(), obj.get("destination_callsign").getAsString());
            return callsignEntry;
        }
        catch(IOException | JsonParseException e){
            return null;
        }
    }

    public static boolean dumpCallsignConfig(String callsign, String destination){
        JsonObject obj = new JsonObject();
        obj.addProperty("callsign", callsign);
        obj.addProperty("destination_callsign", destination);

        try(FileWriter writer = new FileWriter(callsignName)){
            writer.write(obj.toString());
            return true;
        }
        catch(IOException e){
            return false;
        }
    }

    public static UserConfig getConfig(){return config;}
}

class UserConfig {
    public String id;
    public String school;
    public String city;
    public String state;

    public UserConfig(String id, String school, String city, String state){
        this.id = id;
        this.school = school;
        this.city = city;
        this.state = state;
    }
}