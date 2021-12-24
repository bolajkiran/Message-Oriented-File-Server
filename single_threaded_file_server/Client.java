// Client Side

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    private static DataOutputStream toServer = null;
    private static DataInputStream fromServer = null;
    private static String basePath = null;

    public void run() {
        try {
            int serverPort = 4020;
            InetAddress host = InetAddress.getByName("localhost");

            System.out.println("Connecting to server on port " + serverPort);

            Socket socket = new Socket(host, serverPort);
            //Socket socket = new Socket("127.0.0.1", serverPort);
            System.out.println("Just connected to " + socket.getRemoteSocketAddress());

            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);

            basePath = Paths.get(".").toAbsolutePath().normalize().toString();
            String clientBaseDir = basePath + "/client_files/";

            loop:
            while (true) {
                System.out.println("File Server supports four basic operations: UPLOAD, DOWNLOAD, DELETE and RENAME.");
                System.out.println("\t __________");
                System.out.println("\t| COMMANDS |");
                System.out.println("\t|__________|");
                System.out.println("\t| UPLOAD   |");
                System.out.println("\t| DOWNLOAD |");
                System.out.println("\t| DELETE   |");
                System.out.println("\t| RENAME   |");
                System.out.println("\t| EXIT     |");
                System.out.println("\t|__________|");

                System.out.println("\nPlease enter the command to send to server:");
                String command = scanner.nextLine();
                System.out.println("Sending " + command + " to server...");

                command = command.trim().toUpperCase();
                toServer.writeUTF(command);

                switch (command) {
                    case "UPLOAD":
                        System.out.println("Listing the files for you: ");
                        File dir = new File(clientBaseDir);
                        File[] files = dir.listFiles();
                        System.out.println("Listing files to upload in: " + clientBaseDir);
                        for (File file : files) {
                            System.out.print(file.getName() + "   ");
                        }
                        System.out.println();
                        String fileToUpload = null;
                        String fullPath = null;
                        while (true) {
                            System.out.println("Enter the filename to UPLOAD (ex. file1.txt):");
                            fileToUpload = scanner.nextLine();
                            fullPath = clientBaseDir + fileToUpload.trim();
                            if (new File(fullPath).exists()) {
                                break;
                            } else {
                                System.out.println("Incorrect filename entered, file does not exist to upload it to server!!");
                            }
                        }

                        toServer.writeUTF(fileToUpload);
                        System.out.println("Uploading a file to the server...");
                        uploadFile(fullPath);
                        System.out.println("Successfully uploaded " + fileToUpload + " to the server!");
                        System.out.println("Server says - " + fromServer.readUTF());
                        break;
                    case "DOWNLOAD":
                        System.out.println("Enter the filename to download (ex. file1.txt):");
                        String fileToDownload = scanner.nextLine();
                        toServer.writeUTF(fileToDownload);
                        System.out.println("Sending filename - " + fileToDownload + " to server...");
                        int fileStatus = fromServer.readInt();
                        if (fileStatus == 1) {
                            System.out.println("Server says - " + fromServer.readUTF());
                            System.out.println("Downloading a file from the server...");
                            downloadFileFromServer(fileToDownload);
                            System.out.println("Successfully downloaded the " + fileToDownload + " from the server!");
                            break;
                        } else {
                            System.out.println("Server says - " + fromServer.readUTF());
                        }
                        break;
                    case "DELETE":
                        System.out.println("Enter the filename to delete: ");
                        String file = scanner.nextLine();
                        System.out.println("Sending filename " + file + " to the server for delete operation...");
                        toServer.writeUTF(file);
                        int fileExistStatus = fromServer.readInt();
                        if (fileExistStatus == 1) {
                            System.out.println("Server says - " + fromServer.readUTF());
                            System.out.println("Server says - " + fromServer.readUTF());
                            break;
                        } else {
                            System.out.println("Server says - " + fromServer.readUTF());
                        }

                        break;
                    case "RENAME":
                        System.out.println("Enter the filename to rename: ");
                        file = scanner.nextLine();
                        System.out.println("Sending filename " + file + " to the server for rename operation...");
                        toServer.writeUTF(file);
                        System.out.println("Enter the new name to rename the file: " + file);
                        String rename_file = scanner.nextLine();
                        System.out.println("Sending new filename " + rename_file + " to the server for rename operation...");
                        toServer.writeUTF(rename_file);
                        fileExistStatus = fromServer.readInt();
                        if (fileExistStatus == 1) {
                            System.out.println("Server says - " + fromServer.readUTF());
                            System.out.println("Server says - " + fromServer.readUTF());
                        } else {
                            System.out.println("Server says - " + fromServer.readUTF());
                        }
                        break;
                    case "EXIT":
                        break loop;
                }
            }

            System.out.println("");
            toServer.close();
            fromServer.close();
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void downloadFileFromServer(String fileToDownload) {
        String downloadDir = basePath + "/downloads";
        File dir = new File(downloadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fullPath = downloadDir + "/" + fileToDownload;
        try {
            int bytes = 0;
            FileOutputStream fileOutputStream = new FileOutputStream(fullPath);

            long size = fromServer.readLong();     // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = fromServer.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;      // read upto file size
            }
            fileOutputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void uploadFile(String fileToUpload) {
        try {
            int bytes = 0;
            File file = new File(fileToUpload);
            FileInputStream fileInputStream = new FileInputStream(file);

            // send file size
            toServer.writeLong(file.length());
            // break file into chunks
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                toServer.write(buffer, 0, bytes);
                toServer.flush();
            }
            fileInputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}

