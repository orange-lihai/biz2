package biz.churen.handler;

import biz.churen.dao.ZGenericDao;
import biz.churen.util.ZServletUtil;
import io.javalin.Handler;

import java.util.List;
import java.util.Map;

public class CommonHandler {

  public static Handler commonHandler = ctx -> {
    String uri = ctx.req.getRequestURI();
    ZGenericDao dao = new ZGenericDao(uri);
    List<Map> rs = dao.query(ZServletUtil.getRequestParameters(ctx.req));
    ctx.result(rs.toString());
  };
}
