package org.opencds.cqf.ruler.casereporting.r4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.Canonicals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ChangeLog {
  public List<Page<?>> pages;
  public String manifestUrl;
  ChangeLog(String url) {
    this.pages = new ArrayList<Page<?>>();
    this.manifestUrl = url;
  }
  public <T extends BaseMetadataObject> Page<T> addPage(String url, T oldData, T newData) {
    var page = new Page<T>(url, oldData, newData);
    this.pages.add(page);
    return page;
  }
  public Page<ValueSetChild> addPage(ValueSet theSourceResource, ValueSet theTargetResource, KnowledgeArtifactProcessor.diffCache cache) throws UnprocessableEntityException {
    if (!theSourceResource.getUrl().equals(theTargetResource.getUrl())) {
      throw new UnprocessableEntityException("URLs don't match");
    }
    // Map< [Code], [Object with code, version, system, etc.] > 
    Map<String, ValueSetChild.Code> codeMap = new HashMap<String, ValueSetChild.Code>();
    updateCodeMap(codeMap, theSourceResource, cache);
    updateCodeMap(codeMap, theTargetResource, cache);
    var oldData = new ValueSetChild(theSourceResource.getTitle(), theSourceResource.getIdPart(), theSourceResource.getVersion(), KnowledgeArtifactProcessor.getPriority(theSourceResource), KnowledgeArtifactProcessor.getConditions(theSourceResource), theSourceResource.getCompose().getInclude(), theSourceResource.getExpansion().getContains(), codeMap);
    var newData = new ValueSetChild(theTargetResource.getTitle(), theTargetResource.getIdPart(), theTargetResource.getVersion(),KnowledgeArtifactProcessor.getPriority(theTargetResource), KnowledgeArtifactProcessor.getConditions(theTargetResource), theTargetResource.getCompose().getInclude(), theTargetResource.getExpansion().getContains(), codeMap);
    var url = theTargetResource.getUrl();
    var page = new Page<ValueSetChild>(url, oldData, newData);
    this.pages.add(page);
    return page;
  }
  private void updateCodeMap(Map<String, ValueSetChild.Code> codeMap, ValueSet valueSet, KnowledgeArtifactProcessor.diffCache cache) {
    // looks like deleted and inserted leafs dont get cached, need to update diff method
    if (valueSet.getCompose().hasInclude()) {
      valueSet.getCompose().getInclude()
        .forEach(concept -> {
          if (concept.hasConcept()) {
            mapConceptSetToCodeMap(codeMap, concept, Canonicals.getIdPart(valueSet.getUrl()));
          }
          if (concept.hasValueSet()) {
            concept.getValueSet().stream()
            .map(vs -> {
              var test = cache.getResource(vs.getValue())
              .map(v -> {
                var test2 = v;
                return (ValueSet) v;
              });
              return test;
            }
            )
            .filter(Optional::isPresent).map(Optional::get)
            .forEach(vs -> updateCodeMap(codeMap, vs, cache));
          }
        });
    }

  }
  private void mapConceptSetToCodeMap(Map<String, ValueSetChild.Code> codeMap, ValueSet.ConceptSetComponent concept, String source){
      var system = concept.getSystem();
      var id = concept.getId();
      var version = concept.getVersion();
      concept.getConcept()
      .stream()
      .filter(ValueSet.ConceptReferenceComponent::hasCode)
      .forEach(conceptReference -> {
        var code = new ValueSetChild.Code(id, system, conceptReference.getCode(), version, conceptReference.getDisplay(), source, null);
        codeMap.put(conceptReference.getCode(), code);
      });
  }
  public Page<LibraryChild> addPage(Library theSourceResource, Library theTargetResource) throws UnprocessableEntityException {
    if (!theSourceResource.getUrl().equals(theTargetResource.getUrl())) {
      throw new UnprocessableEntityException("URLs don't match");
    }
    var oldData = new LibraryChild(theSourceResource.getTitle(), theSourceResource.getIdPart(), theSourceResource.getName(), theSourceResource.getPurpose(), theSourceResource.getVersion(),Optional.ofNullable((Period)theSourceResource.getEffectivePeriod()).map(p -> p.getStart()).map(s-> s.toString()).orElse(null), Optional.ofNullable(theSourceResource.getApprovalDate()).map(s-> s.toString()).orElse(null), theSourceResource.getRelatedArtifact());
    var newData = new LibraryChild(theTargetResource.getTitle(), theTargetResource.getIdPart(), theTargetResource.getName(), theTargetResource.getPurpose(), theTargetResource.getVersion(),Optional.ofNullable((Period)theTargetResource.getEffectivePeriod()).map(p -> p.getStart()).map(s-> s.toString()).orElse(null), Optional.ofNullable(theTargetResource.getApprovalDate()).map(s-> s.toString()).orElse(null), theTargetResource.getRelatedArtifact());    
    var url = theTargetResource.getUrl();
    var page = new Page<LibraryChild>(url, oldData, newData);
    this.pages.add(page);
    return page;
  }
  public Page<PlanDefinitionChild> addPage(PlanDefinition theSourceResource, PlanDefinition theTargetResource) throws UnprocessableEntityException {
    if (!theSourceResource.getUrl().equals(theTargetResource.getUrl())) {
      throw new UnprocessableEntityException("URLs don't match");
    }
    var oldData = new PlanDefinitionChild(theSourceResource.getTitle(), theSourceResource.getIdPart(), theSourceResource.getVersion());
    var newData = new PlanDefinitionChild(theTargetResource.getTitle(), theTargetResource.getIdPart(), theTargetResource.getVersion());
    var url = theTargetResource.getUrl();
    var page = new Page<PlanDefinitionChild>(url, oldData, newData);
    this.pages.add(page);
    return page;
  }
  public boolean hasPage(String url) {
    return this.pages.stream().filter(p -> p.url.equals(url)).findAny().isPresent();
  }
  public Optional<Page<? extends BaseMetadataObject>> getPage(String url) {
    return this.pages.stream().filter(p -> p.url.equals(url)).findAny();
  }
  public static class Page<T extends BaseMetadataObject> {
      public T oldData;
      public T newData;
      public String url;
      public List<Page<? extends BaseMetadataObject>> children;
      Page(String url, T oldData, T newData) {
        this.url = url;
        this.oldData = oldData;
        this.newData = newData;
      }
      void addOperation(String type, String path, Object value, Object original, ChangeLog parent) {
        if (type != null) {
          switch (type) {
            case "replace":
              addReplaceOperation(type, path, value, value, parent);
              break;
            case "delete":
              addDeleteOperation(type, path, null, value, parent);
              break;
            case "insert":
              addInsertOperation(type, path, value, null, parent);
              break;
            default:
              throw new UnprocessableEntityException("Unknown type provided when adding an operation to the ChangeLog");
          }
        } else {
          throw new UnprocessableEntityException("Type must be provided when adding an operation to the ChangeLog");
        }
      }
      void addInsertOperation(String type, String path, Object value, Object original, ChangeLog parent) {
        if (type != "insert") {
          throw new UnprocessableEntityException("wrong type");
        }
        this.newData.addOperation(type, path, value, original, parent);
      }
      void addDeleteOperation(String type, String path, Object value, Object original, ChangeLog parent) {
        if (type != "delete") {
          throw new UnprocessableEntityException("wrong type");
        }
        this.oldData.addOperation(type, path, value, original, parent);
      }
      void addReplaceOperation(String type, String path, Object value, Object original, ChangeLog parent) {
        if (type != "replace") {
          throw new UnprocessableEntityException("wrong type");
        }
        this.oldData.addOperation(type, path, value, null, parent);
        this.newData.addOperation(type, path, null, original, parent);
      }
  }
  public static class SingleValue {
    public String value;
    public ChangeLogOperation operation;
    public void setOperation(ChangeLogOperation operation) {
      if ( operation != null ) {
        if( this.operation != null
        && this.operation.type == operation.type
        && this.operation.path == operation.path
        && this.operation.newValue != operation.newValue) {
          throw new UnprocessableEntityException("Multiple changes to the same element");
        }
        this.operation = operation;
      }
    }
  }
  public static class ChangeLogOperation {
    public String type;
    public String path;
    public String newValue;
    public String oldValue;
    private final IParser parser = FhirContext.forR4().newJsonParser();
    ChangeLogOperation(String type, String path, IBase newValue, IBase original) {
      this.type = type;
      this.path = path;
      if (original != null) {
        this.oldValue = original.toString();
      }
      if (newValue != null) {
        this.newValue = newValue.toString();
      }
    }
    ChangeLogOperation(String type, String path, IBase newValue, String original) {
      this.type = type;
      this.path = path;
      this.oldValue = original;
      if (newValue != null) {
        this.newValue = newValue.toString();
      }    
    }
    ChangeLogOperation(String type, String path, Object newValue, Object original) {
      this.type = type;
      this.path = path;
      if (original != null) {
        this.oldValue = original.toString();
      }
      if (newValue != null) {
        this.newValue = newValue.toString();
      }
    }
    
  }
  public static class BaseMetadataObject {
    public SingleValue title  = new SingleValue();
    public SingleValue id = new SingleValue();
    public SingleValue version = new SingleValue();
    public String resourceType;
    BaseMetadataObject(String title, String id, String version) {
      if (!StringUtils.isEmpty(title)) {
        this.title.value = title;
      }
      if (!StringUtils.isEmpty(id)) {
        this.id.value = id;
      }
        if (!StringUtils.isEmpty(version)) {
      this.version.value = version;
        }
    }
    public void addOperation(String type, String path, Object value, Object original, ChangeLog parent) {
      if (type != null) {
        var newOp = new ChangeLogOperation(type, path, value, original);
        if (path.equals("id")) {
          this.id.setOperation(newOp);
        } else if (path.contains("title")) {
          this.title.setOperation(newOp);
        } else if (path.equals("version")) {
          this.version.setOperation(newOp);
        }
      }
    }
  }
  public static class ValueSetChild extends BaseMetadataObject {
    public List<Code> codes = new ArrayList<>();
    public List<Grouper> grouperList = new ArrayList<>();
    // public KnowledgeArtifactProcessor.diffCache cache;
    public final String resourceType = "ValueSet";
    public static class Code {
      public String id;
      public String system;
      public String code;
      public String version;
      public String display;
      public String memberOid;
      public ChangeLogOperation operation;
      Code(String id, String system, String code, String version, String display, String memberOid, ChangeLogOperation operation) {
        this.id = id;
        this.system = system;
        this.code = code;
        this.version = version;
        this.display = display;
        this.memberOid = memberOid;
        this.operation = operation;
      }
      public ChangeLogOperation getOperation() {
        return this.operation;
      }
      public void setOperation(ChangeLogOperation operation) {
        if( operation != null ) {
          if( this.operation != null
          && this.operation.type == operation.type
          && this.operation.path == operation.path
          && this.operation.newValue != operation.newValue) {
            throw new UnprocessableEntityException("Multiple changes to the same element");
          }
          this.operation = operation;
        }
      }
    }
    public static class Grouper {
      public String memberOid;
      public List<Code> conditions = new ArrayList<Code>();
      public SingleValue priority = new SingleValue();
      public ChangeLogOperation operation;
    }
    ValueSetChild(String title, String id, String version, String priority, List<CodeableConcept> conditions, List<ValueSet.ConceptSetComponent> compose, List<ValueSet.ValueSetExpansionContainsComponent> contains, Map< String , Code> codeMap) {
      super(title, id, version);
      if (contains != null) {
        contains.forEach(contained -> {
          if (codeMap.get(contained.getCode()) != null) {
            var code = codeMap.get(contained.getCode());
            this.codes.add(new Code(code.id, code.system, code.code, code.version, code.display, code.memberOid, code.operation));
          }
        });
      }
      if (compose != null) {
        compose.stream()
        .filter(cmp -> cmp.hasValueSet())
        .flatMap(c -> c.getValueSet().stream())
        .filter(vs -> vs.hasValue())
        .map(vs -> vs.getValue())
        .forEach(vs -> {
          var grouper = new Grouper();
          grouper.memberOid = vs;
          this.grouperList.add(grouper);
        });
      }
    }
    public Code addCode(String id, String system, String code, String version, String memberOid, String display) {
      var newCodeObj = new Code(id, system, code, version, display, memberOid, null);
      this.codes.add(newCodeObj);
      return newCodeObj;
    }
    @Override
    public void addOperation(String type, String path, Object value, Object original, ChangeLog parent) {
      if (type != null) {
        super.addOperation(type, path, value, original, parent);
        var operation = new ChangeLogOperation(type,path,value,original);
        if (path.contains("compose.include")) {
          // if the valuesets changed
          String urlToCheck = null;
          if (value instanceof IPrimitiveType || original instanceof IPrimitiveType) {
            urlToCheck = value instanceof IPrimitiveType ? ((IPrimitiveType<String>) value).getValue() : ((IPrimitiveType<String>) original).getValue();
          } else if (original instanceof ValueSet.ValueSetComposeComponent){
            urlToCheck = ((ValueSet.ValueSetComposeComponent) original).getIncludeFirstRep().getValueSet().get(0).getValue();
          }
          if (urlToCheck != null) {
            final var urlNotNull = urlToCheck;
            this.grouperList.stream().forEach(grouper -> {
              if (grouper.memberOid.equals(urlNotNull)) {
                grouper.operation = operation;
              }
            });
          }
        } 
        else if (path.contains("expansion.contains[")) {
          // if the codes themselves changed
          String codeToCheck = null;
          if (value instanceof IPrimitiveType || original instanceof IPrimitiveType) {
            codeToCheck = value instanceof IPrimitiveType ? ((IPrimitiveType<String>) value).getValue() : ((IPrimitiveType<String>) original).getValue();
          } else if (original instanceof ValueSet.ValueSetExpansionContainsComponent){
            codeToCheck = ((ValueSet.ValueSetExpansionContainsComponent) original).getCode();
          }
          if (codeToCheck != null) {
            final String codeNotNull = codeToCheck;
            this.codes.stream()
            .filter(code -> code.code != null)
            .filter(code -> code.code.equals(codeNotNull)).findAny()
            .ifPresentOrElse(code -> {
              code.setOperation(operation);
            },
            () -> {
              // handle missing codes
            }); 
          }
        }        // FIXME: conditions and priorities
        // else if (path.contains("extension")) {
        //   // handle conditions
        //   var conditions = this.getConditions();
        //   conditions.stream()
        //   .filter(condition -> condition.code != null)
        //   .filter(condition -> condition.code.equals(value)).findAny()
        //   .ifPresentOrElse(condition -> {
        //     condition.setOperation(operation);
        //   },
        //   () -> {
        //     // handle missing conditions
        //   });
        // } else if(path.contains(".priority")) {
        //   this.priority.setOperation(null);
        // }
      }
    }
  }
  public static class PlanDefinitionChild extends BaseMetadataObject {
    public final String resourceType = "PlanDefinition";
    PlanDefinitionChild(String title, String id, String version) {
      super(title, id, version);
    }
  }
  public static class RelatedArtifactWithOperation {
    public String stringValue;
    public RelatedArtifact value;
    public ChangeLogOperation operation;
    public String targetUrl;
    RelatedArtifactWithOperation(RelatedArtifact value) {
      this.targetUrl = value.getResource();
      this.stringValue = value.toString();
      this.value = value;
    }
  }
  public static class LibraryChild extends BaseMetadataObject {
    public final String resourceType = "Library";
    public SingleValue name = new SingleValue();
    public SingleValue purpose = new SingleValue();
    public SingleValue effectiveStart = new SingleValue();
    public SingleValue releaseDate = new SingleValue();
    public List<RelatedArtifactWithOperation> relatedArtifacts = new ArrayList<>();
    LibraryChild(String name, String purpose, String title, String id, String version, String effectiveStart, String releaseDate, List<RelatedArtifact> relatedArtifacts) {
      super(title, id, version);
      if (!StringUtils.isEmpty(name)) {
        this.name.value = name;
      }
      if (!StringUtils.isEmpty(purpose)) {
        this.purpose.value = purpose;
      }
      if (!StringUtils.isEmpty(effectiveStart)) {
        this.effectiveStart.value = effectiveStart;
      }
      if (!StringUtils.isEmpty(releaseDate)) {
        this.releaseDate.value = releaseDate;
      }
      if (!relatedArtifacts.isEmpty()) {
        relatedArtifacts.forEach(ra -> this.relatedArtifacts.add(new RelatedArtifactWithOperation(ra)));
      }
    }
    private Optional<RelatedArtifactWithOperation> getRelatedArtifactFromUrl(String target) {
      return this.relatedArtifacts.stream().filter(ra -> ra.targetUrl != null && ra.targetUrl.equals(target)).findAny();
    }
    @Override
    public void addOperation(String type, String path, Object value, Object original, ChangeLog parent) {
      if(type != null) {
        super.addOperation(type, path, value, original, parent);
        var operation = new ChangeLogOperation(type, path, value, original);
        Optional<RelatedArtifactWithOperation> operationTarget = Optional.ofNullable(null);
        if (path != null && path.contains("elatedArtifact") ){
          if (value instanceof RelatedArtifact) {
            operationTarget = getRelatedArtifactFromUrl(((RelatedArtifact) value).getResource());
          } else if (original instanceof RelatedArtifact) {
            operationTarget = getRelatedArtifactFromUrl(((RelatedArtifact) value).getResource());
          } else if (path.contains("[")) {
            var matcher = Pattern
										.compile("relatedArtifact\\[(\\d+)\\]")
										.matcher(path);
            if (matcher.find()) {
              var pathToRelatedArtifact = matcher.group();
              var relatedArtifactIndex = Integer.parseInt(matcher.group(1));
              operationTarget = Optional.of(this.relatedArtifacts.get(relatedArtifactIndex));
            }
          }
          if (operationTarget.isPresent()) {
            operationTarget.get().operation = operation;
          }
        }
      }
    }
  }
}
