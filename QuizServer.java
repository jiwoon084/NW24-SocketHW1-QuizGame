package sec01;

import java.io.*;
import java.net.*;
import java.util.*;

public class QuizServer {
    private static int PORT = 1234; // 기본 포트 번호 (파일이 없거나 오류 발생 시 사용)
    private static String SERVER_IP = "localhost"; // 기본 서버 IP
    private static final List<Question> questions = Arrays.asList(
    	new Question("What is the capital of South Korea?", "Seoul"),
        new Question("What is the capital of Australia?", "Canberra"),
        new Question("What is 34 + 785?", "819"),
        new Question("What is the solution to the equation x + 8 = 7?", "-1"),
        new Question("What is the color of the sky on a clear day?", "Blue")
    ); // 퀴즈 질문과 정답을 저장하는 리스트

    public static void main(String[] args) {
        loadServerInfo(); // input.txt에서 IP와 포트 번호를 불러옴

        // 서버 소켓을 생성하고 클라이언트 연결을 대기
        try (ServerSocket serverSocket = new ServerSocket()) {
            InetAddress ip = InetAddress.getByName(SERVER_IP); // IP 주소 생성
            serverSocket.bind(new InetSocketAddress(ip, PORT)); // 특정 IP와 포트에 바인딩
            System.out.println("퀴즈 서버가 IP " + SERVER_IP + "와 포트 " + PORT + "에서 실행 중입니다.");

            // 무한 루프를 돌면서 클라이언트 연결을 수락
            while (true) {
                Socket clientSocket = serverSocket.accept(); // 클라이언트 연결 수락
                System.out.println("클라이언트가 연결되었습니다: " + clientSocket.getInetAddress());
                // 각 클라이언트를 새로운 스레드로 처리
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        }
    }

    // 서버 IP와 포트 번호를 input.txt 파일에서 불러오는 메소드
    private static void loadServerInfo() {
        File file = new File("server_info.txt");
        System.out.println("server_info.txt 경로: " + file.getAbsolutePath());
        if (!file.exists()) {
            System.out.println("server_info.txt 파일이 존재하지 않습니다. 기본값을 사용합니다.");
            return; // 기본값 유지
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            SERVER_IP = reader.readLine(); // 첫 줄에서 IP 주소 읽기
            String portLine = reader.readLine(); // 두 번째 줄에서 포트 번호 읽기

            // IP 유효성 검사
            if (!isValidIPAddress(SERVER_IP) || !isLocalIPAddress(SERVER_IP)) {
                System.out.println("server_info.txt에 설정된 IP 주소가 현재 컴퓨터에서 사용되지 않습니다. 기본값으로 변경합니다.");
                SERVER_IP = "localhost"; // 기본값으로 복구
            }

            // 포트 번호 유효성 검사
            try {
                PORT = Integer.parseInt(portLine);
                if (!isValidPort(PORT) || !isPortAvailable(PORT)) {
                    System.out.println("server_info.txt에 설정된 포트 번호가 유효하지 않거나 이미 사용 중입니다. 기본값으로 변경합니다.");
                    PORT = 1234; // 기본값으로 복구
                }
            } catch (NumberFormatException e) {
                System.out.println("server_info.txt에 포트 번호가 잘못된 형식입니다. 기본값으로 변경합니다.");
                PORT = 1234; // 기본값으로 복구
            }
            
         // 최종 설정 저장
            saveServerInfo(SERVER_IP, PORT);
            
            System.out.println("server_info.txt에서 서버 설정을 불러왔습니다: IP=" + SERVER_IP + ", PORT=" + PORT);
        } catch (IOException e) {
            System.out.println("server_info.txt 파일 읽기 실패. 기본값을 사용합니다. 오류: " + e.getMessage());
        }
    }

    // IP 주소 유효성 검사 메서드
    private static boolean isValidIPAddress(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    // IP 주소가 현재 컴퓨터의 네트워크 인터페이스에서 사용 가능한지 확인
    private static boolean isLocalIPAddress(String ip) {
        try {
            InetAddress targetAddress = InetAddress.getByName(ip);
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.equals(targetAddress)) {
                        return true; // 입력된 IP가 현재 컴퓨터의 네트워크 인터페이스에 존재함
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // 포트 번호 유효성 검사 메서드
    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535; // 포트 번호는 0 ~ 65535 범위 내여야 함
    }

    // 포트가 사용 가능한지 확인
    private static boolean isPortAvailable(int port) {
        try (ServerSocket tempSocket = new ServerSocket(port)) {
            tempSocket.setReuseAddress(true); // 포트 재사용 허용
            return true; // 포트가 사용 가능함
        } catch (IOException e) {
            return false; // 포트가 이미 사용 중임
        }
    }
    
    // 서버 설정을 server_info.txt에 저장
    private static void saveServerInfo(String ip, int port) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("server_info.txt"))) {
            writer.write(ip);
            writer.newLine();
            writer.write(String.valueOf(port));
        } catch (IOException e) {
            System.err.println("server_info.txt에 설정 저장 실패: " + e.getMessage());
        }
    }

    // 클라이언트 요청을 처리하는 클래스
    private static class ClientHandler implements Runnable {
        private final Socket socket; // 클라이언트 소켓
        private int score = 0; // 클라이언트 점수

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // 클라이언트와의 통신을 처리
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // 퀴즈 질문을 하나씩 클라이언트에 전송하고 응답을 평가
                for (Question question : questions) {
                    out.println("QUESTION: " + question.getQuestion()); // 질문 전송
                    String answer = in.readLine(); // 클라이언트의 응답 수신
                    if (question.isCorrect(answer)) { // 정답 여부 확인
                        score += 20; // 정답일 경우 점수 증가
                        out.println("CORRECT"); // 클라이언트에게 정답임을 알림
                    } else {
                        out.println("INCORRECT"); // 오답임을 알림
                    }
                }
                // 모든 질문이 끝난 후 최종 점수를 클라이언트에 전송하고 연결 종료
                out.println("SCORE: " + score + "/" + questions.size() * 20);
                socket.close(); // 클라이언트 소켓 닫기
            } catch (IOException e) {
                System.err.println("클라이언트 처리 오류: " + e.getMessage());
            }
        }
    }

    // 퀴즈 질문과 정답을 저장하는 클래스
    private static class Question {
        private final String question; // 질문 텍스트
        private final String answer; // 정답 텍스트

        Question(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        // 클라이언트의 응답이 정답인지 확인하는 메소드
        public boolean isCorrect(String answer) {
            return this.answer.equalsIgnoreCase(answer.trim()); // 대소문자 구분 없이 비교
        }
    }
}
