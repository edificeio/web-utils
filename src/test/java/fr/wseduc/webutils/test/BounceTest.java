/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.wseduc.webutils.test;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.wseduc.webutils.data.ZLib;
import fr.wseduc.webutils.email.Bounce;
import fr.wseduc.webutils.email.SendInBlueSender;
import fr.wseduc.webutils.security.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BounceTest {

	@Test
	public void shouldParseBounce() throws Exception {
		final String example = "{\"date\":\"2021-07-18T18:30:01.715+02:00\",\"message-id\":\"<202106190441.00000000000@smtp-test.test.fr>\",\"event\":\"open\",\"email\":\"test@test.com\",\"subject\":\"TEST\",\"ip\":\"00.000.00.000\",\"tag\":[],\"reason\":\"\",\"from\":\"test@test.com\",\"link\":\"\"}";
		final SendInBlueSender sender = new SendInBlueSender(Vertx.vertx(), new JsonObject().put("uri", "https://test.com:443").put("api-key", "test"));
		final Bounce bounce = sender.getMapper().readValue(example, new TypeReference<Bounce>(){});
		final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Assert.assertEquals("18-07-2021 18:30:01",format.format(bounce.getDate()));
		Assert.assertEquals("test@test.com",bounce.getEmail());
	}

}
