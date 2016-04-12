package main.proxy;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by phili on 2016-04-07.
 */
public class Server implements Runnable {

    private final Socket socket;
    private Pattern httpPattern = Pattern.compile("http[\\D]*[/]*");
    private static Pattern refusedWords = Pattern.compile("SpongeBob|Paris Hilton|Norrk\\?\\?ping|Brittney Spears",Pattern.CASE_INSENSITIVE);
    private boolean keepAlive;
    private final Client client;

    public static void main(String[] args) {
        /*Scanner input = new Scanner(System.in);
        String urlS = input.nextLine();
        Pattern pattern = Pattern.compile("http[\\D]*[/]*");
        try {
            System.out.println("pattern: "+pattern.matcher(urlS).matches());
            InetAddress ip;
            if (pattern.matcher(urlS).matches()) {
                ip = InetAddress.getByName(new URL(urlS).getHost());
            } else {
                ip = InetAddress.getByName(urlS);
            }
            System.out.println("the ip: "+ip+" the url: "+urlS);

            System.out.println("localHostIp: "+InetAddress.getLocalHost());

            System.out.println("setting upp connection");
            Socket t = new Socket(InetAddress.getByName(new URL("https://httpbin.org").getHost()),80);
            System.out.println(t.isConnected());
            PrintWriter pw = new PrintWriter(t.getOutputStream());
            pw.write("GET /ip HTTP/1.0\r\nConnection: Keep-Alive\r\n\r\n");
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
        }*/

        //System.out.println(refusedWords.matcher("beep boop a81378/(%/# norrk??ping lel kek //").find());

        try {
            //ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            ServerSocket serverSocket = new ServerSocket(2500);
            while(true) {
                System.out.println("Looping accept");
                Socket currentSocket = serverSocket.accept();
                Server server = new Server(currentSocket);
                server.run();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Server(Socket socket) {
        this.socket = socket;
        this.client = new Client();
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            int s;
            StringBuilder builder = new StringBuilder();
            int carriageReturnsFound = 0;
            while((s = br.read()) != -1){
                //Check for carriageReturn and keep count of them
                switch(s){
                    case 13:
                        carriageReturnsFound++;
                        break;
                    case 10:
                        break;
                    default:
                        carriageReturnsFound = 0;
                        break;
                }

                builder.append((char) s);

                //If we have read 2 carriage returns, we are done with this request
                if(carriageReturnsFound == 2){
                    break;
                }
            }

            String[] request = builder.toString().split("\r\n");
            boolean dataAfter = false;
            int numberOfBytes = 0;
            StringBuilder newRequest = new StringBuilder();
            String[] firstLine = request[0].split(" ");
            newRequest.append(firstLine[0] + " ");
            if(firstLine[1].charAt(0) == '/') {
                newRequest.append((firstLine[1]));
            }else{
                boolean dotFound = false;
                for (int i = 0; i < firstLine[1].length(); i++) {
                    if(!dotFound && firstLine[1].charAt(i) == '.'){
                        dotFound = true;
                    }
                    if(dotFound && firstLine[1].charAt(i) == '/'){
                        newRequest.append(firstLine[1].substring(i)+" ");
                        break;
                    }
                }
                if (!dotFound) {
                    throw new MalformedURLException();
                }
            }

            for (int i = 2; i < firstLine.length; i++) {
                newRequest.append(firstLine[i]+" ");
            }
            newRequest.append("\r\n");

            InetAddress destAdress = null;
            for(int partIndex = 1; partIndex < request.length;partIndex++) {
                String partHeader = request[partIndex];

                // get ip from host header and append to string builder
                if (partHeader.contains("Host")) {
                    // get ip
                    if (partHeader.contains("www.")) {
                        destAdress = InetAddress.getByName(partHeader.split(": ")[1]);
                    } else {
                        if(Pattern.compile(".*\\..*\\..*",Pattern.CASE_INSENSITIVE).matcher(partHeader).matches()){
                            destAdress = InetAddress.getByName(partHeader.split(": ")[1]);
                        }else {
                            destAdress = InetAddress.getByName("www." + partHeader.split(": ")[1]);
                        }
                    }
                    newRequest.append(partHeader+"\r\n");
                }

                // check if there is a content-lenght header and pars the length of data after
                else if (partHeader.contains("Content-Length")) {
                    dataAfter = true;
                    String[] tmp = partHeader.split(":");
                    if (tmp[1].contains(" ")) {
                        numberOfBytes = Integer.parseInt(tmp[1].split(" ")[1]);
                    }else {
                        numberOfBytes = Integer.parseInt(tmp[1]);
                    }
                }

                else if (partHeader.contains("Connection")) {
                    if (partHeader.split(": ")[1].toLowerCase().equals("keep-alive")){
                        keepAlive = true;
                    }else{
                        keepAlive = false;
                    }
                    newRequest.append("Connection: close" + "\r\n");
                }


                // append all other part headers
                else {
                    newRequest.append(partHeader+"\r\n");
                }
            }
            newRequest.append("\r\n");
            ByteArrayOutputStream byteArrayOutputStream = null;
            // if the field content-length was present read the data
            if (dataAfter) {
                byteArrayOutputStream = new ByteArrayOutputStream();
                for (int i = 0; i < numberOfBytes; i++) {
                    byteArrayOutputStream.write(br.read());
                }
            }


            System.out.println(builder.toString());
            System.out.println("");
            System.out.println("modified request");
            System.out.println(newRequest.toString());

            ByteArrayOutputStream response = client.sendRequest(destAdress, newRequest.toString(), byteArrayOutputStream);

            socket.getOutputStream().write(response.toByteArray());
            //Filter here ( Later )
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}



