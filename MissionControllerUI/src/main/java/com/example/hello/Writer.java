package com.example.hello;

import java.io.*;
import java.util.ArrayList;

public class Writer {
    /*
    public void write(ArrayList<Object> list) {
        try {
            FileWriter fw = new FileWriter("file.txt");
            PrintWriter pw = new PrintWriter(fw);

            for (int i = 0; i < list.size(); i++)   {
                pw.println(list.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     */

    public static void write(ArrayList<Object> list) {
        try {
            //FileOutputStream fileOut = new FileOutputStream("important.txt");
            //ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            PrintWriter pw = new PrintWriter(new FileWriter("important.txt", false));
            for (int i = 0; i < list.size(); i++)   {
                pw.print(list.get(i).toString() + "\n");
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
