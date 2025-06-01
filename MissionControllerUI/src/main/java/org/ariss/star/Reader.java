package org.ariss.star;

import java.io.*;
import java.util.ArrayList;

public class Reader {
    /*
    public void read(ArrayList<Object> list)  {
        try {
            FileReader fr = new FileReader("file.txt");
            BufferedReader br = new BufferedReader(fr);

            String str;
            while ((str = br.readLine()) != null)  {
                list.add(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     */
    public static void read(ArrayList<Object> list)  {
        try {
            list.clear();
            //ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            BufferedReader br = new BufferedReader(new FileReader("important.txt"));
            String line = br.readLine();
            while (line != null)   {
                list.add(line);
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
