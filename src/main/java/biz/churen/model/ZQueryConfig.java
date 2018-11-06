package biz.churen.model;

import biz.churen.model.annotation.ZColumnName;
import biz.churen.model.annotation.ZPrimaryKey;
import biz.churen.model.annotation.ZTable;
import biz.churen.model.annotation.ZUniqueKey;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@ZTable(tableName = "Z_QUERY_CONFIG")
@ZUniqueKey(columnNames = {"url"})
@ZPrimaryKey(columnNames = {"id"})
public class ZQueryConfig {
  @ZColumnName(columnName = "ID") private String id;
  @ZColumnName(columnName = "URL") private String url;
  @ZColumnName(columnName = "NAME") private String name;
  @ZColumnName(columnName = "SQL") private String sql;
  @ZColumnName(columnName = "UNIQUE_KEYS") private String uniqueKeys;
  @ZColumnName(columnName = "PRIMARY_KEYS") private String primaryKeys;
  @ZColumnName(columnName = "DESCRIPTION") private String description;
  @ZColumnName(columnName = "VALID") private Integer valid;
}
