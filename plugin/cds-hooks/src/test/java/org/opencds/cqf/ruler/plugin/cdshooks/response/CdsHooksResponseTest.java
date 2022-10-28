package org.opencds.cqf.ruler.plugin.cdshooks.response;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cdshooks.response.Card;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class CdsHooksResponseTest {
	@Test
	void unicodeCharacterTest() throws JsonProcessingException {
		Card card = new Card();
		card.setSummary("Greater than or equals symbol (≥) (&#8805;) (U+2265)");

		ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
		String result = mapper.writeValueAsString(card);
		JsonElement jsonResult = JsonParser.parseString(result);
		String gsonResult = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(jsonResult);
		String s = "";
	}
}
