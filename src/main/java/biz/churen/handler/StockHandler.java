package biz.churen.handler;

import biz.churen.dao.ZGenericDao;
import biz.churen.db.ZDBPool;
import biz.churen.model.stock.Stock;
import biz.churen.model.stock.StockDay;
import biz.churen.util.ZBasicUtil;
import biz.churen.util.ZServletUtil;
import io.javalin.Handler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class StockHandler {

  public static Handler stockList = ctx -> {
    long n = 0;
    String _stockListUrl = "http://quote.eastmoney.com/stocklist.html";
    Document doc = Jsoup.connect(_stockListUrl).get();
    log.info(doc.title());
    Elements elements = doc.select("div#quotesearch ul li a");
    List<String> sqlList = new ArrayList<>();
    for (Element e : elements) {
      String href = e.attr("href");
      String text = e.text();
      String code = text.substring(text.lastIndexOf("(") + 1, text.lastIndexOf(")"));
      String name = text.substring(0, text.lastIndexOf("("));
      String type = href.contains("/sz"+code) ? "1" : "0";
      String city = href.substring(href.lastIndexOf("/") + 1, href.lastIndexOf(code));
      String sql = "insert into s_stock(code, name, `type`, `city`, home_url, create_time) " +
          "select s.code, s.name, s.type, s.city, s.home_url, s.create_time from (" +
          "select '"+code+"' code," +"'"+name+"' name," +"'"+type+"' type," +"'"+city+"' city," +
                 "'"+href+"' home_url," +"now() create_time from dual" +
          ") s where not exists (select 1 from s_stock x where x.code = s.code)";
      sqlList.add(sql);
    }
    n = sqlList.parallelStream().map(ZDBPool::update).count();
    ctx.result(n + " stocks!");
  };

  public static Handler stockHistory = ctx -> {
    String _urlTemp = "http://quotes.money.163.com/service/chddata.html?code=#{code}&start=#{from}&end=#{to}&fields=TCLOSE;HIGH;LOW;TOPEN;LCLOSE;CHG;PCHG;TURNOVER;VOTURNOVER;VATURNOVER;TCAP;MCAP";
    Map<String, String> param = ZServletUtil.getParameters(ctx.req);
    String _from = DateFormatUtils.format(DateUtils.addDays(new Date(), -1), "yyyyMMdd");
    String _to = DateFormatUtils.format(DateUtils.addDays(new Date(), 0), "yyyyMMdd");
    List<String> stockCodes = new ArrayList<>();
    Optional.ofNullable(param).ifPresent(m -> {
      String codes = null == m.get("codes") ? "" : m.get("codes");
      if (StringUtils.isBlank(codes)) {
        ZGenericDao<Stock> dao = new ZGenericDao<>(Stock.class);
        List<Stock> stocks = dao.query(new HashMap<>(), 100000);
        Optional.ofNullable(stocks).orElse(new ArrayList<>()).forEach(f -> stockCodes.add(f.getType()+f.getCode()));
      } else {
        stockCodes.addAll(Arrays.asList(codes.split(",")));
      }
    });
    String from = Optional.ofNullable(param).map(m -> ZBasicUtil.getStr(m.get("from"), _from)).orElse(_from);
    String to = Optional.ofNullable(param).map(m -> ZBasicUtil.getStr(m.get("to"), _to)).orElse(_to);
    long n = stockCodes.parallelStream().map(c -> {
      String _url = _urlTemp.replaceAll("#\\{code}", c).replaceAll("#\\{from}", from).replaceAll("#\\{to}", to);
      // log.info(_url);
      return spiderStockHistory(_url);
    }).count();
    //
    ctx.result(n + "");
  };

  public static int spiderStockHistory(String url) {
    int n = 0;
    List<StockDay> data = new ArrayList<>();
    try {
      String _randFileName = "/C:/stock/" + ((int) (Math.random() * 1000)) + ".dat";
      Connection.Response response = Jsoup.connect(url).ignoreContentType(true).execute();
      if (200 != response.statusCode()) {
        log.error("response error: => " + url);
        return -1;
      }
      // Document _doc = Jsoup.parse(new URL(url).openStream(), "GBK", url);
      try (FileOutputStream out = (new FileOutputStream(new java.io.File(_randFileName)))) {
        out.write(response.bodyAsBytes());
      }
      Iterable<CSVRecord> records = CSVFormat.DEFAULT
          .withDelimiter(',')
          .withQuote(null)
          .withRecordSeparator("\r\n")
          .withIgnoreEmptyLines(false)
          .withAllowMissingColumnNames(false)
          .withFirstRecordAsHeader()
          .withHeader("日期", "股票代码", "名称", "收盘价", "最高价", "最低价", "开盘价", "前收盘", "涨跌额", "涨跌幅", "换手率"
              , "成交量", "成交金额", "总市值", "流通市值")
          .parse(new BufferedReader(new InputStreamReader(new FileInputStream(_randFileName), "gbk")));
      for (CSVRecord record : records) {
        StockDay d = new StockDay(
          record.get("股票代码").substring(1), java.sql.Date.valueOf(record.get("日期")),
          ZBasicUtil.getDigit(record.get("收盘价"), "-1"),
          ZBasicUtil.getDigit(record.get("最高价"), "-1"),
          ZBasicUtil.getDigit(record.get("最低价"), "-1"),
          ZBasicUtil.getDigit(record.get("开盘价"), "-1"),
          ZBasicUtil.getDigit(record.get("前收盘"), "-1"),
          ZBasicUtil.getDigit(record.get("涨跌额"), "-1"),
          ZBasicUtil.getDigit(record.get("涨跌幅"), "-1"),
          ZBasicUtil.getDigit(record.get("换手率"), "-1"),
          ZBasicUtil.getDigit(record.get("成交量"), "-1"),
          ZBasicUtil.getDigit(record.get("成交金额"), "-1"),
          ZBasicUtil.getDigit(record.get("总市值"), "-1"),
          ZBasicUtil.getDigit(record.get("流通市值"), "-1"),
          1
        );
        data.add(d);
      }
      // (new File(_randFileName)).deleteOnExit();
      data.forEach(ZGenericDao::insertIfNotExists);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return n;
  }
}
