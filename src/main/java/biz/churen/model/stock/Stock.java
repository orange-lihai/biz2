package biz.churen.model.stock;

import biz.churen.model.annotation.ZColumnName;
import biz.churen.model.annotation.ZPrimaryKey;
import biz.churen.model.annotation.ZTable;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter @Setter
@ZTable(tableName = "s_stock")
@ZPrimaryKey(columnNames = {"code"})
public class Stock {
  @ZColumnName (columnName = "code") private String code;
  @ZColumnName (columnName = "name") private String name;
  @ZColumnName (columnName = "type") private String type;
  @ZColumnName (columnName = "city") private String city;
  @ZColumnName (columnName = "home_url") private String homeUrl;
  @ZColumnName (columnName = "create_time")   private Date createTime;
  @ZColumnName (columnName = "valid") private Integer valid;
}
