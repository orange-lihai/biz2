package biz.churen.role;

import io.javalin.Context;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;

public class ZRole {

  public static AccessManager accessManager = (handler, ctx, permittedRoles) -> {
    Role roles = (new ZRole()).getUserRole(ctx);
    if (permittedRoles.contains(roles)) {
      handler.handle(ctx);
    } else {
      ctx.status(401).result("Unauthorized => " + ctx.req.getRequestURI());
    }
  };

  public enum ZRoleType implements Role {
    ANYONE, ROLE_READ, ROLE_WRITE, ROLE_DELETE, ROLE_ADMIN;
  }

  public Role getUserRole(Context ctx) {
    // TODO
    return ZRoleType.ANYONE;
  }


}
