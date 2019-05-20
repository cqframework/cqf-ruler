package org.opencds.cqf.cdshooks.response;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.exceptions.MissingRequiredFieldException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christopher on 5/4/2017.
 */
public class CdsCard {

    /*
    *
    * Specification v1.0:
    *   summary     - REQUIRED  - String
    *   detail      - OPTIONAL  - String
    *   indicator   - REQUIRED  - String
    *   source      - REQUIRED  - Object
    *   suggestions - OPTIONAL  - Array[suggestion]
    *   links       - OPTIONAL  - Array[link]
    *
    * */

    public CdsCard() {
        this.source = new Source();
        this.suggestions = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    // Required elements cstor
    public CdsCard(String summary, String indicator, Source source) {
        this.summary = summary;
        this.indicator = IndicatorCode.toCode(indicator);
        this.source = source;
    }

    private String summary;
    public boolean hasSummary() {
        return this.summary != null && !this.summary.isEmpty();
    }
    public String getSummary() {
        return this.summary;
    }
    public CdsCard setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    private String detail;
    public boolean hasDetail() {
        return this.detail != null && !this.detail.isEmpty();
    }
    public String getDetail() {
        return this.detail;
    }
    public CdsCard setDetail(String detail) {
        this.detail = detail;
        return this;
    }

    private IndicatorCode indicator;
    public enum IndicatorCode {
        INFO("info"),
        WARN("warning"),
        HARDSTOP("hard-stop");

        public final String code;
        IndicatorCode(String code) {
            this.code = code;
        }

        public static IndicatorCode toCode(String indicator) {
            switch (indicator) {
                case "info": return IndicatorCode.INFO;
                case "warning": return IndicatorCode.WARN;
                case "hard-stop": return IndicatorCode.HARDSTOP;
                default: throw new RuntimeException("Invalid indicator code: " + indicator);
            }
        }
    }
    public boolean hasIndicator() {
        return this.indicator != null;
    }
    public IndicatorCode getIndicator() {
        return this.indicator;
    }
    public CdsCard setIndicator(IndicatorCode indicator) {
        this.indicator = indicator;
        return this;
    }
    public CdsCard setIndicator(String indicator) {
        this.indicator = IndicatorCode.toCode(indicator);
        return this;
    }

    /*
    *
    * label - REQUIRED  - String
    * url   - OPTIONAL  - URL
    * icon  - OPTIONAL  - URL
    *
    * */
    private Source source;
    public static class Source {
        private String label;
        private URL url;
        private URL icon;

        public boolean hasLabel() {
            return this.label != null && !this.label.isEmpty();
        }
        public String getLabel() {
            return this.label;
        }
        public CdsCard.Source setLabel(String label) {
            this.label = label;
            return this;
        }

        public boolean hasUrl() {
            return this.url != null;
        }
        public URL getUrl() {
            return this.url;
        }
        public CdsCard.Source setUrl(URL url) {
            this.url = url;
            return this;
        }
        public CdsCard.Source setUrl(String url) {
            try {
                this.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed CDS Hooks Card source URL: " + url);
            }
            return this;
        }

        public boolean hasIcon() {
            return this.icon != null;
        }
        public URL getIcon() {
            return this.icon;
        }
        public CdsCard.Source setIcon(URL icon) {
            this.icon = icon;
            return this;
        }
        public CdsCard.Source setIcon(String icon) {
            try {
                this.icon = new URL(icon);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed CDS Hooks Card source icon URL: " + url);
            }
            return this;
        }
    }
    public boolean hasSource() {
        return source.hasLabel();
    }
    public Source getSource() {
        return this.source;
    }
    public CdsCard setSource(Source source) {
        this.source = source;
        return this;
    }

    /*
    *
    * label   - REQUIRED    - String
    * uuid    - OPTIONAL    - String
    * actions - OPTIONAL    - Array[action]
    *
    * */
    private List<Suggestions> suggestions;
    public static class Suggestions {
        private String label;
        private String uuid;
        private List<Action> actions;

        public boolean hasLabel() {
            return this.label != null && !this.label.isEmpty();
        }
        public String getLabel() {
            return this.label;
        }
        public CdsCard.Suggestions setLabel(String label) {
            this.label = label;
            return this;
        }

        public boolean hasUuid() {
            return this.uuid != null && !this.uuid.isEmpty();
        }
        public String getUuid() {
            return this.uuid;
        }
        public CdsCard.Suggestions setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public boolean hasActions() {
            return this.actions != null && !this.actions.isEmpty();
        }
        public List<Action> getActions() {
            return this.actions;
        }
        public CdsCard.Suggestions setActions(List<Action> actions) {
            this.actions = actions;
            return this;
        }
        public void addAction(Action action) {
            if (this.actions == null) {
                this.actions = new ArrayList<>();
            }
            this.actions.add(action);
        }

        /*
        *
        * type        - REQUIRED    - String
        * description - REQUIRED    - String
        * resource    - OPTIONAL    - FHIR Resource (create/update) or ID (delete)
        *
        * */
        public static class Action {
            public enum ActionType {create, update, delete}

            private ActionType type;
            private String description;
            private IBaseResource resource;

            public boolean hasType() {
                return this.type != null;
            }
            public ActionType getType() {
                return this.type;
            }
            public Action setType(ActionType type) {
                this.type = type;
                return this;
            }

            public boolean hasDescription() {
                return this.description != null && !this.description.isEmpty();
            }
            public String getDescription() {
                return this.description;
            }
            public Action setDescription(String description) {
                this.description = description;
                return this;
            }

            public boolean hasResource() {
                return this.resource != null;
            }
            public IBaseResource getResource() {
                return this.resource;
            }
            public Action setResource(IBaseResource resource) {
                this.resource = resource;
                return this;
            }
        }
    }
    public boolean hasSuggestions() {
        return this.suggestions != null && !this.suggestions.isEmpty();
    }
    public List<Suggestions> getSuggestions() {
        return this.suggestions;
    }
    public CdsCard setSuggestions(List<Suggestions> suggestions) {
        this.suggestions = suggestions;
        return this;
    }
    public void addSuggestion(Suggestions suggestions) {
        this.suggestions.add(suggestions);
    }

    /*
    *
    * label      - REQUIRED     - String
    * url        - REQUIRED     - URL
    * type       - REQUIRED     - String
    * appContext - OPTIONAL     - String
    *
    * */
    private List<Links> links;
    public static class Links {
        private String label;
        private URL url;
        private String type;
        private String appContext;

        public boolean hasLabel() {
            return this.label != null && !this.label.isEmpty();
        }
        public String getLabel() {
            return this.label;
        }
        public CdsCard.Links setLabel(String label) {
            this.label = label;
            return this;
        }

        public boolean hasUrl() {
            return this.url != null;
        }
        public URL getUrl() {
            return this.url;
        }
        public CdsCard.Links setUrl(URL url) {
            this.url = url;
            return this;
        }
        public CdsCard.Links setUrl(String url) {
            try {
                this.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed CDS Hooks Card link URL: " + url);
            }
            return this;
        }

        public boolean hasType() {
            return this.type != null && !this.type.isEmpty();
        }
        public String getType() {
            return this.type;
        }
        public CdsCard.Links setType(String type) {
            this.type = type;
            return this;
        }

        public boolean hasAppContext() {
            return this.appContext != null && !this.appContext.isEmpty();
        }
        public String getAppContext() {
            return this.appContext;
        }
        public CdsCard.Links setAppContext(String appContext) {
            this.appContext = appContext;
            return this;
        }
    }
    public boolean hasLinks() {
        return this.links != null && !this.links.isEmpty();
    }
    public List<Links> getLinks() {
        return this.links;
    }
    public CdsCard setLinks(List<Links> links) {
        this.links = links;
        return this;
    }
    public void addLink(Links link) {
        this.links.add(link);
    }

    public JsonObject toJson() {
        try {
            JsonObject card = new JsonObject();
            if (!hasSummary()) {
                throw new MissingRequiredFieldException("The summary field must be specified in the action.title or action.dynamicValue field in the PlanDefinition");
            }
            card.addProperty("summary", getSummary());
            if (!hasIndicator()) {
                throw new MissingRequiredFieldException("The indicator field must be specified in the action.dynamicValue field in the PlanDefinition");
            }
            card.addProperty("indicator", getIndicator().code);
            if (hasDetail()) {
                card.addProperty("detail", getDetail());
            }

            // todo - the source requirements have been relaxed here - throw an error if missing label
            JsonObject sourceObject = new JsonObject();
            Source source = getSource();
            sourceObject.addProperty("label", source.getLabel());
            if (source.hasUrl()) {
                sourceObject.addProperty("url", source.getUrl().toString());
            }
            if (source.hasIcon()) {
                sourceObject.addProperty("icon", source.getIcon().toString());
            }
            card.add("source", sourceObject);

            if (hasSuggestions()) {
                JsonArray suggestionArray = new JsonArray();
                for (Suggestions suggestion : getSuggestions()) {
                    JsonObject suggestionObj = new JsonObject();
                    if (!suggestion.hasLabel()) {
                        throw new MissingRequiredFieldException("The suggestion.label field must be specified in the action.label field in the PlanDefinition");
                    }
                    suggestionObj.addProperty("label", suggestion.getLabel());
                    if (suggestion.hasUuid()) {
                        suggestionObj.addProperty("uuid", suggestion.getUuid());
                    }
                    if (suggestion.hasActions()) {
                        JsonArray actionArray = new JsonArray();
                        for (Suggestions.Action action : suggestion.getActions()) {
                            JsonObject actionObj = new JsonObject();
                            if (!action.hasType()) {
                                throw new MissingRequiredFieldException("The suggestion.action.type field must be specified as either create, update, or remove in the action.type field in the PlanDefinition");
                            }
                            actionObj.addProperty("type", action.getType().toString());
                            if (!action.hasDescription()) {
                                throw new MissingRequiredFieldException("The suggestion.action.description field must be specified in the description field in the ActivityDefinition referenced in the PlanDefinition");
                            }
                            actionObj.addProperty("description", action.getDescription());
                            if (action.hasResource()) {
                                JsonElement res = new JsonParser().parse(FhirContext.forDstu3().newJsonParser().setPrettyPrint(true).encodeResourceToString(action.getResource()));
                                actionObj.add("resource", res);
                            }
                            actionArray.add(actionObj);
                        }
                        suggestionObj.add("actions", actionArray);
                    }
                    suggestionArray.add(suggestionObj);
                }
                card.add("suggestions", suggestionArray);
            }

            if (hasLinks()) {
                JsonArray linksArray = new JsonArray();
                for (Links linkElement : getLinks()) {
                    JsonObject link = new JsonObject();
                    if (!linkElement.hasLabel()) {
                        throw new MissingRequiredFieldException("The link.label field must be specified in the relatedArtifact.display field in the PlanDefinition");
                    }
                    link.addProperty("label", linkElement.getLabel());
                    if (!linkElement.hasUrl()) {
                        throw new MissingRequiredFieldException("The link.url field must be specified in the relatedArtifact.url field in the PlanDefinition");
                    }
                    link.addProperty("url", linkElement.getUrl().toString());
                    // todo - relaxed requirements for type - throw error
                    link.addProperty("type", linkElement.getType());
                    if (linkElement.hasAppContext()) {
                        link.addProperty("appContext", linkElement.getAppContext());
                    }
                    linksArray.add(link);
                }
                card.add("links", linksArray);
            }
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return errorCard(e).toJson();
        }
    }

    public static CdsCard errorCard(Exception e) {
        CdsCard errorCard = new CdsCard();
        errorCard.setIndicator(CdsCard.IndicatorCode.HARDSTOP);
        errorCard.setSummary(e.getClass().getSimpleName() + " encountered during execution");
        errorCard.setDetail(e.getMessage());
        errorCard.setSource(new CdsCard.Source());
        return errorCard;
    }
}
