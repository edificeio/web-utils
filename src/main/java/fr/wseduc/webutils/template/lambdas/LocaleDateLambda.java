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
import java.util.Locale;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class LocaleDateLambda implements Mustache.Lambda
{
  private DateTimeFormatter fmt;

  public LocaleDateLambda(String locale)
  {
    Locale l = new Locale(locale);
    this.fmt = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("FF", l)).withLocale(l);
  }

  @Override
  public void execute(Template.Fragment frag, Writer out) throws IOException
  {
    String unixTimestamp = frag.execute();
    String result = "PAS DE DATE";

    if(unixTimestamp != null)
    {
      try
      {
        Instant i = new Instant(Long.parseLong(unixTimestamp));
        result = fmt.print(i);
      }
      catch(NumberFormatException e)
      {
        result = "DATE INVALIDE";
      }
    }
    out.write(result);
  }
}