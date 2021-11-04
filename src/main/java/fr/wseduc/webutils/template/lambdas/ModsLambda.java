/*
 * Copyright © "Open Digital Education", 2021
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

package fr.wseduc.webutils.template.lambdas;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.io.Writer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
/**
 * This Mustache Lambda is responsible for injecting :
 * - mods version number by using a {{#modName}} directive,
 *   Clients loading a templated resource including this directive, will get the latest version number of the module.
 *
 * The lambda listens to MODSINFO_CHANGED_EVENT_NAME vertx bus events to keep mods version cache up-to-date.
 */
public class ModsLambda implements Mustache.Lambda
{
  public static final String MODSINFO_MAP_NAME= "modsInfoMap";
  public static final String MODSINFO_CHANGED_EVENT_NAME= "modsInfoChanged";
  private static final String MODSINFO_DEPLOYMENT_MOD_NAME= "deployment-tag";
	protected Vertx vertx;
  // Keep modules information cached.
  private final Map<String, JsonObject> modsInfo = new HashMap<String, JsonObject>();

  public ModsLambda(final Vertx vertx) {
    this.vertx = vertx;

    // Handler for MODSINFO_CHANGED_EVENT_NAME events. 
    // Puts modules information in local cache, for instant access later in the execute() method.
    // => do not directly access the shared vertx map if possible.
    Handler<Message<JsonObject>> onModsInfoChanged = new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        if( event!=null && event.body()!=null && !event.body().isEmpty() ) {
          final JsonObject jo = event.body();
          final String modName = jo.getString("name");
          if( modName!=null && modName.length()>0 ) {
            modsInfo.put( modName, jo );
            if( !MODSINFO_DEPLOYMENT_MOD_NAME.equals(modName) ) {
              // Also update a "deployment-tag" pseudo-module
              updateDeploymentTag( jo.getString("deployedAt") );
            }
          }
        }
      }
    };
    vertx.eventBus().localConsumer(MODSINFO_CHANGED_EVENT_NAME, onModsInfoChanged);
  }

  protected void updateDeploymentTag(final String modDate ) {
    if( modDate!=null && modDate.length()>0 ) {
      Map<String, Object> deploymentTagMap = new HashMap<String, Object>();
      deploymentTagMap.put( "name", MODSINFO_DEPLOYMENT_MOD_NAME );
      deploymentTagMap.put( "version", modDate );
      modsInfo.put( MODSINFO_DEPLOYMENT_MOD_NAME, new JsonObject(deploymentTagMap) );
    }
  }

  @Override
  public void execute(Template.Fragment frag, Writer out) throws IOException {
    String modName = frag.execute();
    JsonObject jo = modsInfo.get( modName );

    if( jo==null ) {
      // The lambda was started after MODSINFO_CHANGED_EVENT_NAME events were emitted, so let's initialize it.
      final Map<String, JsonObject> sharedMods = vertx.sharedData().getLocalMap(MODSINFO_MAP_NAME);
      jo = sharedMods.get( modName );
      if( jo!=null ) {
        modsInfo.put( modName, jo );
        if( !MODSINFO_DEPLOYMENT_MOD_NAME.equals(modName) ) {
          // Also update a "deployment-tag" pseudo-module
          updateDeploymentTag( jo.getString("deployedAt") );
        }
      }
    }

    out.write( jo!=null ? jo.getString("version", "") : "" );
  }
}