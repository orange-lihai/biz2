package biz.churen.util.extend;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Setter @Getter
@Slf4j
public class TomcatAccessLogger {

  private Pattern pattern;
  private List<String> names;

  public TomcatAccessLogger(String pattern, List<String> names) {
    this.pattern = Pattern.compile(pattern);
    this.names = names;
  }

  public String extractParameters(String line, String name) {
    String r = null;
    Matcher m = this.pattern.matcher(line);
    if (m.find()) {
      r = m.group(name);
    }
    return r;
  }

  private Map<String, Integer> findInText(String fileName) {
    Map<String, Integer> rs = new HashMap<>();
    try (FileReader fr = new FileReader(new File(fileName));
         BufferedReader bf = new BufferedReader(fr)
    ) {
      String line;
      while (null != (line = bf.readLine())) {
        String query = extractParameters(line, "query");
        if (null != query) {
          query = (query.split("\\?"))[0];
          query = query.replaceAll("/{2,}", "/");
          if (query.endsWith("/")) { query = query.substring(0, query.length() - 1); }
          rs.put(query, rs.getOrDefault(query, 0) + 1);
        }
      }
    } catch (Exception ex) {
      log.info(ex.getMessage(), ex);
    }
    return rs;
  }

  public Map<String, Integer> findInDir(String dir) {
    Map<String, Integer> rs = new HashMap<>();
    File f = new File(dir);
    if (f.exists() && f.isDirectory()) {
      File[] files = f.listFiles((_dir, name) -> name.endsWith(".txt"));
      Arrays.stream(files).forEach(m -> {
        Map<String, Integer> r = findInText(m.getPath());
        for (String k : r.keySet()) {
          if (!rs.containsKey(k)) { System.out.println(" URL: " + k); }
          rs.put(k, rs.getOrDefault(k, 0) + r.get(k));
        }
      });
    }
    return rs;
  }

  public static void main(String[] args) {
    // 172.19.3.19 - - [01/Aug/2018:23:59:47 +0800] "GET /1/teacher/introduction?userId=liangxiaodong3%40xdf.cn&appId=7&sign=FCF62003126547DE763A398AA6C51689 HTTP/1.1" 200 314 6 6
    String accessLogPattern = "([^\"]{1,})\"([\\w]{3,})[ ](?<query>[^ ]{1,})[ ]([^ ]{4,})[ ][\\d]{3,3}[ ][\\d]{1,}[ ][\\d]{1,}[ ](?<mills>[\\d]{1,})";
    List<String> names = Arrays.asList("query", "mills");
    TomcatAccessLogger accessLogger = new TomcatAccessLogger(accessLogPattern, names);
    // Map<String, Integer> rs = accessLogger.findInText("D:\\logs\\wxapidata_access_log\\localhost_access_log.2018-08-01.txt");
    Map<String, Integer> rs = accessLogger.findInDir("D:\\logs\\wxapidata_access_log\\");
    System.out.println(" 统计结果: ===> ");
    for (String k : rs.keySet()) {
      System.out.println(k + " ==> " + rs.get(k));
    }
  }

}
