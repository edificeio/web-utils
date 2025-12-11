/*
 * Copyright © "Open Digital Education", 2020
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package fr.wseduc.webutils.template;

import java.io.Writer;
import java.io.StringWriter;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import fr.wseduc.webutils.collections.JsonUtils;
import fr.wseduc.webutils.http.ProcessTemplateContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.data.FileResolver.absolutePath;

public class FileTemplateProcessor extends TemplateProcessor
{
  private Vertx vertx;
  private String templateFolder;

  private final ConcurrentMap<String, Template> cache = new ConcurrentHashMap<String, Template>();
  private boolean useCache = false;

  public FileTemplateProcessor(Vertx vertx, String templateFolder)
  {
    super();
    this.vertx = vertx;
    this.templateFolder = templateFolder.endsWith("/") ? templateFolder : templateFolder + "/";
  }

  public FileTemplateProcessor(Vertx vertx, String templateFolder, boolean useCache)
  {
    this(vertx, templateFolder);
    this.enableCache(useCache);
  }

  // ================================================= CACHE CONTROL ==============================================

  public void enableCache(boolean useCache)
  {
    this.useCache = useCache;
  }

  public void clearCache()
  {
    this.cache.clear();
  }

  // ============================================= TEMPLATE PROCESSING ============================================

  @Deprecated
  public void processTemplate(String resourceName, JsonObject params, Reader r, final Handler<Writer> handler)
  {
    if(r != null)
      this.processTemplate(compiler.compile(r), params, handler);
    else
      this.processTemplateToWriter(resourceName, params, handler);
  }

  @Override
  @Deprecated
  protected void getTemplate(String resourceName, final Handler<Template> handler)
  {
    String path = this.templateFolder + resourceName;
    final String p = absolutePath(path);
    handleGetTemplate(p,  handler);
  }

  @Override
  public void processTemplate(ProcessTemplateContext context, final Handler<Writer> handler) {
    if(context.reader() != null) {
      super.getTemplate(context, t -> processTemplate(context,  t , handler));
    } else {
      super.processTemplate(context, handler);
    }
  }


  @Override
  protected void getTemplate(ProcessTemplateContext context, final Handler<Template> handler) {
    String path = this.templateFolder + context.templateString();
    final String p = absolutePath(path);
    handleGetTemplate(p,  handler);
  }

  private void handleGetTemplate(String p, Handler<Template> handler) {
    if (this.useCache) {
      Template cacheEntry = cache.get(p);
      if(cacheEntry != null) {
        handler.handle(cacheEntry);
        return;
      }
    }

    this.vertx.fileSystem().readFile(p, ar -> {
      if (ar.succeeded()) {
        Template template = compiler.compile(ar.result().toString("UTF-8"));
        if(useCache) {
          cache.put(p, template);
        }
        handler.handle(template);
        return;
      }
      handler.handle(null);
    });
  }
}