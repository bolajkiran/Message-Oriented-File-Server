// Server Side
import java.net.*;
import java.io.*;
import java.nio.file.Paths;

public class Server {
    private static DataOutputStream toClient = null;
    private static DataInputStream fromClient = null;

    public void run() {
        try {
            int serverPort = 4020;
            ServerSocket serverSocket = new ServerSocket(serverPort);
            serverSocket.setSoTimeout(100000);

            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");

            Socket clientSocket = serverSocket.accept();
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

            toClient = new DataOutputStream(clientSocket.getOutputStream());
            fromClient = new DataInputStream(clientSocket.getInputStream());


            String basePath = Paths.get(".").toAbsolutePath().normalize().toString();
            String serverBaseDir = basePath + "/server_files/";
            File dir = new File(serverBaseDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            while(true) {
                String command = fromClient.readUTF();
                System.out.println("Server received the command: " + command);

                switch (command.trim()) {
                    case "UPLOAD":
                        String fileToSave = fromClient.readUTF();
                        System.out.println("Client sent the filename: " + fileToSave + " which it will upload!");
                        String fullPath = serverBaseDir + fileToSave.trim();
                        System.out.println("Waiting for client to upload a file...");
                        saveFile(fullPath);
                        System.out.println("Received the file: " + fileToSave + " from client!");
                        toClient.writeUTF("successfully received the file: " + fileToSave + " from client!");
                        break;
                    case "DOWNLOAD":
                        String fileToTransfer = fromClient.readUTF();
                        System.out.println("Client requested " + fileToTransfer + " to download!");
                        System.out.println("Checking for existing of file at server side: ");
                        String file = serverBaseDir + fileToTransfer.trim();
                        if (new File(file).exists()) {
                            toClient.writeInt(1);
                            System.out.println("File exists at server side!");
                            toClient.writeUTF("file: " + fileToTransfer + " exists at server side!");
                            System.out.println("Sending file to client...");
                            transferFileToClient(file);
                            System.out.println("Sent the file to client!");
                        } else  {
                            toClient.writeInt(0);
                            toClient.writeUTF("file: " + fileToTransfer + " does not exists at server side!");
                        }

                        break;
                    case "DELETE":
                        String filename = null;
                        System.out.println("Waiting for client to provide the file to delete...");
                        filename = fromClient.readUTF();
                        System.out.println("Server received the filename: " + filename + " to delete.");
                        System.out.println("Checking for file existence...");
                        File fullFilePath = new File(serverBaseDir + filename);
                        if (fullFilePath.exists()) {
                            System.out.println("File " + filename + " exists!");
                            toClient.writeInt(1);
                            System.out.println("file: " + filename + " exists!\uD83D\uDE00");
                            toClient.writeUTF("file: " + filename + " exists!\uD83D\uDE00");
                            fullFilePath.delete();
                            System.out.println("Successfully deleted the file: " + filename);
                            toClient.writeUTF("successfully deleted the file: " + filename);
                            break;
                        } else {
                            toClient.writeInt(0);
                            toClient.writeUTF("file " + filename + " does not exist!");
                            System.out.println("File does not exist in " + serverBaseDir);
                        }
                        break;
                    case "RENAME":
                        System.out.println("Waiting for client to provide the file to rename...");
                        filename = fromClient.readUTF();
                        System.out.println("Server received the filename: " + filename + " to delete.");
                        System.out.println("Waiting for client to provide the new filename to the file: " + filename + "...");
                        String new_filename = fromClient.readUTF();
                        System.out.println("Server received the new filename: " + new_filename + " to rename file:" + filename);
                        File fileToRename = new File(serverBaseDir + filename.trim());
                        File newFile = new File(serverBaseDir + new_filename.trim());
                        if (fileToRename.exists()) {
                            System.out.println("File " + filename + " exists!");
                            toClient.writeInt(1);
                            toClient.writeUTF("file: " + filename + " exists!\uD83D\uDE00");
                            System.out.println("Renaming the file...");
                            fileToRename.renameTo(newFile);
                            System.out.println("Successfully renamed the file: " + filename + " to " + new_filename);
                            toClient.writeUTF("successfully rename the file: " + filename + " to " + new_filename);
                            break;
                        } else {
                            toClient.writeInt(0);
                            System.out.println("File does not exist in " + serverBaseDir);
                            toClient.writeUTF("file: " + filename + " does not exist!");
                        }
                        break;
                    case "EXIT":
                        break;
                }
            }
        } catch(IOException ex) {
            System.out.println("Session timed out!");
        }
    }

    public static void saveFile(String fileToSave) {
        try {
            int bytes = 0;
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);

            long size = fromClient.readLong();     // read file size
            byte[] buffer = new byte[4*1024];
            while (size > 0 && (bytes = fromClient.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer,0,bytes);
                size -= bytes;      // read upto file size
            }
            fileOutputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void transferFileToClient(String fileToTransfer) {
        try {
            int bytes = 0;
            File file = new File(fileToTransfer);
            FileInputStream fileInputStream = new FileInputStream(file);

            // send file size
            toClient.writeLong(file.length());
            // break file into chunks
            byte[] buffer = new byte[4*1024];
            while ((bytes=fileInputStream.read(buffer))!=-1){
                toClient.write(buffer,0, bytes);
                toClient.flush();
            }
            fileInputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}

