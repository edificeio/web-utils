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

package fr.wseduc.webutils.template.lambdas;

import java.io.Writer;
import java.io.IOException;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import fr.wseduc.webutils.I18n;

public class I18nLambda implements Mustache.Lambda
{
  private final I18n i18n;

  private final String host;
  private final String locale;

  public I18nLambda(String locale)
  {
    this(locale, null);
  }

  public I18nLambda(String locale, String host)
  {
    this.i18n = I18n.getInstance();
    this.host = host;
    this.locale = locale != null ? locale : "fr";
  }

  @Override
  public void execute(Template.Fragment frag, Writer out) throws IOException {
    String key = frag.execute();
    String text;

    if(this.host == null)
      text = i18n.translate(key, I18n.DEFAULT_DOMAIN, locale);
    else
      text = i18n.translate(key, host, locale);

    // This will handle translation units with embedded mustache templates
    Mustache.compiler().compile(text).execute(frag.context(), out);
    //out.write(text);
  }
}