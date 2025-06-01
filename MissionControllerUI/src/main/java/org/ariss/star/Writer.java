package org.ariss.star;

import java.io.*;
import java.util.ArrayList;

public class Writer {
    public static void write(ArrayList<Object> list) {
        try {
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
