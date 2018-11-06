package biz.churen.handler;

import io.javalin.Handler;

public class IndexHandler {
  public static Handler indexHandler = ctx -> ctx.result("Hello World");
}
