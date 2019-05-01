
import java.net.*;
import java.io.*;

class Hash {

    public static int arr[] = new int[10000];

    public static int get(int key) {
        return arr[key];
    }

    public static void put(int key, int value) {
        arr[key] = value;
    }
}

class Int {

    public static int returnValue[] = new int[1];

    public static int get() {
        return returnValue[0];
    }

    public static void put(int value) {
        returnValue[0] = value;
    }
}

class ClientHandler implements Runnable {

    Thread TCH;
    private Socket socket = null;
    private DataInputStream in;
    private DataOutputStream out;
    private int action;
    private int key;
    private int src_port;

    public ClientHandler(String address, int port, int action, int key, int src_port) {
        TCH = new Thread(this, "TCH");
        this.action = action;
        this.key = key;
        this.src_port = src_port;

        try {
            socket = new Socket(address, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            TCH.start();
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    @Override
    public void run() {
        if (action == 2) {
            Int.put(Integer.MIN_VALUE);
            try {
                String str = null;
                out.writeUTF("2:" + this.src_port + ":" + key + "");
                //System.out.println("2:" + this.src_port + ":" + key + "");
                while (str == null) {
                    str = in.readUTF();
                }
                Int.put(Integer.parseInt(str));

                synchronized (Int.returnValue) {
                    Int.returnValue.notifyAll();
                }
                out.flush();
            } catch (Exception i) {
                System.out.println(i);
            }
        } else if (action == 3) {
            Int.put(Integer.MIN_VALUE);
            try {
                String str = null;
                out.writeUTF("3:" + this.src_port + ":" + key + "");
                while (str == null) {
                    str = in.readUTF();
                }
                Int.put(Integer.parseInt(str));

                synchronized (Int.returnValue) {
                    Int.returnValue.notifyAll();
                }
                out.flush();
            } catch (Exception i) {
                System.out.println(i);
            }
        } else if (action == 4) {
            try {
                out.writeUTF("4:" + this.src_port + ":" + key + "");
                //out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

class Server implements Runnable {

    public Thread TS;
    private int port_no;
    private int dest_port_no;
    public boolean is_first;
    private Socket socket;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;

    public Server(int port, int dest_port) {
        TS = new Thread(this, "TS");
        this.dest_port_no = dest_port;
        this.port_no = port;
        if (port == dest_port) {
            this.is_first = true;
        } else {
            this.is_first = false;
        }
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");
            TS.start();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    @Override
    public void run() {
        try {
            int n, ch, src_port;

            while (true) {
                while (socket == null) {
                    socket = server.accept();
                    //System.out.println("Client accepted");
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                }

                try {
                    String query = in.readUTF();
                    ch = Integer.parseInt(query.substring(0, 1));
                    src_port = Integer.parseInt(query.substring(2, 6));
                    n = Integer.parseInt(query.substring(7));
                    //System.out.println(ch + " " + src_port + " " + n);

                    if (ch == 2) {
                        int value = Hash.get(n);
                        if (value == -1) {
                            if (src_port == this.dest_port_no) {
                                out.flush();
                                out.writeUTF("-1");
                            } else {
                                ClientHandler client = new ClientHandler("127.0.0.1", this.dest_port_no, 2, n, src_port);
                                synchronized (Int.returnValue) {
                                    Int.returnValue.wait();
                                }
                                value = Int.get();
                                Int.put(Integer.MIN_VALUE);
                            }
                        }
                        out.flush();
                        out.writeUTF("" + value + "");

                        socket.close();
                        socket = null;  //may be removed
                    } else if (ch == 3) {
                        int value = Hash.get(n);
                        if (value == -1) {
                            value = 0;
                        }
                        if (src_port != this.dest_port_no) {
                            ClientHandler client = new ClientHandler("127.0.0.1", this.dest_port_no, 3, n, src_port);
                            synchronized (Int.returnValue) {
                                Int.returnValue.wait();
                            }
                            value += Int.get();
                            Int.put(Integer.MIN_VALUE);
                        }

                        out.flush();
                        out.writeUTF("" + value + "");

                        socket.close();
                        socket = null;  //may be removed

                    } else if (ch == 4) {
                        if (this.is_first == true) {
                            this.dest_port_no = src_port;
                            System.out.println("Destination Port Changed to: " + this.dest_port_no);
                        } else {
                            ClientHandler client = new ClientHandler("127.0.0.1", this.dest_port_no, 4, 0, src_port);
                        }

                        socket.close();
                        socket = null;
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

class Client implements Runnable {

    Thread TC;
    private Socket socket = null;
    private DataInputStream in;
    private DataOutputStream out;
    private final String address;
    private final int port;

    public Client(String address, int port) {
        TC = new Thread(this, "TC");
        this.address = address;
        this.port = port;
        TC.start();
    }

    public void setServer(String address, int port) {
        try {
            socket = new Socket(address, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    @Override
    public void run() {
        setServer(this.address, this.port);
        try {
            out.writeUTF("4:" + this.port + ":" + 0 + "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int val;

        while (true) {
            try {
                System.out.println("1. PUT\t2. GET\t3. SUM");
                val = Integer.parseInt(br.readLine());

                switch (val) {
                    case 1: {
                        System.out.print("Enter Key Value Pair: ");
                        int key = Integer.parseInt(br.readLine());
                        int value = Integer.parseInt(br.readLine());
                        Hash.put(key, value);
                        break;
                    }
                    case 2: {
                        setServer(this.address, this.port);

                        System.out.print("Enter Key: ");
                        int key = Integer.parseInt(br.readLine());
                        try {
                            out.writeUTF("2:" + this.port + ":" + key + "");

                            String str = null;
                            while (str == null) {
                                str = in.readUTF();
                            }
                            out.flush();
                            if (Integer.parseInt(str) == -1) {
                                System.out.println("Value does not exists!");
                            } else {
                                System.out.println(str);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 3: {
                        setServer(this.address, this.port);

                        System.out.print("Enter Key: ");
                        int key = Integer.parseInt(br.readLine());
                        try {
                            out.writeUTF("3:" + this.port + ":" + key + "");

                            String str = null;
                            while (str == null) {
                                str = in.readUTF();
                            }
                            out.flush();

                            System.out.println(str);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 4:
                        try {
                            in.close();
                            out.close();
                            socket.close();
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                        break;
                    default:
                        System.out.println("Invalid Choice!");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Invalid Input");
            }
        }
    }
}

public class DHT {

    public static void main(String args[]) throws Exception {

        int src_port = 0, dst_port = 0;
        for (int i = 0; i < 1000; i++) {
            Hash.arr[i] = -1;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Source Port, Destination Port");
        try {
            src_port = Integer.parseInt(br.readLine());
            dst_port = Integer.parseInt(br.readLine());
        } catch (Exception e) {
            System.out.println("Invalid Input");
        }

        Server server = new Server(src_port, dst_port);
        Client client = new Client("127.0.0.1", src_port);
    }
}
