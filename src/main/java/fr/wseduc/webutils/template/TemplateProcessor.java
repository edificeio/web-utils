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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.data.FileResolver.absolutePath;

public class TemplateProcessor
{
  protected static final Logger log = LoggerFactory.getLogger(TemplateProcessor.class);

  private Vertx vertx;
  private String templateFolder;

  private Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
  private Map<String, Mustache.Lambda> templateLambdas = new ConcurrentHashMap<String, Mustache.Lambda>();

  private final ConcurrentMap<String, Template> cache = new ConcurrentHashMap<String, Template>();
  private boolean useCache = false;

  public TemplateProcessor(Vertx vertx, String templateFolder)
  {
    this.vertx = vertx;
    this.templateFolder = templateFolder.endsWith("/") ? templateFolder : templateFolder + "/";
  }

  public TemplateProcessor(Vertx vertx, String templateFolder, boolean useCache)
  {
    this(vertx, templateFolder);
    this.enableCache(useCache);
  }

  // =========================================== COMPILER CONFIGURATION ===========================================

  public TemplateProcessor setLambda(String identifier, Mustache.Lambda lambda)
  {
    this.templateLambdas.put(identifier, lambda);
    return this;
  }

  public TemplateProcessor clearLambda(String identifier)
  {
    this.templateLambdas.remove(identifier);
    return this;
  }

  public TemplateProcessor defaultValue(String defaultValue)
  {
    this.compiler = this.compiler.defaultValue(defaultValue);
    return this;
  }

  public TemplateProcessor escapeHTML(boolean enableHTMLEscaping)
  {
    this.compiler = this.compiler.escapeHTML(enableHTMLEscaping);
    return this;
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

  public void processTemplate(String resourceName, JsonObject params, final Handler<String> handler)
  {
    processTemplate(resourceName, params, null, new Handler<Writer>()
    {
      @Override
      public void handle(Writer w)
      {
        if (w != null)
          handler.handle(w.toString());
        else
          handler.handle(null);
      }
    });
  }

  public void processTemplate(String resourceName, JsonObject params, Reader r, final Handler<Writer> handler)
  {
    final JsonObject ctxParams = (params == null) ? new JsonObject() : params.copy();
    final Map<String, Object> ctx = JsonUtils.convertMap(ctxParams);
    this.applyLambdas(ctx);

    getTemplate(resourceName, r, new Handler<Template>()
    {
      @Override
      public void handle(Template t)
      {
        if (t != null)
        {
          try
          {
            Writer writer = new StringWriter();
            t.execute(ctx, writer);
            handler.handle(writer);
          }
          catch (Exception e)
          {
            log.error(e.getMessage(), e);
            handler.handle(null);
          }
        }
        else
          handler.handle(null);
      }
    });
  }

  private void getTemplate(String resourceName, Reader r, final Handler<Template> handler)
  {
    String path;
    if (resourceName != null && r != null && !resourceName.trim().isEmpty()) // Pourquoi a-t-on besoin que resourceName soit valide si r est déjà non-nul ?!
    {
      handler.handle(compiler.compile(r));
      return;
    }
    else
      path = this.templateFolder + resourceName;

    final String p = absolutePath(path);
    if (this.useCache == true)
    {
      Template cacheEntry = cache.get(p);
      if(cacheEntry != null)
      {
        handler.handle(cacheEntry);
        return;
      }
    }

    this.vertx.fileSystem().readFile(p, new Handler<AsyncResult<Buffer>>()
    {
      @Override
      public void handle(AsyncResult<Buffer> ar)
      {
        if (ar.succeeded())
        {
          Template template = compiler.compile(ar.result().toString("UTF-8"));

          if(useCache == true)
            cache.put(p, template);

          handler.handle(template);
        }
        else
          handler.handle(null);
      }
    });
  }

  // ================================================ PRIVATE UTILS ===============================================

  private void applyLambdas(Map<String, Object> context)
  {
    for(Map.Entry<String, Mustache.Lambda> entry : this.templateLambdas.entrySet())
    {
      context.put(entry.getKey(), entry.getValue());
    }
  }
}