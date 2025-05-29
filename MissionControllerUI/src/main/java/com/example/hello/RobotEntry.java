package com.example.hello;

import java.util.Objects;

public class RobotEntry {
    String id, school, city, state, port;
    boolean isLocalRobot;
    public RobotEntry(String id, String school, String city, String state){
        this.id = id;
        this.school = school;
        this.city = city;
        this.state = state;
        this.isLocalRobot = false;
    }
    public RobotEntry(String port){
        this.port = port;
        this.isLocalRobot = true;
    }

    public boolean isLocal(){
        return isLocalRobot;
    }

    public String getId(){
        return isLocalRobot ? port : id;
    }

    public String toString(){
        if(isLocalRobot){
            return port;
        }
        return String.format("%s\n%s\n%s\n%s", this.id, this.school, this.city, this.state);
    }

    public RobotEntry get_copy(){
        if(isLocalRobot){
            return new RobotEntry(port);
        }
        return new RobotEntry(id, school, city, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RobotEntry)) return false;
        RobotEntry that = (RobotEntry) o;
        return isLocalRobot == that.isLocalRobot &&
                Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), isLocalRobot);
    }
}
