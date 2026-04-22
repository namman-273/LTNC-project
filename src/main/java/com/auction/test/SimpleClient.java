package com.auction.test;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("--- ĐÃ KẾT NỐI SERVER ---");

            // Luồng này cực kỳ quan trọng: Luôn đợi tin nhắn UPDATE từ Server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("\n[SERVER GỬI ĐẾN]: " + response);
                        System.out.print("> Nhập lệnh: ");
                    }
                } catch (IOException e) {
                    System.out.println("Ngắt kết nối.");
                }
            }).start();

            // Luồng chính: Cho phép  nhập LOGIN, BID từ bàn phím
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String cmd = scanner.nextLine();
                out.println(cmd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
