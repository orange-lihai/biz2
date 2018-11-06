package biz.churen.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter @Setter
@NoArgsConstructor
public class ZParameterValue {
  public ZParameterValue(OpTypes opType, Object value, DBTypes dbType) {
    this.opType = opType;
    this.value = value;
    this.dbType = dbType;
  }

  public ZParameterValue(OpTypes opType, Object value) {
    this(opType, value, DBTypes.ORACLE);
  }

  public enum OpTypes {
    EQ("equal"),
    NOT_EQ("not equal"),
    GT("great than"),
    GE("great or equal"),
    LT("less than"),
    LE("less or equal"),
    IN("in"),
    NOT_IN("not in"),
    NULL("is null"),
    NOT_NULL("is not null"),
    LIKE("like"),
    LLIKE("llike"),
    RLIKE("rlike");

    @Getter @Setter String name;
    OpTypes(String name) {
      this.name = name;
    }
  }

  public enum DBTypes {
    MySQL("MySQL 5.7+"),
    SQL_SERVER("SQL Server 2008+"),
    ORACLE("Oracle 9i+");

    @Getter @Setter String name;
    DBTypes(String name) {
      this.name = name;
    }
  }

  private OpTypes opType;
  private Object value;
  private DBTypes dbType;

  public String toSqlString() {
    String sql = "";
    if (opType == OpTypes.EQ) {
      sql = " = " + toSqlValue();
    } else if (opType == OpTypes.NOT_EQ) {

    } else {
      // DO NOTHING !!!
    }
    return sql;
  }

  private String toSqlValue() {
    Class z = value.getClass();
    if (z.isAssignableFrom(String.class)) {
      return "'" + value.toString() + "'";
    } else if (z.isAssignableFrom(Date.class) || z.isAssignableFrom(java.sql.Date.class)) {
      return "TO_DATE(" + value + ")";
    } else if (z.isAssignableFrom(java.sql.Time.class)) {
      return "TO_DATE(" + value + ")";
    } else if (z.isAssignableFrom(java.sql.Timestamp.class)) {
      return "TO_DATE(" + value + ")";
    } else if (z.isAssignableFrom(Boolean.class)) {
      return ((boolean) value) ? "1" : "0";
    } else {
      return value.toString();
    }
  }
}
