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

  protected Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
  private Map<String, Mustache.Lambda> templateLambdas = new ConcurrentHashMap<String, Mustache.Lambda>();

  public TemplateProcessor()
  {
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

  // ============================================= TEMPLATE PROCESSING ============================================


  public void processTemplate(String templateString, JsonObject params, final Handler<String> handler)
  {
    this.processTemplateToWriter(templateString, params, new Handler<Writer>()
    {
      @Override
      public void handle(Writer w)
      {
        handler.handle(w == null ? null : w.toString());
      }
    });
  }

  public void processTemplateToWriter(String templateString, JsonObject params, final Handler<Writer> handler)
  {
    this.getTemplate(templateString, new Handler<Template>()
    {
      @Override
      public void handle(Template t)
      {
        processTemplate(t, params, handler);
      }
    });
  }

  protected void processTemplate(Template t, JsonObject params, final Handler<Writer> handler)
  {
    final JsonObject ctxParams = (params == null) ? new JsonObject() : params.copy();
    final Map<String, Object> ctx = JsonUtils.convertMap(ctxParams);
    this.applyLambdas(ctx);

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

  protected void getTemplate(String templateString, final Handler<Template> handler)
  {
    handler.handle(compiler.compile(templateString));
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