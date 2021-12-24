import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Server class
class Server {
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static void main(String[] args)
    {
        ServerSocket server = null;

        try {

            // server is listening on port 6024
            server = new ServerSocket(1234);

            // running infinite loop for getting
            System.out.println("Waiting for any client to connect...");
            // client request
            while (true) {

                // socket object to receive incoming client
                // requests
                Socket client = server.accept();

                // Displaying that new client is connected
                // to server
                //System.out.println("New client connected" + client.getInetAddress().getHostAddress());
                System.out.println("New client connected: "
                        + client.getRemoteSocketAddress());
                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                DataInputStream fromClient = new DataInputStream(client.getInputStream());

                // create a new thread object
                ClientHandler clientSocket
                        = new ClientHandler(client, toClient, fromClient);

                // This thread will handle the client
                // separately
                executor.execute(clientSocket);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ClientHandler class
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private DataOutputStream toClient = null;
        private DataInputStream fromClient = null;

        // Constructor
        public ClientHandler(Socket socket, DataOutputStream toClient, DataInputStream fromClient) throws IOException {
            this.clientSocket = socket;
            this.toClient = toClient;
            this.fromClient = fromClient;
        }

        public void run() {
            try {

                System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());


                String basePath = Paths.get(".").toAbsolutePath().normalize().toString();
                String serverBaseDir = basePath + "/server_files/";
                File dir = new File(serverBaseDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                loop: while(true) {
                    String command = fromClient.readUTF();
                    System.out.println("Server received the command: " + command + " from client: " + clientSocket.getRemoteSocketAddress());

                    switch (command.trim()) {
                        case "UPLOAD":
                            String fileToSave = fromClient.readUTF();
                            System.out.println("Client:" + clientSocket.getRemoteSocketAddress() + " sent the filename: " + fileToSave + " which it will upload!");
                            String fullPath = serverBaseDir + fileToSave.trim();
                            System.out.println("Waiting for client:" + clientSocket.getRemoteSocketAddress() + " to upload a file...");
                            saveFile(fullPath, fromClient);
                            System.out.println("Received the file: " + fileToSave + " from client:" + clientSocket.getRemoteSocketAddress());
                            toClient.writeUTF("successfully received the file: " + fileToSave + " from client:" + clientSocket.getRemoteSocketAddress());
                            toClient.flush();
                            break;
                        case "DOWNLOAD":
                            String fileToTransfer = fromClient.readUTF();
                            System.out.println("Client:" + clientSocket.getRemoteSocketAddress() + " requested " + fileToTransfer + " to download!");
                            System.out.println("Checking for existing of file at server side: ");
                            String file = serverBaseDir + fileToTransfer.trim();
                            if (new File(file).exists()) {
                                toClient.writeInt(1);
                                toClient.flush();
                                System.out.println("File exists at server side!");
                                toClient.writeUTF("file: " + fileToTransfer + " exists at server side!");
                                toClient.flush();
                                System.out.println("Sending file to client:" + clientSocket.getRemoteSocketAddress());
                                transferFileToClient(file, toClient);
                                System.out.println("Sent the file to client:" + clientSocket.getRemoteSocketAddress());
                            } else  {
                                toClient.writeInt(0);
                                toClient.flush();
                                toClient.writeUTF("file: " + fileToTransfer + " does not exists at server side!");
                                toClient.flush();
                            }

                            break;
                        case "DELETE":
                            String filename = null;
                            System.out.println("Waiting for client:" + clientSocket.getRemoteSocketAddress() + " to provide the file to delete...");
                            filename = fromClient.readUTF();
                            System.out.println("Server received the filename: " + filename + " to delete from client: " + clientSocket.getRemoteSocketAddress());
                            System.out.println("Checking for file existence...");
                            File fullFilePath = new File(serverBaseDir + filename);
                            if (fullFilePath.exists()) {
                                System.out.println("File " + filename + " exists!");
                                toClient.writeInt(1);
                                toClient.flush();
                                System.out.println("file: " + filename + " exists!\uD83D\uDE00");
                                toClient.writeUTF("file: " + filename + " exists!\uD83D\uDE00");
                                toClient.flush();
                                fullFilePath.delete();
                                System.out.println("Successfully deleted the file: " + filename);
                                toClient.writeUTF("successfully deleted the file: " + filename);
                                toClient.flush();
                                break;
                            } else {
                                toClient.writeInt(0);
                                toClient.flush();
                                toClient.writeUTF("file " + filename + " does not exist!");
                                toClient.flush();
                                System.out.println("File does not exist in " + serverBaseDir);
                            }
                            break;
                        case "RENAME":
                            System.out.println("Waiting for client:" + clientSocket.getRemoteSocketAddress() + " to provide the file to rename...");
                            filename = fromClient.readUTF();
                            System.out.println("Server received the filename: " + filename + " to delete.");
                            System.out.println("Waiting for client:" + clientSocket.getRemoteSocketAddress() + " to provide the new filename to the file: " + filename + "...");
                            String new_filename = fromClient.readUTF();
                            System.out.println("Server received the new filename: " + new_filename + " to rename file:" + filename + " from client: " + clientSocket.getRemoteSocketAddress());
                            File fileToRename = new File(serverBaseDir + filename.trim());
                            File newFile = new File(serverBaseDir + new_filename.trim());
                            if (fileToRename.exists()) {
                                System.out.println("File " + filename + " exists!");
                                toClient.writeInt(1);
                                toClient.flush();
                                toClient.writeUTF("file: " + filename + " exists!\uD83D\uDE00");
                                toClient.flush();
                                System.out.println("Renaming the file...");
                                fileToRename.renameTo(newFile);
                                System.out.println("Successfully renamed the file: " + filename + " to " + new_filename);
                                toClient.writeUTF("successfully rename the file: " + filename + " to " + new_filename);
                                toClient.flush();
                                break;
                            } else {
                                toClient.writeInt(0);
                                toClient.flush();
                                System.out.println("File does not exist in " + serverBaseDir);
                                toClient.writeUTF("file: " + filename + " does not exist!");
                                toClient.flush();
                            }
                            break;
                        case "EXIT":
                            break loop;
                    }
                }
            } catch(IOException ex) {
                System.out.println("Session timeout!");
            } finally {
                try {
                    toClient.close();
                    fromClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public static void saveFile(String fileToSave, DataInputStream fromClient) {
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

        public static void transferFileToClient(String fileToTransfer, DataOutputStream toClient) {
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
                System.out.println("Session timed out!");
            }
        }
    }

}

