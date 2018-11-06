package biz.churen;

import biz.churen.db.ZDBPool;
import biz.churen.handler.CommonHandler;
import biz.churen.handler.IndexHandler;
import biz.churen.role.ZRole;
import io.javalin.Javalin;

import static io.javalin.security.SecurityUtil.roles;

public class Application {
  public static void main(String[] args) {
    Javalin app = Javalin.create();
    app.enableStaticFiles("/public");
    app.enableDebugLogging();
    app.accessManager(ZRole.accessManager);
    ZDBPool.init();
    app.before(ctx -> {});
    app.get("/", IndexHandler.indexHandler, roles(ZRole.ZRoleType.ANYONE));
    app.get("/*", CommonHandler.commonHandler);
    app.post("/*", CommonHandler.commonHandler);
    app.after(ctx -> {});
    app.start(7000);
  }
}
