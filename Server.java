import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.IntStream;

public class Server implements Runnable {
    private static boolean successVote = false;
    private static int port;
    private static int VN = 0;
    private static int RU = 8;
    private static int DS = 0;
    private static int serverID;
    private static int level = 0;
    private static int serverNum = 8;
    private static int finishWrite = 0;
    private static String filePath = "/home/012/y/yx/yxm180012/cs6378/proj3/";
    private static ServerSocket ssocket;

    public static HashMap<Integer,Socket> socketMap = new HashMap<Integer,Socket>();
    public static HashMap<Socket,PrintWriter> outs = new HashMap<Socket,PrintWriter>();
    public static HashMap<Socket,BufferedReader> ins = new HashMap<Socket,BufferedReader>();

    public static HashMap<Character, Integer> nameMap = new HashMap<Character, Integer>() {
        private static final long serialVersionUID = 1L;
        {
            put('A',0);
            put('B',1);
            put('C',2);
            put('D',3);
            put('E',4);
            put('F',5);
            put('G',6);
            put('H',7);
        }
    };

    //serverID, level, ReachableServer
    private static int[][][] components = new int[][][] {
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {0},{0}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {7}, {7}}
    };

    public static HashMap<Integer, String> serverInfo = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1L;
        {
            put(0,"dc10.utdallas.edu");
            put(1,"dc11.utdallas.edu");
            put(2,"dc12.utdallas.edu");
            put(3,"dc13.utdallas.edu");
            put(4,"dc14.utdallas.edu");
            put(5,"dc15.utdallas.edu");
            put(6,"dc16.utdallas.edu");
            put(7,"dc17.utdallas.edu");
        }
    };

    public static Random rand = new Random();

    public Server(int serverID, int port) {
        this.serverID = serverID;
        this.port = port;

        // wait servers
        try {
            ssocket = new ServerSocket(port);
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        initConnection();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {

       //start threads to read sockets
        for (Socket socket : socketMap.values()) {
            Thread t = new Thread(new MsgHandler(socket));
            t.start();
        }

        // send completion notification after into cs 20 times
        while (level < 1)
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (finishWrite < 2) {
                if (startVote()) {
                    request2vote();
                }
            }
            if (successVote) {
                DS = components[serverID][level][0];
                RU = components[serverID][level].length;
                successVote = false;
            }

            finishWrite = 0;
            level++;
        }
    }

    private boolean startVote() {
        if (finishWrite >= 2) {
            return false;
        }
        if (level == 0 && serverID == 0) {
            return true;
        }
        if (level == 1 && (serverID == 0 || serverID == 4)) {
            return true;
        }
        if (level == 2 && (serverID == 0 || serverID == 1 || serverID == 4 || serverID == 7)) {
            return true;
        }
        if (level == 3 && serverID == 0 || serverID == 1|| serverID == 7) {
            return true;
        }
        return false;
    }

    private void request2vote() {
        int[] ServersInComponent = components[serverID][level];

        String reqMsg;

        reqMsg = "Write Level " + level + " Start Server " + getKey(nameMap,serverID) + " Other Cleint " + ServersInComponent;

        for (int c: ServersInComponent) {
            msgToServer(c, reqMsg);
        }

    }

    public static class MsgHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;

        public MsgHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public synchronized void run() {
            try {
                String inputLine;
                in = ins.get(socket);

                while ((inputLine = in.readLine()) != null) {
                    String[] splitStr;

                    System.out.println("Server " + getKey(nameMap,serverID) + " received msg: (" + inputLine + ") from  " + socket.getRemoteSocketAddress());
                    splitStr = inputLine.split("\\s+");

                    if ("Write".equals(splitStr[0])) {
                        int level = Integer.parseInt(splitStr[2]);
                        int fromServer = Integer.parseInt(splitStr[5]);
                        if (valideVote(fromServer, level)) {
                            VN ++;
                            successVote = true;
                            appendStrToFile(filePath.concat(Integer.toString(serverID)) + "/X.txt", inputLine);
                        }
                        finishWrite ++;
                    } else if ("END".equals(splitStr[0])) {
                        if (serverID == 1) {
                            for (PrintWriter outf : outs.values()) {
                                outf.println("END");
                                outf.flush();
                            }
                        }
                        stop();
                    } else {
                        System.out.println("Command Not Yet Support!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean valideVote(int level, int fromServer) {
        int[] ServersInComponent = components[fromServer][level];

        if (ServersInComponent.length * 2 == RU) {
            return Arrays.asList(ServersInComponent).contains(DS);
        }
        if (ServersInComponent.length * 2 > RU) {
            return true;
        }
        return false;
    }

    public synchronized static void msgToServer(int Server, String msg) {
        System.out.println("Send (" + msg + ") to server " + Server + " " + socketMap.get(Server).getRemoteSocketAddress());
        PrintWriter out = outs.get(socketMap.get(Server));
        out.println(msg);
        out.flush();
    }

    public static void appendStrToFile(String fileName, String str) {
        try {
            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(str);
            out.write("\n");
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("exception occoured" + e);
        }
    }

    public static void initConnection () {
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        try {
            for (int i = 0; i < serverNum; i++) {
                if (i != serverID) {
                    socket = new Socket(serverInfo.get(i), port);
                    System.out.println("Server " + getKey(nameMap,serverID) + " connects to " + socket.getRemoteSocketAddress());
                    socketMap.put(i,socket);

                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outs.put(socket, out);
                    ins.put(socket, in);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // intput: components after partition
    public static void partition (int[] componentA, int[] componentB) {
        if (contains(componentA,serverID)) {
            for (int server : componentB) {
                try {
                    outs.get(socketMap.get(server)).close();
                    outs.remove(socketMap.get(server));
                    ins.get(socketMap.get(server)).close();
                    ins.remove(socketMap.get(server));
                    socketMap.get(server).close();
                    socketMap.remove(server);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

   // intput: components need to merge
    public static void merge (int[] componentA, int[] componentB) {
        if (contains(componentA,serverID)) {
            for (int server : componentB) {
                try {
                    Socket socket = new Socket(serverInfo.get(server),port);
                    System.out.println("Server " + getKey(nameMap,serverID) + " connects to " + socket.getRemoteSocketAddress());
                    socketMap.put(server,socket);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outs.put(socket, out);
                    ins.put(socket, in);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean contains(final int[] array, final int v) {
        boolean result = false;
        for(int i : array){
            if(i == v){
                result = true;
                break;
            }
        }
        return result;
    }

    public static void stop() {
        System.out.println("Server " + getKey(nameMap,serverID) + " closing itself");
        try {
            for (BufferedReader in : ins.values()) {
                in.close();
            }
            for (PrintWriter out : outs.values()) {
                out.close();
            }
            for (Socket socket : socketMap.values()) {
                socket.close();
            }
            Thread.sleep(100);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (K key : map.keySet()) {
            if (value.equals(map.get(key))) {
                return key;
            }
        }
        return null;
    }

    // need user input of id
    // port(better be 49152 to 65535)
    public static void main(String[] args) throws IOException {

        // set basic param
        if (args.length > 0){
            try{
                // get start
                Server c = new Server(nameMap.get(args[0].charAt(0)), Integer.parseInt(args[1]));
                Thread t = new Thread(c);
                t.start();
            } catch (NumberFormatException e){
                System.err.println("Argument must be an integer");
                System.exit(1);
            }
        }
    }


}