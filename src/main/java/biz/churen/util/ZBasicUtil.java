package biz.churen.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class ZBasicUtil {

  public static String getStr(Object o, String defaultValue) {
    return (null == o || StringUtils.isBlank(o.toString())) ? defaultValue : o.toString();
  }

  public static String getDigit(String o, String defaultValue) {
    return NumberUtils.isDigits(o) ? o : defaultValue;
  }
}
