package org.opencds.cqf.ruler.cdshooks.response;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import org.hl7.fhir.Coding;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

public class Cards {
    @JsonIgnore
    public static IParser parser;
    public List<Card> cards;

    public static class Card {
        public String uuid;
        public String summary;
        public String detail;
        public String indicator;
        public Source source;
        public List<Suggestion> suggestions;
        public String selectionBehavior;
        public List<Coding> overrideReasons;
        public List<Link> links;

        public static class Source {
            public String label;
            public String uri;
            public String icon;
            public Coding topic;
        }

        public static class Suggestion {
            public String label;
            public String uuid;
            public boolean isRecommended;
            public List<Action> actions;

            public static class Action {
                public String type;
                public String description;
                public IBaseResource resource;
                public String resourceId;

                @JsonGetter("resource")
                @JsonRawValue
                public String getResource() {
                    return parser.setPrettyPrint(true).encodeResourceToString(resource);
                }
            }
        }

        public static class Link {
            public String label;
            public String url;
            public String type;
            public String appContext;
        }
    }
}
