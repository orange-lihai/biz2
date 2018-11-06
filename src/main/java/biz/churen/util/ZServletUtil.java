package biz.churen.util;

import biz.churen.dao.ZParameterValue;
import io.javalin.json.JavalinJson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ZServletUtil {
  public static Map<String, ZParameterValue> getRequestParameters(HttpServletRequest request) {
    String type = Optional.ofNullable(request.getContentType()).orElse("");
    if (StringUtils.isBlank(type) || type.toLowerCase().contains("application/x-www-form-urlencoded")) {
      return getSimpleParameters(request);
    } else if (type.toLowerCase().contains("application/json")) {
      return getJsonParameters(request);
    } else if (type.toLowerCase().contains("multipart/form-data")) {
      return getMultipartParameters(request);
    } else {
      return getSimpleParameters(request);
    }
  }

  private static Map<String, ZParameterValue> getMultipartParameters(HttpServletRequest request) {
    Map<String, ZParameterValue> rs = new HashMap<>();
    try {
      // Create a factory for disk-based file items
      DiskFileItemFactory factory = new DiskFileItemFactory();
      // Set factory constraints
      factory.setSizeThreshold(1024 * 1024 * 32);
      // factory.setRepository(yourTempDirectory);
      // Create a new file upload handler
      ServletFileUpload upload = new ServletFileUpload(factory);
      // Set overall request size constraint
      upload.setSizeMax(1024 * 1024 * 32);
      // Parse the request
      List<FileItem> items = upload.parseRequest(request);
      // Process the uploaded items
      for (FileItem item : items) {
        if (item.isFormField()) {
          String name = item.getFieldName();
          String value = item.getString();
          rs.put(name, new ZParameterValue(ZParameterValue.OpTypes.EQ, value));
        } else {
          String name = item.getFieldName();
          byte value[] = item.get();
          rs.put(name, new ZParameterValue(ZParameterValue.OpTypes.EQ, value));
        }
      }
    } catch (FileUploadException e) {
      e.printStackTrace();
    }
    return rs;
  }

  public static Map<String, ZParameterValue> getSimpleParameters(HttpServletRequest request) {
    Map<String, ZParameterValue> rs = new HashMap<>();
    Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String k = names.nextElement();
      rs.put(k, new ZParameterValue(ZParameterValue.OpTypes.EQ, request.getParameter(k)));
    }
    return rs;
  }

  public static Map<String, ZParameterValue> getJsonParameters(HttpServletRequest request) {
    Map<String, ZParameterValue> rs = new HashMap<>();
    Map<String, Object> map = getJsonMap(request);
    Optional.ofNullable(map).ifPresent(m ->
        m.keySet().forEach(k -> rs.put(k, new ZParameterValue(ZParameterValue.OpTypes.EQ, map.get(k)))));
    return rs;
  }

  public static Map<String, Object> getJsonMap(HttpServletRequest request) {
    Map<String, Object> map = new HashMap<>();
    int contentLength = request.getContentLength();
    if(contentLength < 0) { return null; }
    byte buffer[] = new byte[1024];
    try (InputStream in = request.getInputStream()) {
      for (int i = 0; i < contentLength; ) {
        int len = in.read(buffer, i, buffer.length);
        if (len == -1) { break; }
        i += len;
      }
      String charEncoding = Optional.ofNullable(request.getCharacterEncoding()).orElse("UTF-8");
      map = JavalinJson.fromJson(new String(buffer, charEncoding), Map.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Optional.ofNullable(map).orElse(new HashMap<>());
  }
}
