package de.adesso.fileupload.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;

public class ClientIpResolver {

  public static boolean isSameClientIp(String clientsIp, String uploadSessionIp) {
    try {
      InetAddress addr1 = InetAddress.getByName(clientsIp);
      InetAddress addr2 = InetAddress.getByName(uploadSessionIp);

      // Treat all loopback addresses as equal for localhost
      if (addr1.isLoopbackAddress() && addr2.isLoopbackAddress()) {
        return true;
      }

      byte[] ipv6_1 = toIpv6Bytes(addr1);
      byte[] ipv6_2 = toIpv6Bytes(addr2);

      return Arrays.equals(ipv6_1, ipv6_2);
    } catch (Exception e) {
      return false;
    }
  }

  private static byte[] toIpv6Bytes(InetAddress address) {
    byte[] addrBytes = address.getAddress();

    if (addrBytes.length == 16) {
      return addrBytes;
    }

    byte[] ipv6 = new byte[16];
    ipv6[10] = (byte) 0xff;
    ipv6[11] = (byte) 0xff;
    System.arraycopy(addrBytes, 0, ipv6, 12, 4);
    return ipv6;
  }

  public static String getClientsIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
