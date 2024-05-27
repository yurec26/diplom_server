package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {
    public static Integer PORT = 8080; // default is 8080
    public static Map<Socket, String> activeConnections = new HashMap<>();
    public static BlockingQueue<String> massagesStack = new ArrayBlockingQueue<>(10);
    public static FileWriter writer;
    public static Scanner scanner;

    public static void main(String[] args) throws IOException {
        // файл настроек. Единственная строчка - порт (обязательно).
        File fileSettings = new File("C:/Users/Юрий/IdeaProjects/diplom_2/server/src/main/resources/settings.txt");
        File filelog = new File("C:/Users/Юрий/IdeaProjects/diplom_2/server/src/main/resources/log.txt");

        choosePort(fileSettings);
        sendMsgThread();
        displayMsgThread(filelog);
        serverMain();
    }

    public static void sendMsgThread() {
        scanner = new Scanner(System.in);
        // поток отправки сообщений всем пользователям в чат
        new Thread(() -> {
            while (true) {
                String adminMassage = scanner.nextLine();
                try {
                    massagesStack.put("admin: " + adminMassage);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void displayMsgThread(File log) {
        // поток вывода на экран сообщений для всех пользователей и админа
        new Thread(() -> {
            while (true) {
                try {
                    String massage = massagesStack.take();
                    logFile(log, massage);
                    System.out.println(massage);
                    for (Socket socket : activeConnections.keySet()) {
                        try {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(massage);
                        } catch (IOException e) {
                            return;
                        }
                    }
                } catch (InterruptedException | ConcurrentModificationException e) {
                    System.out.println("something wrong with atomic collection");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void serverMain() throws IOException {
        // стартуем сервер
        try (ServerSocket socket = new ServerSocket(PORT)) {
            System.out.println("start server");
            awaitingConnectionMainLoop(socket);
        }
    }

    public static void awaitingConnectionMainLoop(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket socketClient = serverSocket.accept();
                System.out.println("new connection");
                // есть новое поделючение, распараллеливаем его в новый поток для работы с новым клиентом
                new Thread(() -> {
                    try (PrintWriter out = new PrintWriter(socketClient.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()))) {
                        // приветствие пользователя
                        out.println("admin: " + "Write your name: ");
                        try {
                            String infoFromClient = in.readLine();
                            if (!infoFromClient.equals("exit")) {
                                activeConnections.put(socketClient, infoFromClient);
                                out.println("admin: " + "Hello, " + infoFromClient + ", you can start chatting:");
                                out.println("----------------------");
                                massagesStack.put("admin : user " + infoFromClient + " entered the chat.");
                            } else {
                                System.out.println("user exit without log in");
                            }
                        } catch (InterruptedException | SocketException e) {
                            Thread.currentThread().interrupt();// елси сразу выключать
                        }
                        // цикл получения сообщений для обработки в очередь
                        receiveMsgLoopForServer(socketClient, in);
                        //
                    } catch (SocketException | InterruptedException e) {

                        activeConnections.remove(socketClient);
                        System.out.println("user disconnected immediately, thread interrupted");
                    } catch (IOException ignored) {
                    }
                }).start();
            } catch (IOException e) {
                return;
            }
        }
    }

    public static void receiveMsgLoopForServer(Socket socketClient, BufferedReader in) throws InterruptedException {
        while (true) {
            try {
                String massage = in.readLine();
                if (massage.equals("exit")) {
                    massagesStack.put("admin: " + activeConnections.get(socketClient) + " disconnected from chat");
                    activeConnections.remove(socketClient);
                    Thread.currentThread().interrupt();
                    break;
                }
                massagesStack.put(activeConnections.get(socketClient) + ": " + massage);
            } catch (NullPointerException e) {
                break;
            } catch (IOException e) {
                massagesStack.put("admin: " + activeConnections.get(socketClient) + " disconnected from chat");
                System.out.println(activeConnections.get(socketClient) + " broke the connection");
                activeConnections.remove(socketClient);
                break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /////////////////// file methods

    public static String choosePort(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            writeSettingsFile(file);
        } else {
            scanner = new Scanner(file);
            if (!scanner.hasNextLine()) {
                writeSettingsFile(file);
            } else {
                String line = scanner.nextLine();
                String systemMsd = ("Current port is: " + line);
                System.out.println(systemMsd);
                PORT = Integer.parseInt(line);
                return systemMsd;
            }
        }
        return null;
    }

    public static void writeSettingsFile(File file) throws IOException {
        scanner = new Scanner(System.in);
        System.out.print("Please set the connecting port: ");
        String newPort = scanner.nextLine();
        PORT = Integer.parseInt(newPort);
        writeFile(file, false, newPort);
    }

    public static void logFile(File file, String massage) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = now.format(formatter);
        writeFile(file, true, "'" + massage + "' " + formattedTime + "\n");
    }

    public static void writeFile(File file, boolean append, String msg) throws IOException {
        writer = new FileWriter(file, append);
        writer.write(msg);
        writer.close();
    }

}
