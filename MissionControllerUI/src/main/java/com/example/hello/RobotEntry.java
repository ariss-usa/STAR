package com.example.hello;

import java.util.Objects;

public class RobotEntry {
    String id, school, city, state, port;
    String myCallsign, callsignToAccept;
    EntryType type;

    /*
     * For Remote entries
     */
    public RobotEntry(String id, String school, String city, String state){
        this.id = id;
        this.school = school;
        this.city = city;
        this.state = state;
        this.type = EntryType.REMOTE;
    }

    /*
     * For Local entries
     */
    public RobotEntry(String port){
        this.port = port;
        this.type = EntryType.LOCAL;
    }

    /*
     * For APRS entries
     */
    public RobotEntry(String myCall, String callsignToAccept){
        this.myCallsign = myCall;
        this.callsignToAccept = callsignToAccept;
        this.type = EntryType.APRS;
    }

    public EntryType getType(){
        return type;
    }

    public String getId(){
        if (type == EntryType.REMOTE){return id;}
        if (type == EntryType.LOCAL) {return port;}
        if (type == EntryType.APRS) {return myCallsign;}
        return null;
    }

    public String toString(){
        if(type == EntryType.LOCAL){
            return port;
        }
        if(type == EntryType.APRS){
            return String.format("My callsign: %s, receiving from %s", this.myCallsign, this.callsignToAccept);
        }
        return String.format("%s\n%s\n%s\n%s", this.id, this.school, this.city, this.state);
    }

    public RobotEntry get_copy(){
        if(type == EntryType.LOCAL){
            return new RobotEntry(port);
        }
        else if(type == EntryType.APRS){
            return new RobotEntry(myCallsign, callsignToAccept);
        }
        return new RobotEntry(id, school, city, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RobotEntry)) return false;
        RobotEntry that = (RobotEntry) o;
        return this.getType() == that.getType() &&
                Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getType());
    }
}

enum EntryType {
    LOCAL,
    REMOTE,
    APRS
}