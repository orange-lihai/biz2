package biz.churen.dao;

import biz.churen.db.ZDBPool;
import biz.churen.model.ITable;
import biz.churen.model.ZQueryConfig;
import biz.churen.model.annotation.ZColumnName;
import biz.churen.model.annotation.ZPrimaryKey;
import biz.churen.model.annotation.ZTable;
import biz.churen.model.annotation.ZUniqueKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Setter @Getter
@NoArgsConstructor
@Slf4j
public class ZGenericDao<E> {
  private Class clazz;
  private String daoUrl;
  private boolean isVirtual;
  private ZQueryConfig queryConfig;

  public ZGenericDao(Class clazz) {
    this.clazz = clazz;
    this.daoUrl = clazz.getName();
    this.isVirtual = false;
  }

  public ZGenericDao(String daoUrl) {
    ZGenericDao dao = new ZGenericDao(ZQueryConfig.class);
    ZQueryConfig queryConfig = (ZQueryConfig) dao.queryByUniqueKeys(daoUrl);
    this.queryConfig = queryConfig;
    this.clazz = (new HashMap<String, Object>()).getClass();
    this.daoUrl = (null != queryConfig && queryConfig.getValid() > 0) ? daoUrl : "__NOT__FOUND__";
    this.isVirtual = true;
  }

  public E queryByPrimaryKeys(Object... primaryValues) {
    List<String> primaryKeys;
    if (isVirtual) {
      primaryKeys = Arrays.asList(queryConfig.getPrimaryKeys().split("[,]+"));
    } else {
      primaryKeys = Arrays.asList(((ZPrimaryKey) clazz.getAnnotation(ZPrimaryKey.class)).columnNames());
    }

    Map<String, ZParameterValue> parameters = tupleUpQueryParameter(primaryKeys, primaryValues);
    return queryOne(parameters);
  }

  public E queryByUniqueKeys(Object... uniqueValues) {
    List<String> uniqueKeys;
    if (isVirtual) {
      uniqueKeys = Arrays.asList(queryConfig.getUniqueKeys().split("[,]+"));
    } else {
      uniqueKeys = Arrays.asList(((ZUniqueKey) clazz.getAnnotation(ZUniqueKey.class)).columnNames());
    }
    Map<String, ZParameterValue> parameters = tupleUpQueryParameter(uniqueKeys, uniqueValues);
    return queryOne(parameters);
  }

  private Map<String, ZParameterValue> tupleUpQueryParameter(List<String> primaryKeys, Object[] primaryValues) {
    Map<String, ZParameterValue> parameters = new HashMap<>();
    for (int i = 0; i < primaryKeys.size() && i < primaryValues.length; i++) {
      ZParameterValue v = new ZParameterValue();
      v.setOpType(ZParameterValue.OpTypes.EQ);
      v.setValue(primaryValues[i]);
      parameters.put(primaryKeys.get(i), v);
    }
    return parameters;
  }

  public E queryOne(Map<String, ZParameterValue> parameters) {
    if (null == parameters) { parameters = new HashMap<>(); }
    List<E> es = query(parameters, 1);
    return (null != es && es.size() > 0) ? es.get(0) : null;
  }

  public <E> List<E> query(Map<String, ZParameterValue> parameters) {
    return query(parameters, 1000);
  }

  public <E> List<E> query(Map<String, ZParameterValue> parameters, int limit) {
    if (null == parameters) { parameters = new HashMap<>(); }
    StringBuilder sql = new StringBuilder();
    String tableName = isVirtual ? queryConfig.getSql() : ((ZTable) clazz.getAnnotation(ZTable.class)).tableName();
    sql.append("select ukt__.* from ").append(isVirtual ? "(" : "")
        .append(tableName).append(isVirtual ? ")" : "")
        .append(" ukt__ where 1 = 1");
    Stack<Pair<Pair<Integer, Integer>, String>> stack = new Stack<>();
    for (int i = 0; i < sql.length(); i++) {
      if ('{' == sql.charAt(i)) {
        boolean required = (sql.charAt(i - 1) == '!');
        int j = sql.indexOf("}", i) + 1;
        String _branch = sql.substring(i + 1, j - 1);
        String _k = parameters.keySet().stream().filter(k -> _branch.contains("$"+k)).findFirst().orElse("");
        String _sql = "";
        if (StringUtils.isBlank(_k)) {
          if (required) {
            _sql = sql.substring(i - 1, j);
          }
        } else {
          _sql = parameters.get(_k).toSqlString();
        }
        String _v = _sql;
        if (!StringUtils.isBlank(_k)) {
           _v = _branch.replaceAll("\\$"+_k, _sql);
        }
        Pair<Pair<Integer, Integer>, String> _node = new ImmutablePair<>(new ImmutablePair<>(i - 1, j), _v);
        stack.push(_node);
        i = j + 1;
      }
    }
    while (stack.size() > 0) {
      Pair<Pair<Integer, Integer>, String> _node = stack.pop();
      sql.replace(_node.getLeft().getLeft(), _node.getLeft().getRight(), _node.getRight());
    }
    log.info(sql.toString());
    /*if (sql.toString().contains("!{") || sql.toString().contains("#{")) {
      throw SQLException("SQL Error => " + sql.toString());
    }*/
    return (List<E>) ZDBPool.query(ZDBPool.getDefaultPoolName(), sql.toString(), this.clazz, limit);
  }

  public static <U> int insertIfNotExists(U u) {
    try {
      Class z = u.getClass();
      List<String> primaryKeys = Arrays.asList(((ZPrimaryKey) z.getAnnotation(ZPrimaryKey.class)).columnNames());
      String tableName = ((ZTable) z.getDeclaredAnnotation(ZTable.class)).tableName();
      List<String> columns = new ArrayList<>();
      List<String> columnValues = new ArrayList<>();
      for (Field f : z.getDeclaredFields()) {
        String columnName = Optional.ofNullable(f.getDeclaredAnnotation(ZColumnName.class))
            .map(ZColumnName::columnName).orElse(f.getName());
        columns.add(columnName);
        f.setAccessible(true);
        Object o = f.get(u);
        Object v = null == o ? "" : o;
        if (o instanceof String) {
          v = "'" + o + "' ";
        }
        if (o instanceof java.sql.Time) {
          v = "DATE_FORMAT('" + DateFormatUtils.format((java.sql.Time) v, "HH:mm:ss") + "', '%h:%i:%s')";
        } else if (o instanceof java.sql.Date) {
          v = "DATE_FORMAT('" + DateFormatUtils.format((java.sql.Date) v, "yyyy-MM-dd") + "', '%Y-%m-%d')";
        } else if (o instanceof java.sql.Timestamp) {
          v = ((java.sql.Timestamp) v).getTime();
        } else if (o instanceof java.util.Date) {
          v = "DATE_FORMAT('" + DateFormatUtils.format((java.util.Date) v, "yyyy-MM-dd HH:mm:ss") + "', '%Y-%m-%d %h:%i:%s')";
        }
        columnValues.add(v.toString() + " " + columnName);
      }
      StringBuilder sql = new StringBuilder("insert into ").append(tableName)
          .append("(").append(StringUtils.join(columns, ",")).append(")")
          .append(" select ")
          .append(columns.stream().map(m -> "s." + m).collect(Collectors.joining(",")))
          .append(" from ")
          .append("(")
          .append(" select ").append(StringUtils.join(columnValues, ",")).append(" from dual")
          .append(") s where not exists ( select 1 from ").append(tableName).append(" x where ")
          .append(primaryKeys.stream().map(m -> "x." + m + " = " + "s." + m).collect(Collectors.joining(" and ")))
          .append(")");
      return ZDBPool.update(sql.toString());
    } catch (Exception ex) {
      log.error(ex.getLocalizedMessage());
    }
    return -1;
  }
}
