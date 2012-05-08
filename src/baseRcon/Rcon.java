package baseRcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Rcon {
    
	private String pass;
	private Socket socket;
	private int id;
    private InputStream in;
    private OutputStream out;
    private ArrayList<Packet> responseBuffer;
    private boolean run;
	/**
	 * Creates a new Rcon session.
	 * @param IP of Server
	 * @param RCON port
	 * @param Password
	 * @throws IOException
	 * @throws BadRcon 
	 */
	public Rcon(String arg1, int arg2, String arg3) throws IOException, BadRcon {
		run = true;
		pass = arg3;
		socket = new Socket();
		responseBuffer = new ArrayList<Packet>();
        InetAddress addr = InetAddress.getLocalHost();
        byte[] ipAddr = addr.getAddress();
        InetAddress inetLocal = InetAddress.getByAddress(ipAddr);
        socket.bind(new InetSocketAddress(inetLocal, 0));
        socket.connect(new InetSocketAddress(arg1, arg2), 1000);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        socket.setSoTimeout(500);
        id = 0;
        auth();
        new Thread(new Runnable() {
        	public void run() {
        		do {
        		try {
					responseBuffer.add(receivePacket());
				} catch (IOException e) {
				}
        		} while(run);
        	}
        }).start();
	}
	
	private void auth() throws IOException, BadRcon {
		
        byte[] authRequest = constructPacket(3, pass, "");
        out.write(authRequest);
        
        Packet response = receivePacket();
        if (response.id == -1) {
        	throw new BadRcon();
        }
	}
	
	/**
	 * Sends a command to the server.
	 * @param Command
	 * @throws IOException
	 */
	public void sendCommand(String cmd) throws IOException {
        byte[] command = constructPacket(2 , cmd, "");
        out.write(command);
	}
	
	private byte[] constructPacket(int cmd, String s1, String s2) {
		id++;
        ByteBuffer p = ByteBuffer.allocate(s1.length() +s2.length() + 14);
        p.order(ByteOrder.LITTLE_ENDIAN);

        p.putInt(s1.length() + s2.length() + 10);
        p.putInt(id);
        p.putInt(cmd);
        p.put(s1.getBytes());
        p.put((byte) 0x00);
        p.put(s2.getBytes());
        p.put((byte) 0x00);

        return p.array();
	}
	
    private Packet receivePacket() throws IOException {
    	Packet pack = new Packet();
        ByteBuffer p = ByteBuffer.allocate(4120);
        p.order(ByteOrder.LITTLE_ENDIAN);

        byte[] length = new byte[4];
        if (in.read(length, 0, 4) != 4) {
        	return null;
        }
        
        byte[] a = new byte[4];
        if (in.read(a, 0, 4) != 4) {
        	return null;
        }
        byte[] b = new byte[4];
        if (in.read(b, 0, 4) != 4) {
        	return null;
        }
        p.put(length);
        p.put(a);
        p.put(b);
        
        ByteBuffer s1 = ByteBuffer.allocate(4120);
        s1.order(ByteOrder.LITTLE_ENDIAN);
        for(byte c =(byte) 0x01;c != (byte) 0x00;) {
        	c = (byte) in.read();
        	s1.put(c);
        }
        
        ByteBuffer s2 = ByteBuffer.allocate(4120);
        s2.order(ByteOrder.LITTLE_ENDIAN);
        for(byte c =(byte) 0x01;c != (byte) 0x00;) {
        	c = (byte) in.read();
        	s2.put(c);
        }
        
        pack.length = p.getInt(0);
        pack.id = p.getInt(4);
        pack.cmd = p.getInt(8);
        pack.s1 = new String(s1.array());
        pack.s2 = new String(s2.array());
        return pack;
    }
    /**
     * Closes the Socket. If not run before shutdown, it will break the server.
     * @throws IOException
     */
    public void closeSocket() throws IOException {
    	in.close();
    	out.close();
    	socket.close();
    	run = false;
    }
    /**
     * Returns the next buffered response from the server.
     * @return Next Buffered Response
     */
    public Packet nextResponse() {
    	if (responseBuffer.isEmpty()) {
    		return null;
    	}
    	Packet a = responseBuffer.get(0);
    	responseBuffer.remove(0);
    	return a;
    }

}
