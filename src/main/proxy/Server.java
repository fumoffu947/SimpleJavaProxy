package main.proxy;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by phili on 2016-04-07.
 */
public class Server implements Runnable {

    private final Socket socket;
    private Pattern httpPattern = Pattern.compile("http[\\D]*[/]*");
    public static Pattern refusedWords = Pattern.compile("SpongeBob|Paris.?Hilton|Norrk([oö]|%c3%96)ping|Brittney.?Spears",Pattern.CASE_INSENSITIVE);
    private boolean keepAlive = true;
    private final Client client;

    private String redirectMSG = "HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error1.html\r\n\r\n";
    private String redirectMsgResponse = "HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n";

    private boolean redirect = false;


    public static void main(String[] args) {

        if (args.length > 0) {
            try {
                ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
                if (!serverSocket.isClosed()) {
                    System.out.println("Socket was opened on port " + Integer.parseInt(args[0]));
                }
                //ServerSocket serverSocket = new ServerSocket(2500);
                int nexdServerID = 0;
                while (true) {
                    //System.out.println("Looping accept");
                    Socket currentSocket = serverSocket.accept();

                    //System.out.println("before starting new thread");
                    Thread thread = new Thread(new Server(currentSocket));
                    thread.setName("Server: " + nexdServerID);
                    thread.start();
                    nexdServerID++;
                    //System.out.println("after thread started");

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Server(Socket socket) {
        this.socket = socket;
        this.client = new Client();
    }

    @Override
    public void run() {
        try {
            while (keepAlive && socket.isConnected() && !redirect) {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String s;
                List<String> incomingHeader = new ArrayList<>();
                // read in the header
                while ((s = br.readLine()) != null) {
                    if (s.isEmpty()) {
                        break;
                    }
                    incomingHeader.add(s);
                }
                // if the header is empty or the connection was closed sett keepAlive to false and exit the loop
                if (s == null && incomingHeader.size() < 1) {
                    keepAlive = false;
                    continue;
                }

                // kill if http url do contain non-accepted words
                if (refusedWords.matcher(incomingHeader.get(0)).find()) {
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
                    printWriter.write(redirectMSG);
                    printWriter.flush();
                    redirect = true;
                    continue;
                }

                boolean dataAfter = false;
                int numberOfBytes = 0;
                StringBuilder newRequest = new StringBuilder();

                //Pick out the first line as we need to filter out host
                String[] firstLine = incomingHeader.get(0).split(" ");
                newRequest.append(firstLine[0] + " ");

                // filter the host out of the first header line
                int firstDot = firstLine[1].indexOf('.');
                int beginSlashOfAbsolutPath = firstLine[1].indexOf('/', (firstDot != -1) ? firstDot : 0);
                newRequest.append(firstLine[1].substring(beginSlashOfAbsolutPath != -1 ? beginSlashOfAbsolutPath : 0) + " ");

                // add the rest of the first line of the header
                for (int i = 2; i < firstLine.length; i++) {
                    newRequest.append(firstLine[i]);
                    if (i != firstLine.length-1) {
                        newRequest.append(" ");
                    }
                }
                newRequest.append("\r\n");

                InetAddress destAdress = null;
                //Go through the header, appending the fields to the newrequest and examine the fields we are interested in
                for (int partIndex = 1; partIndex < incomingHeader.size(); partIndex++) {
                    String partHeader = incomingHeader.get(partIndex);

                    // get ip from host header and append to string builder
                    if (partHeader.contains("Host")) {
                        // get ip
                        try {
                            destAdress = InetAddress.getByName(partHeader.split(": ")[1]);
                            newRequest.append(partHeader + "\r\n");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                            //System.out.println("trying with www.");
                            destAdress = InetAddress.getByName("www." + partHeader.split(": ")[1]);
                            String[] split = partHeader.split(": ");
                            newRequest.append(split[0] + ": " + "www." + split[1] + "\r\n");
                        }
                    }

                    // check if there is a content-length header and pars the length of data after
                    else if (partHeader.contains("Content-Length")) {
                        dataAfter = true;
                        String[] tmp = partHeader.split(":");
                        if (tmp[1].contains(" ")) {
                            numberOfBytes = Integer.parseInt(tmp[1].split(" ")[1]);
                        } else {
                            numberOfBytes = Integer.parseInt(tmp[1]);
                        }
                    } else if (partHeader.contains("Connection")) {
                        //Check keep alive, and change it in the new request to close
                        if (partHeader.split(": ")[1].toLowerCase().equals("keep-alive")) {
                            keepAlive = true;
                        } else {
                            keepAlive = false;
                        }
                        newRequest.append("Connection: close" + "\r\n");
                    }


                    // append all other part headers
                    else {
                        if (!partHeader.isEmpty()) {
                            newRequest.append(partHeader + "\r\n");
                        }
                    }
                }
                // to finnish the HTTP request
                newRequest.append("\r\n");

                ByteArrayOutputStream byteArrayOutputStream = null;
                // if the field content-length was present read the data
                if (dataAfter) {
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    for (int i = 0; i < numberOfBytes; i++) {
                        byteArrayOutputStream.write(br.read());
                    }
                    //System.out.println("ByteArrayOutputStream size                                " + byteArrayOutputStream.size());
                }


                //System.out.println(incomingHeader.toString());
                //System.out.println("");
                //System.out.println("modified request");
                //System.out.println(newRequest.toString());

                DataContainer response = client.sendRequest(destAdress, newRequest.toString(), byteArrayOutputStream);

                //If response.redirect is true, we want to redirect the browser
                if(response.redirect){
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
                    printWriter.write(redirectMsgResponse);
                    printWriter.flush();
                    redirect = true;
                    continue;
                }else{  //Otherwise, write the data to the socket
                    if(response.responseData != null){
                        socket.getOutputStream().write(response.responseData.toByteArray());
                    }
                }


                //System.out.println("done writing");
            }
            //System.out.println("A thread has ended !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            //System.out.println("exiting thrads name: "+Thread.currentThread().getName());

            //Flush and close socket as we are done with this thread
            socket.getOutputStream().flush();
            socket.getOutputStream().close();
            socket.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}



