package main.proxy;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by phili on 2016-04-07.
 */
public class Server {

    public static void main(String[] args) {
        /**Scanner input = new Scanner(System.in);
        String urlS = input.nextLine();
        Pattern pattern = Pattern.compile("http[\\D]*[/]*");*/
        try {
            /**System.out.println("pattern: "+pattern.matcher(urlS).matches());
            InetAddress ip;
            if (pattern.matcher(urlS).matches()) {
                ip = InetAddress.getByName(new URL(urlS).getHost());
            } else {
                ip = InetAddress.getByName(urlS);
            }
            System.out.println("the ip: "+ip+" the url: "+urlS);*/


            System.out.println("setting upp connection");
            Socket t = new Socket(InetAddress.getByName(new URL("https://httpbin.org").getHost()),80);
            System.out.println(t.isConnected());
            PrintWriter pw = new PrintWriter(t.getOutputStream());
            pw.write("GET / HTTP/1.0\r\n\r\n");
            pw.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(t.getInputStream()));
            String s;
            System.out.println("done writing");
            while((s = br.readLine()) != null) {
                System.out.println(s);
            }
            br.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}



