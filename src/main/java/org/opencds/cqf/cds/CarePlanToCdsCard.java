package org.opencds.cqf.cds;

import org.hl7.fhir.dstu3.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarePlanToCdsCard {

    public static List<CdsCard> convert(CarePlan carePlan) {
        List<CdsCard> cards = new ArrayList<>();

        for (CarePlan.CarePlanActivityComponent activity : carePlan.getActivity()) {
            CdsCard card = new CdsCard();
            if (activity.hasReference() && activity.getReferenceTarget() instanceof RequestGroup) {
                RequestGroup requestGroup = (RequestGroup) activity.getReferenceTarget();
                card = convert(requestGroup);
            }
            if (activity.hasExtension() && activity.getExtensionFirstRep().getValue() instanceof StringType) {
                // indicator
                card.setIndicator(activity.getExtensionFirstRep().getValue().toString());
            }
            cards.add(card);
        }

        return cards;
    }

    private static CdsCard convert(RequestGroup requestGroup) {
        CdsCard card = new CdsCard();

        // links
        if (requestGroup.hasExtension()) {
            List<CdsCard.Links> links = new ArrayList<>();
            for (Extension extension : requestGroup.getExtension()) {
                CdsCard.Links link = new CdsCard.Links();

                if (extension.getValue() instanceof StringType) {
                    String labelOrType = extension.getValue().toString();
                    if (labelOrType.equals("absolute") || labelOrType.equals("relative")) {
                        link.setType(labelOrType);
                    }
                    else {
                        link.setLabel(labelOrType);
                    }
                }

                else if (extension.getValue() instanceof UriType) {
                    String url = extension.getValue().toString();
                    link.setUrl(url);
                }

                else {
                    throw new RuntimeException("Invalid link extension type: " + extension.getValue().fhirType());
                }

                links.add(link);
            }
            card.setLinks(links);
        }

        if (requestGroup.hasAction()) {
            for (RequestGroup.RequestGroupActionComponent action : requestGroup.getAction()) {
                // basic
                if (action.hasTitle()) {
                    card.setSummary(action.getTitle());
                }
                if (action.hasDescription()) {
                    card.setDetail(action.getDescription());
                }

                // source
                if (action.hasDocumentation()) {
                    // Assuming first related artifact has everything
                    RelatedArtifact documentation = action.getDocumentationFirstRep();
                    CdsCard.Source source = new CdsCard.Source();
                    if (documentation.hasDisplay()) {
                        source.setLabel(documentation.getDisplay());
                    }
                    if (documentation.hasUrl()) {
                        source.setUrl(documentation.getUrl());
                    }
                    if (documentation.hasDocument() && documentation.getDocument().hasUrl()) {
                        source.setIcon(documentation.getDocument().getUrl());
                    }

                    card.setSource(source);
                }

                // suggestions
                // TODO - uuid
                boolean hasSuggestions = false;
                CdsCard.Suggestions suggestions = new CdsCard.Suggestions();
                CdsCard.Suggestions.Action actions = new CdsCard.Suggestions.Action();
                if (action.hasLabel()) {
                    suggestions.setLabel(action.getLabel());
                    hasSuggestions = true;
                }
                if (action.hasType()) {
                    actions.setType(CdsCard.Suggestions.Action.ActionType.valueOf(action.getType().getCode()));
                    hasSuggestions = true;
                }
                if (action.hasResource()) {
                    actions.setResource(action.getResourceTarget());
                    hasSuggestions = true;
                }
                if (hasSuggestions) {
                    suggestions.setActions(Collections.singletonList(actions));
                    card.setSuggestions(Collections.singletonList(suggestions));
                }
            }
        }

        return card;
    }
}
