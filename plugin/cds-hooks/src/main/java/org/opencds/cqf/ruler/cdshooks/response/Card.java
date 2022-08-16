package org.opencds.cqf.ruler.cdshooks.response;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hl7.fhir.Coding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Card {
    private static final Logger logger = LoggerFactory.getLogger(Card.class);

    // Member variables

    @JsonIgnore
    private static final int SUMMARY_SIZE = 140;

    /**
     * Unique identifier of the card. MAY be used for auditing and logging cards and
     * SHALL be included in any subsequent calls to the CDS service's feedback endpoint.
     */
    private String uuid;

    /**
     * One-sentence, &lt;140-character summary message for display to the user inside of this card.
     */
    @JsonProperty(required = true)
    private String summary;

    /**
     * Optional detailed information to display; if provided MUST be represented in
     * <a href="https://github.github.com/gfm/">(GitHub Flavored) Markdown</a>.
     * (For non-urgent cards, the CDS Client MAY hide these details until the user
     * clicks a link like "view more details...").
     */
    private String detail;

    /**
     * Urgency/importance of what this card conveys. Allowed values, in order of increasing
     * urgency, are: info, warning, critical. The CDS Client MAY use this field to help make
     * UI display decisions such as sort order or coloring.
     */
    @JsonProperty(required = true)
    private String indicator;

    /**
     * Grouping structure for the {@link Card.Source} of the information displayed on
     * this card. The source should be the primary source of guidance for the decision
     * support the card represents.
     */
    @JsonProperty(required = true)
    private Card.Source source;

    /**
     * Allows a service to suggest a set of changes in the context of the current activity
     * (e.g. changing the dose of a medication currently being prescribed, for the order-sign
     * activity). If suggestions are present, {@link Card#selectionBehavior} MUST also be provided.
     * @see Card.Suggestion
     */
    private List<Card.Suggestion> suggestions;

    /**
     * Describes the intended selection behavior of the {@link Card#suggestions} in the card.
     * Allowed values are: at-most-one, indicating that the user may choose none or at most
     * one of the suggestions; any, indicating that the end user may choose any number of
     * suggestions including none of them and all of them. CDS Clients that do not understand
     * the value MUST treat the card as an error.
     */
    private String selectionBehavior;

    /**
     * Override reasons can be selected by the end user when overriding a card without taking
     * the suggested recommendations. The CDS service MAY return a list of override reasons to
     * the CDS client. If override reasons are present, the CDS Service MUST populate a display
     * value for each reason's Coding. The CDS Client SHOULD present these reasons to the
     * clinician when they dismiss a card. A CDS Client MAY augment the override reasons
     * presented to the user with its own reasons.
     */
    private List<Coding> overrideReasons;

    /**
     * Allows a service to suggest a link to an app that the user might want to run for
     * additional information or to help guide a decision.
     * @see Card.Link
     */
    private List<Card.Link> links;

    // Getters and Setters

    /**
     * Get the unique identifier of the card
     * @return {@link Card#uuid}
     */
    @JsonGetter
    public String getUuid() {
        return uuid;
    }

    /**
     * Set the unique identifier of the card
     * @param uuid {@link Card#uuid}
     */
    @JsonSetter
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the &lt;140-character summary message of the card
     * @return {@link Card#summary}
     */
    @JsonGetter
    public String getSummary() {
        return summary;
    }

    /**
     * Set the &lt;140-character summary message of the card
     * @param summary {@link Card#summary}
     */
    @JsonSetter
    public void setSummary(String summary) {
        if (summary.length() > SUMMARY_SIZE) {
            logger.warn("The card summary exceeds the 140 character limit set by the CDS Hooks specification.");
            // TODO: uncomment once gaps in Opioid recommendations are reconciled
            //summary = summary.substring(0, SUMMARY_SIZE);
        }
        this.summary = summary;
    }

    /**
     * Get the detailed information of the card
     * @return {@link Card#detail}
     */
    @JsonGetter
    public String getDetail() {
        return detail;
    }

    /**
     * Set the detailed information of the card
     * @param detail {@link Card#detail}
     */
    @JsonSetter
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * Get the urgency/importance of the card
     * @return {@link Card#indicator}
     */
    @JsonGetter
    public String getIndicator() {
        return indicator;
    }

    /**
     * Set the urgency/importance of the card
     * @param indicator {@link Card#indicator}
     * @throws ErrorHandling.CdsHooksError Error detailing unknown {@link Card#indicator} value
     */
    @JsonSetter
    public void setIndicator(String indicator) throws ErrorHandling.CdsHooksError {
        switch (indicator.toLowerCase()) {
            case "info":
            case "warning":
            case "critical":
                this.indicator = indicator;
                break;
            default: throw new ErrorHandling.CdsHooksError(
                    String.format("Unknown indicator value: %s", indicator));
        }
    }

    /**
     * Get the source of the card information
     * @return {@link Card.Source}
     */
    @JsonGetter
    public Source getSource() {
        return source;
    }

    /**
     * Set the source of the card information
     * @param source {@link Card.Source}
     */
    @JsonSetter
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Get the set of changes the card suggests
     * @return List of {@link Card.Suggestion}
     * @throws ErrorHandling.CdsHooksError Error detailing conditional constraint on {@link Card#selectionBehavior}
     */
    @JsonGetter
    public List<Suggestion> getSuggestions() throws ErrorHandling.CdsHooksError {
        if (suggestions == null) return null;
        if (this.getSelectionBehavior() == null) {
            logger.warn("If suggestions are present, selectionBehavior MUST also be provided");
            // TODO: uncomment once gaps in Opioid recommendations are reconciled
            //throw new ErrorHandling.CdsHooksError(
            //        "If suggestions are present, selectionBehavior MUST also be provided");
        }
        return suggestions;
    }

    /**
     * Set the suggested set of changes
     * @param suggestions {@link Card#suggestions}
     */
    @JsonSetter
    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Get the selection behavior of the suggestions in the card
     * @return {@link Card#selectionBehavior}
     */
    @JsonGetter
    public String getSelectionBehavior() {
        return selectionBehavior;
    }

    /**
     * Set the selection behavior of the suggestions in the card
     * @param selectionBehavior {@link Card#selectionBehavior}
     * @throws ErrorHandling.CdsHooksError  Error detailing unknown value
     */
    @JsonSetter
    public void setSelectionBehavior(String selectionBehavior) throws ErrorHandling.CdsHooksError {
        switch (selectionBehavior.toLowerCase()) {
            case "at-most-one":
            case "any":
                this.selectionBehavior = selectionBehavior;
                break;
            default: throw new ErrorHandling.CdsHooksError(
                    String.format("Unknown selectionBehavior value: %s", selectionBehavior));
        }
    }

    /**
     * Get override reasons for not taking the suggested recommendations
     * @return {@link Card#overrideReasons}
     */
    @JsonGetter
    public List<Coding> getOverrideReasons() {
        return overrideReasons;
    }

    /**
     * Set override reasons for not taking the suggested recommendations
     * @param overrideReasons {@link Card#overrideReasons}
     */
    @JsonSetter
    public void setOverrideReasons(List<Coding> overrideReasons) {
        this.overrideReasons = overrideReasons;
    }

    /**
     * Get link to additional information and/or guidance
     * @return List of {@link Card.Link}
     */
    @JsonGetter
    public List<Link> getLinks() {
        return links;
    }

    /**
     * Set link to additional information and/or guidance
     * @param links {@link Card#links}
     */
    @JsonSetter
    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public static class Source {

        // Member variables

        /**
         * A short, human-readable label to display for the source of the information displayed
         * on this card. If a URL is also specified, this MAY be the text for the hyperlink.
         */
        @JsonProperty(required = true)
        private String label;

        /**
         * An optional absolute URL to load (via GET, in a browser context) when a user clicks
         * on this link to learn more about the organization or data set that provided the
         * information on this card. Note that this URL should not be used to supply a
         * context-specific "drill-down" view of the information on this card. For that,
         * use {@link Card.Link#url} instead.
         */
        private String uri;

        /**
         * An absolute URL to an icon for the source of this card. The icon returned by this
         * URL SHOULD be a 100x100 pixel PNG image without any transparent regions. The CDS
         * Client may ignore or scale the image during display as appropriate for user experience.
         */
        private String icon;

        /**
         * A topic describes the content of the card by providing a high-level categorization
         * that can be useful for filtering, searching or ordered display of related cards in
         * the CDS client's UI. This specification does not prescribe a standard set of topics.
         */
        private Coding topic;

        // Getters and Setters

        /**
         * Get the human-readable label for the source of information
         * @return {@link Card.Source#label}
         */
        @JsonGetter
        public String getLabel() {
            return label;
        }

        /**
         * Set the human-readable label for the source of information
         * @param label {@link Card.Source#label}
         */
        @JsonSetter
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * Get th absolute URL for additional information about the
         * organization or data set that provided the information
         * @return {@link Card.Source#uri}
         */
        @JsonGetter
        public String getUri() {
            return uri;
        }

        /**
         * Set the absolute URL for additional information about the
         * organization or data set that provided the information
         * @param uri {@link Card.Source#uri}
         */
        @JsonSetter
        public void setUri(String uri) {
            this.uri = uri;
        }

        /**
         * Get absolute URL to an icon for the source of this card
         * @return {@link Card.Source#icon}
         */
        @JsonGetter
        public String getIcon() {
            return icon;
        }

        /**
         * Set absolute URL to an icon for the source of this card
         * @param icon {@link Card.Source#icon}
         */
        @JsonSetter
        public void setIcon(String icon) {
            this.icon = icon;
        }

        /**
         * Get the topic description of the source content
         * @return {@link Card.Source#topic}
         */
        @JsonGetter
        public Coding getTopic() {
            return topic;
        }

        /**
         * Set the topic description of the source content
         * @param topic {@link Card.Source#topic}
         */
        @JsonSetter
        public void setTopic(Coding topic) {
            this.topic = topic;
        }
    }

    public static class Suggestion {

        // member variables

        /**
         * Human-readable label to display for this suggestion (e.g. the CDS Client might
         * render this as the text on a button tied to this suggestion).
         */
        @JsonProperty(required = true)
        private String label;

        /**
         * Unique identifier, used for auditing and logging suggestions.
         */
        private String uuid;

        /**
         * When there are multiple {@link Card#suggestions}, allows a service to indicate that
         * a specific suggestion is recommended from all the available suggestions on the card.
         * CDS Hooks clients may choose to influence their UI based on this value, such as
         * pre-selecting, or highlighting recommended suggestions. Multiple suggestions MAY be
         * recommended, if {@link Card#selectionBehavior} is any.
         */
        private boolean isRecommended;

        /**
         * Array of objects, each defining a suggested action. Within a suggestion, all actions
         * are logically AND'd together, such that a user selecting a suggestion selects all of
         * the actions within it. When a suggestion contains multiple actions, the actions SHOULD
         * be processed as per FHIR's rules for processing transactions with the CDS Client's
         * fhirServer as the base URL for the inferred full URL of the transaction bundle entries.
         * (Specifically, deletes happen first, then creates, then updates).
         * @see Card.Suggestion.Action
         */
        private List<Card.Suggestion.Action> actions;

        // getters and setters

        /**
         * Get the human-readable label for the suggestion
         * @return {@link Card.Suggestion#label}
         */
        @JsonGetter
        public String getLabel() {
            return label;
        }

        /**
         * Set the human-readable label for the suggestion
         * @param label {@link Card.Suggestion#label}
         */
        @JsonSetter
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * Get the unique identifier for the suggestion
         * @return {@link Card.Suggestion#uuid}
         */
        @JsonGetter
        public String getUuid() {
            return uuid;
        }

        /**
         * Set the unique identifier for the suggestion
         * @param uuid {@link Card.Suggestion#uuid}
         */
        @JsonSetter
        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Get indicator on whether the suggestion is recommended
         * @return {@link Card.Suggestion#isRecommended}
         */
        @JsonGetter
        public boolean isRecommended() {
            return isRecommended;
        }

        /**
         * Set indicator on whether the suggestion is recommended
         * @param recommended {@link Card.Suggestion#isRecommended}
         */
        @JsonSetter
        public void setRecommended(boolean recommended) {
            isRecommended = recommended;
        }

        /**
         * Get the set of suggested actions
         * @return {@link Card.Suggestion#actions}
         */
        @JsonGetter
        public List<Card.Suggestion.Action> getActions() {
            return actions;
        }

        /**
         * Set the set of suggested actions
         * @param actions {@link Card.Suggestion#actions}
         */
        @JsonSetter
        public void setActions(List<Card.Suggestion.Action> actions) {
            this.actions = actions;
        }

        public static class Action {

            // member variables

            @JsonIgnore
            public FhirContext fhirContext;

            /**
             * The type of action being performed. Allowed values are: create, update, delete.
             */
            @JsonProperty(required = true)
            private String type;

            /**
             * Human-readable description of the suggested action MAY be presented to the end-user.
             */
            @JsonProperty(required = true)
            private String description;

            /**
             * A FHIR resource. When the {@link Card.Suggestion.Action#type} attribute is create,
             * the resource attribute SHALL contain a new FHIR resource to be created. For update,
             * this holds the updated resource in its entirety and not just the changed fields.
             * Use of this field to communicate a string of a FHIR id for delete suggestions is
             * DEPRECATED and {@link Card.Suggestion.Action#resourceId} SHOULD be used instead.
             */
            private IBaseResource resource;

            /**
             * A relative reference to the relevant resource. SHOULD be provided when the
             * {@link Card.Suggestion.Action#type} attribute is delete.
             */
            private String resourceId;

            // getters and setters

            /**
             * Get the type of action to be performed
             * @return {@link Card.Suggestion.Action#type}
             */
            @JsonGetter
            public String getType() {
                return type;
            }

            /**
             * Set the type of action to be performed
             * @param type {@link Card.Suggestion.Action#type}
             * @throws ErrorHandling.CdsHooksError Error detailing
             * unknown {@link Card.Suggestion.Action#type} value
             */
            @JsonSetter
            public void setType(String type) throws ErrorHandling.CdsHooksError {
                switch (type.toLowerCase()) {
                    case "create":
                    case "update":
                    case "delete":
                        this.type = type;
                        break;
                    case "remove":
                        this.type = "delete";
                        break;
                    default: throw new ErrorHandling.CdsHooksError(
                            String.format("Unknown suggestion.action.type value: %s", type));
                }
            }

            /**
             * Get the human-readable description of the suggested action
             * @return {@link Card.Suggestion.Action#description}
             */
            @JsonGetter
            public String getDescription() {
                return description;
            }

            /**
             * Set the human-readable description of the suggested action
             * @param description {@link Card.Suggestion.Action#description}
             */
            @JsonGetter
            public void setDescription(String description) {
                this.description = description;
            }

            /**
             * Get the resource to be created/updated by the suggested action
             * @return @link Card.Suggestion.Action#resource
             */
            public IBaseResource getResource() {
                return resource;
            }

            /**
             * Get the resource to be created/updated by the suggested action
             * @return String representation of {@link Card.Suggestion.Action#resource}
             */
            @JsonGetter("resource")
            @JsonRawValue
            public String getResourceString() {
                if (resource == null) return null;
                return new JsonParser(fhirContext, new LenientErrorHandler()).setPrettyPrint(true).encodeResourceToString(resource);
            }

            /**
             * Set the resource to be created/updated by the suggested action
             * @param resource {@link Card.Suggestion.Action#resource}
             */
            @JsonSetter
            public void setResource(IBaseResource resource) {
                this.resource = resource;
            }

            /**
             * Get the ID of the resource suggested to be deleted by this action
             * @return {@link Card.Suggestion.Action#resourceId}
             */
            @JsonGetter
            public String getResourceId() {
                if (getType() != null && getType().equals("delete") && getResource() != null) {
                    resourceId = getResource().getIdElement().getValue();
                }
                return resourceId;
            }

            /**
             * Set the ID of the resource suggested to be deleted by this action
             * @param resourceId {@link Card.Suggestion.Action#resourceId}
             */
            @JsonSetter
            public void setResourceId(String resourceId) {
                this.resourceId = resourceId;
            }
        }
    }

    public static class Link {

        // member variables

        /**
         * Human-readable label to display for this link (e.g. the CDS Client might render this
         * as the underlined text of a clickable link).
         */
        @JsonProperty(required = true)
        private String label;

        /**
         * URL to load (via GET, in a browser context) when a user clicks on this link. Note
         * that this MAY be a "deep link" with context embedded in path segments, query
         * parameters, or a hash.
         */
        @JsonProperty(required = true)
        private String url;

        /**
         * The type of the given URL. There are two possible values for this field. A type of
         * absolute indicates that the URL is absolute and should be treated as-is. A type of
         * smart indicates that the URL is a SMART app launch URL and the CDS Client should
         * ensure the SMART app launch URL is populated with the appropriate SMART launch parameters.
         */
        @JsonProperty(required = true)
        private String type;

        public enum LinkType {
            ABSOLUTE("absolute"), SMART("smart");
            LinkType(String actionType) {
            }
        }

        /**
         * An optional field that allows the CDS Service to share information from the CDS card
         * with a subsequently launched SMART app. The appContext field should only be valued if
         * the {@link Card.Link#type} is smart and is not valid for absolute links. The appContext
         * field and value will be sent to the SMART app as part of the
         * <a href="https://oauth.net/2/">OAuth 2.0</a> access token response, alongside the other
         * <a href="http://hl7.org/fhir/smart-app-launch/1.0.0/scopes-and-launch-context/#launch-context-arrives-with-your-access_token">SMART launch parameters</a>
         * when the SMART app is launched. Note that appContext could be escaped JSON, base64
         * encoded XML, or even a simple string, so long as the SMART app can recognize it. CDS
         * Client support for appContext requires additional coordination with the authorization
         * server that is not described or specified in CDS Hooks nor SMART.
         */
        private String appContext;

        // getters and setters

        /**
         * Get the human-readable label for the link
         * @return {@link Card.Link#label}
         */
        @JsonGetter
        public String getLabel() {
            return label;
        }

        /**
         * Set the human-readable label for the link
         * @param label {@link Card.Link#label}
         */
        @JsonSetter
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * Get the URL for the link
         * @return {@link Card.Link#url}
         */
        @JsonGetter
        public String getUrl() {
            return url;
        }

        /**
         * Set the URL for the link
         * @param url {@link Card.Link#url}
         */
        @JsonSetter
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Get the type of the given {@link Card.Link#url}
         * @return {@link Card.Link#type}
         */
        @JsonGetter
        public String getType() {
            return type;
        }

        /**
         * Set the type of the given {@link Card.Link#url}
         * @param type {@link Card.Link#type}
         * @throws ErrorHandling.CdsHooksError Error detailing unknown {@link Card.Link#type} value
         */
        @JsonSetter
        public void setType(String type) throws ErrorHandling.CdsHooksError {
            switch (type.toLowerCase()) {
                case "absolute":
                case "smart":
                    this.type = type;
                    break;
                default: throw new ErrorHandling.CdsHooksError(
                        String.format("Unknown link.type value: %s", type));
            }
        }

        /**
         * Get the context to share with a SMART app
         * @return {@link Card.Link#appContext}
         * @throws ErrorHandling.CdsHooksError Error detailing invalid use of appContext
         * with absolute {@link Card.Link#type}
         */
        @JsonGetter
        public String getAppContext() throws ErrorHandling.CdsHooksError {
            if (appContext == null) return null;
            else if (getType().equals("smart")) {
                return appContext;
            }
            throw new ErrorHandling.CdsHooksError("Invalid appContext for absolute link type");
        }

        /**
         * Set the context to share with a SMART app
         * @param appContext {@link Card.Link#appContext}
         */
        @JsonSetter
        public void setAppContext(String appContext) {
            this.appContext = appContext;
        }
    }
}
