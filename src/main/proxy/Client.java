package main.proxy;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by phili on 2016-04-07.
 */
public class Client {

    Pattern typeCheck = Pattern.compile("text/");
    private boolean encodedData = false;
    private boolean isText = false;


    public Client() {
    }

   public DataContainer sendRequest(InetAddress destIp, String header, ByteArrayOutputStream data){
       try {
           ByteArrayOutputStream response = new ByteArrayOutputStream();
           Socket toWebSocket = new Socket(destIp,80);
           DataContainer returnData = new DataContainer();

           //Set up PrintWriter
           PrintWriter pw = new PrintWriter(toWebSocket.getOutputStream());
           pw.write(header);
           pw.flush();

           //If we have data to send, send it to the socket
           if(data != null){
               toWebSocket.getOutputStream().write(data.toByteArray());
               toWebSocket.getOutputStream().flush();
           }

           //Read the response and save a copy of the header in builder to examine fields
           int c;
           StringBuilder builder = new StringBuilder();
           while((c = toWebSocket.getInputStream().read()) != -1){
               response.write(c);
               if(!builder.toString().endsWith("\r\n\r\n")){
                   builder.append((char) c);
               }
           }

           //Examine fields of the header. Looking for encoding and type of data
           String[] headerFields = builder.toString().split("\r\n");
           for(String field : headerFields){
               if (field.contains("Content-Encoding")) {
                   encodedData = true;
               }
               else if (field.contains("Content-Type")) {
                   isText = typeCheck.matcher(field).find();
               }
           }

           //If the data is text and is not encoded, check for bad words
           if(isText && !encodedData){
               String s = response.toString();
               //System.out.println("SCANNING DATA--------------------------------------------------------------------------------");
               returnData.redirect = Server.refusedWords.matcher(s).find();     //If we find it, set redirect true
           }    

           //Set the return data
           returnData.responseData = response;

           //System.out.println(destIp.getHostName());

           return returnData;

       } catch (IOException e) {
           e.printStackTrace();
           return null;
       }



   }
}
