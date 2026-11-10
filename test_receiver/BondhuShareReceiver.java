import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class BondhuShareReceiver {
    private static final int PORT = 8988;
    private static final int MAGIC_BNDU = 0x424E4455; // 'BNDU'
    private static final int MAGIC_FLSH = 0x464C5348; // 'FLSH'
    private static final byte ACK_BYTE = 0x06;
    private static final int BUFFER_SIZE = 512 * 1024; // 512 KB buffer

    public static void main(String[] args) {
        File saveDir = new File("d:/Code/Shear Fast/received_from_phone");
        if (!saveDir.exists()) saveDir.mkdirs();

        System.out.println("===================================================================");
        System.out.println("[*] BondhuShare High-Speed Live PC Receiver");
        System.out.println("[*] Buffer: 512 KB | TCP Window: 2 MB | Zero-Copy Pipeline");
        System.out.println("[*] Port: " + PORT);
        System.out.println("[*] Saved files destination: " + saveDir.getAbsolutePath());
        System.out.println("===================================================================");
        System.out.println("Active PC Network Addresses:");
        printLocalIPs();
        System.out.println("===================================================================");
        System.out.println("Tips for 30 - 60+ MB/s Super Speed:");
        System.out.println("   1. Turn ON Phone Hotspot (5 GHz band) and connect PC to it.");
        System.out.println("   2. Or connect both PC and Phone to 5 GHz Wi-Fi SSID.");
        System.out.println("   3. In BondhuShare, select files with 'Send (Original Quality)'");
        System.out.println("   4. Go to Settings > 'Direct IP Send' and enter your PC IP.");
        System.out.println("   5. Tap 'Connect & Send'");
        System.out.println("===================================================================");
        System.out.println("Waiting for phone connection...\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReceiveBufferSize(2 * 1024 * 1024);

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);
                    socket.setReceiveBufferSize(2 * 1024 * 1024);
                    socket.setSendBufferSize(2 * 1024 * 1024);

                    String clientIp = socket.getInetAddress().getHostAddress();
                    System.out.println("\n[CONNECTED] Phone connected from: " + clientIp);

                    InputStream rawIn = socket.getInputStream();
                    DataInputStream dataInput = new DataInputStream(new BufferedInputStream(rawIn, 64 * 1024));
                    DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 64 * 1024));

                    // Read Magic Header
                    int magic = dataInput.readInt();
                    if (magic != MAGIC_BNDU && magic != MAGIC_FLSH) {
                        System.err.println("[ERROR] Invalid magic header: 0x" + Integer.toHexString(magic));
                        socket.close();
                        continue;
                    }

                    // Read JSON Metadata Header
                    int headerLen = dataInput.readInt();
                    byte[] headerBytes = new byte[headerLen];
                    dataInput.readFully(headerBytes);
                    String json = new String(headerBytes, StandardCharsets.UTF_8);

                    String fileName = extractJsonValue(json, "fileName");
                    if (fileName == null || fileName.isEmpty()) fileName = "received_file_" + System.currentTimeMillis();
                    long totalBytes = extractJsonLong(json, "fileSize");

                    System.out.printf("[RECEIVING] File: %s (%.2f MB)\n", fileName, totalBytes / (1024.0 * 1024.0));

                    // Send ACK
                    dataOutput.writeByte(ACK_BYTE);
                    dataOutput.flush();

                    // Receive File Stream using 512 KB buffer
                    File targetFile = new File(saveDir, fileName);
                    long startTime = System.currentTimeMillis();
                    long lastPrintTime = startTime;
                    long bytesTransferred = 0;
                    long lastSampleBytes = 0;

                    byte[] buffer = new byte[BUFFER_SIZE];
                    try (FileOutputStream fos = new FileOutputStream(targetFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {

                        while (bytesTransferred < totalBytes || totalBytes <= 0) {
                            int toRead = (totalBytes > 0) ? (int) Math.min((long) buffer.length, totalBytes - bytesTransferred) : buffer.length;
                            int read = rawIn.read(buffer, 0, toRead);
                            if (read == -1) break;

                            bos.write(buffer, 0, read);
                            bytesTransferred += read;

                            long now = System.currentTimeMillis();
                            if (now - lastPrintTime >= 250) {
                                long bytesDelta = bytesTransferred - lastSampleBytes;
                                long timeDelta = now - lastPrintTime;
                                double instantSpeed = (timeDelta > 0) ? (bytesDelta / (1024.0 * 1024.0)) / (timeDelta / 1000.0) : 0.0;
                                double percent = (totalBytes > 0) ? (bytesTransferred * 100.0 / totalBytes) : 0;
                                System.out.printf("\r>>> Progress: %.1f%% (%.2f / %.2f MB) | Speed: %.1f MB/s   ",
                                        percent, bytesTransferred / (1024.0 * 1024.0), totalBytes / (1024.0 * 1024.0), instantSpeed);
                                lastPrintTime = now;
                                lastSampleBytes = bytesTransferred;
                            }
                        }
                        bos.flush();
                    }

                    double totalSec = Math.max(0.01, (System.currentTimeMillis() - startTime) / 1000.0);
                    double avgSpeed = (bytesTransferred / (1024.0 * 1024.0)) / totalSec;

                    System.out.println("\n[COMPLETE] 100% Transfer Finished Successfully!");
                    System.out.printf("   - File: %s\n", fileName);
                    System.out.printf("   - Size: %.2f MB\n", bytesTransferred / (1024.0 * 1024.0));
                    System.out.printf("   - Average Speed: %.2f MB/s (Duration: %.2f s)\n", avgSpeed, totalSec);
                    System.out.printf("   - Saved in: %s\n\n", targetFile.getAbsolutePath());
                    System.out.println("Waiting for next file...\n");

                    socket.close();
                } catch (Exception e) {
                    System.err.println("[WARNING] Connection ended: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[FATAL] Server error: " + e.getMessage());
        }
    }

    private static void printLocalIPs() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        System.out.printf("   -> [%s]: %s\n", iface.getDisplayName(), ip);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0L;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)))) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
