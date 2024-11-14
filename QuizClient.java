package sec01;

import java.io.*;
import java.net.*;

public class QuizClient {
    private static String SERVER_IP = "localhost"; // 기본 서버 IP
    private static int PORT = 1234; // 기본 포트 번호

    public static void main(String[] args) {
        loadServerInfo(); // 서버 정보를 input.txt에서 불러오기

        try (Socket socket = new Socket(SERVER_IP, PORT); // 서버에 연결
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.printf("퀴즈 서버에 서버 IP %s와 포트 번호 %d로 연결되었습니다!\n", SERVER_IP, PORT);

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.startsWith("QUESTION:")) { 
                    // 서버에서 질문을 수신
                    System.out.println(serverMessage);
                    System.out.print("정답 입력: ");
                    String answer = consoleInput.readLine();
                    out.println(answer); // 사용자의 답변을 서버로 전송
                } else if (serverMessage.startsWith("SCORE:")) {
                    // 퀴즈 종료 후 최종 점수 수신
                    System.out.println("퀴즈가 종료되었습니다. " + serverMessage);
                    break;
                } else {
                    System.out.println("서버: " + serverMessage); // 기타 서버 메시지 출력
                }
            }
        } catch (IOException e) {
            System.err.println("클라이언트 오류: " + e.getMessage());
        }
    }

    // QuizServer에서 검토하고 결정한 server_info.txt 파일에서 서버 IP와 포트 번호를 불러오는 메소드
    private static void loadServerInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("server_info.txt"))) {
            SERVER_IP = reader.readLine(); // 첫 줄에서 IP 주소 읽기
            PORT = Integer.parseInt(reader.readLine()); // 두 번째 줄에서 포트 번호 읽기
        } catch (IOException | NumberFormatException e) {
            System.out.println("server_info.txt 파일이 없거나 포트 번호 형식이 잘못되었습니다. 기본값을 사용합니다.");
        }
    }
}
