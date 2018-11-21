package biz.churen.db;

import biz.churen.model.annotation.ZColumnName;
import biz.churen.util.ZSQLUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter @Getter
@Slf4j
public class ZDBPool {
  public final static int MAX_RECORD_SIZE = 100000;
  public enum PoolName {
    WoXueApi("WoXueApi", "/config/db/oracle-dev.properties"),
    SelfDB("WoXueApi", "/config/db/mysql-dev.properties")
    ;

    String name;
    String fileName;
    PoolName(String name, String fileName) {
      this.name = name;
      this.fileName = fileName;
    }
  }

  public static PoolName getDefaultPoolName() { return PoolName.SelfDB; }


  public static final Map<PoolName, HikariDataSource> pools = new HashMap<>();

  public static void init() {
    // Examines both filesystem and classpath for .properties file
    HikariConfig config = new HikariConfig(getDefaultPoolName().fileName);
    HikariDataSource ds = new HikariDataSource(config);
    pools.put(getDefaultPoolName(), ds);
  }

  public static  <E> E queryOne(PoolName poolName, String sql, Class<? extends E> clazz) {
    List<E> es = query(poolName, sql, clazz, 1);
    return (null != es && es.size() > 0) ? es.get(0) : null;
  }

  public static <E> List<E> query(PoolName poolName, String sql, Class<? extends E> clazz) {
    return query(poolName, sql, clazz, MAX_RECORD_SIZE);
  }

  public static <E> List<E> query(PoolName poolName, String sql, Class<? extends E> clazz, int limit) {
    limit = Math.min(limit, MAX_RECORD_SIZE);
    List<E> es = new ArrayList<>();
    try {
      List<Map<String, Object>> list = run(poolName, sql, limit);
      for (int i = 0; null != list && i < list.size(); i++) {
        E e = clazz.newInstance();
        Field[] fields = e.getClass().getDeclaredFields();
        for (Field f : fields) {
          f.setAccessible(true);
          String columnName = f.getDeclaredAnnotation(ZColumnName.class).columnName();
          Object o = (list.get(i).get(columnName));
          Object o1 = o;
          if (o instanceof BigDecimal) { o1 = ((BigDecimal) o).intValue(); }
          if (o instanceof Clob) { o1 = ZSQLUtil.clobToString((Clob) o); }
          f.set(e, o1);
        }
        es.add(e);
      }
    } catch (IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
    return es;
  }

  private static List<Map<String, Object>> run(PoolName poolName, String sql, int limit) {
    List<Map<String, Object>> list = new ArrayList<>();
    try (Connection conn = pools.get(poolName).getConnection();
         Statement statement = conn.createStatement();
         ResultSet rs = statement.executeQuery(sql)
    ) {
      ResultSetMetaData metaData = rs.getMetaData();
      int columnCount = metaData.getColumnCount();
      while (rs.next()) {
        if (list.size() >= limit) { break; }
        Map<String, Object> rowData = new HashMap<>();
        for (int i = 1; i <= columnCount; i++) {
          rowData.put(metaData.getColumnName(i), rs.getObject(i));
        }
        list.add(rowData);
      }
    } catch (SQLException e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    return list;
  }

  public static int update(String sql) {
    log.info(sql);
    return update(ZDBPool.getDefaultPoolName(), sql);
  }

  public static int update(PoolName poolName, String sql) {
    int count = -1;
    try (Connection conn = pools.get(poolName).getConnection();
         Statement statement = conn.createStatement();
    ) {
      conn.setAutoCommit(false);
      count = statement.executeUpdate(sql);
      conn.commit();
    } catch (SQLException e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    return count;
  }
}
