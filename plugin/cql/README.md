# CQL Plugin

This plugin provides implementations of CQL-related interfaces to support FHIR operation implementations.

The main CQL-related interfaces are:

| Interface | Description |
|:--|:--:|
| LibraryLoader | Used to load ELM library content from FHIR sources |
| TerminologyProvider | Used perform terminology operations in CQL |
| RetrieveProvider | Use to load clinical data |
| FHIRDal | Used to load FHIR Resources |

Additionally, there are implementations of services for caching of ELM, Terminology, etc.

HAPI has an implementation of multi-tenancy that depends on an `tenantId` as part of the request, which provides partitioning of data. Implementations of operation providers should generally be tenant-aware, and therefore an appropriate instance of the above interfaces must be constructed on a per-request basis.
