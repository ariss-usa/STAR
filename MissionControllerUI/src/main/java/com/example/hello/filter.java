package com.example.hello;

import java.util.ArrayList;

class filter{
    public static ArrayList<String> filterInternetInput(ArrayList<String> list, String myMC){
        ArrayList<String> filteredArray = new ArrayList<String>();
        for(int i = 0; i < list.size(); i++){
            String [] split = list.get(i).split("\n|Online: |Connected: ");
            String online = split[6];
            String connected = split[8];
            if(online.equals("Yes")
                && connected.equals("No") && !split[0].equals(myMC)){
                    String entry = split[0] + "\n" + split[1] + "\n" + split[2] + "\n" + split[4];
                    filteredArray.add(entry);
            }
        }
        return filteredArray;
    }
    public static String filterEntry(String str, String myMC){
        String [] split = str.split("\n|Online: |Connected: ");
        String mc = split[0].substring(11, 16);
        String online = split[6];
        String connected = split[8];
        if(online.equals("Yes")
            && connected.equals("No") && !mc.equals(myMC)){
                String entry = mc + "\n" + split[1] + "\n" + split[2] + "\n" + split[4];
                return entry;
        }
        return "";
    }
}