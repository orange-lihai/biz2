package biz.churen.util;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Clob;

public class ZSQLUtil {

  public static String clobToString(Clob sc) {
    String reString = "";
    try {
      Reader is = sc.getCharacterStream();
      BufferedReader br = new BufferedReader(is);
      String s = br.readLine();
      StringBuffer sb = new StringBuffer();
      while (s != null) {
        sb.append(s);
        s = br.readLine();
      }
      reString = sb.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return reString;
  }
}
