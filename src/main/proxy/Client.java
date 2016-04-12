package main.proxy;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by phili on 2016-04-07.
 */
public class Client {

    public Client() {

    }

   public ByteArrayOutputStream sendRequest(InetAddress destIp, String header, ByteArrayOutputStream data){
       try {
           ByteArrayOutputStream response = new ByteArrayOutputStream();
           Socket toWebSocket = new Socket(destIp,80);
           PrintWriter pw = new PrintWriter(toWebSocket.getOutputStream());

           pw.write(header);
           pw.flush();
           //pw.close();

           if(data != null){
               toWebSocket.getOutputStream().write(data.toByteArray());
               toWebSocket.getOutputStream().flush();
           }

           int s;
           while((s = toWebSocket.getInputStream().read()) != -1){

               response.write(s);
               System.out.print((char) s);
           }

           return response;

       } catch (IOException e) {
           e.printStackTrace();
           return null;
       }



   }
}
