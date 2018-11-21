package biz.churen.model.stock;

import biz.churen.model.annotation.ZColumnName;
import biz.churen.model.annotation.ZPrimaryKey;
import biz.churen.model.annotation.ZTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@ZTable(tableName = "s_stock_day")
@ZPrimaryKey(columnNames = {"code", "day"})
public class StockDay {
  @ZColumnName (columnName = "code") private String code;
  @ZColumnName (columnName = "day") private java.sql.Date day;

  @ZColumnName (columnName = "TCLOSE") private String TCLOSE;
  @ZColumnName (columnName = "HIGH") private String HIGH;
  @ZColumnName (columnName = "LOW") private String LOW;
  @ZColumnName (columnName = "TOPEN") private String TOPEN;
  @ZColumnName (columnName = "LCLOSE") private String LCLOSE;
  @ZColumnName (columnName = "CHG") private String CHG;
  @ZColumnName (columnName = "PCHG") private String PCHG;
  @ZColumnName (columnName = "TURNOVER") private String TURNOVER;
  @ZColumnName (columnName = "VOTURNOVER") private String VOTURNOVER;
  @ZColumnName (columnName = "VATURNOVER") private String VATURNOVER;
  @ZColumnName (columnName = "TCAP") private String TCAP;
  @ZColumnName (columnName = "MCAP") private String MCAP;

  @ZColumnName (columnName = "valid") private Integer valid;
}
