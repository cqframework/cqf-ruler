# Resource Loading

## Methods
 
### [GUI Resource Loader](https://github.com/DBCG/cqf-ruler/blob/master/src/main/java/org/opencds/cqf/helpers/ResourceLoaderGUI.java)

This project provides an interface to load resources. To access it, run the following command:

```
$ mvn exec:java -Dexec.mainClass="org.opencds.cqf.helpers.ResourceLoaderGUI"
```
    
The following dialog box should appear:
![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/load_resource_gui.png "Load Resources GUI")

As you can see, there are 3 different methods provided for loading resources.
1. Single Resource - moves a single resource from one data provider to another.
   - Specify "Source URL" and "Destination URL" and select "Single resource" from the dropdown menu.
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/single_resource_specify_url.png "Load Single Resource - Specify URLs")
   - Specify the "Resource ID" and the "Resource Type" and click "Load".
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/single_resource_load.png "Load Single Resource")
2. List of Resources - moves a list of resources for one data provider to another.
   - Specify "Source URL" and "Destination URL" and select "List of resources" from the dropdown menu.
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/list_resources_specify_url.png "Load List of Resources - Specify URLs")
   - Specify resources as a comma separated list with the resource ID followed by the resource type and click "Load".
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/list_resources_load.png "Load List of Resources")
3. Bundle from File - loads a Bundle of resources from a JSON file into a data provider.
   - Specify "Destination URL" and select "Bundle from file".
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/bundle_specify_url.png "Load Bundle from File - Specify URL")
   - Specify absolute file path to Bundle JSON file and click "Load".
   ![alt text](https://github.com/DBCG/cqf-ruler/tree/master/src/main/resources/img/bundle_load.png "Load Bundle from File")

The console will display any errors that occur.

## HTTP Client

There are 2 different methods of uploading resources using an HTTP client.
1. PUT or POST a single resource
   PUT method is used to create a new resource with a specified ID or update an existing resource.
   
     PUT [base]/baseDstu3/Practitioner/prac-123
     ```
     {
       "resourceType": "Practitioner",
       "id": "prac-123",
       "identifier": [
         {
           "system": "http://clinfhir.com/fhir/NamingSystem/practitioner",
           "value": "z1z1kXlcn3bhaZRsg7izSA1PYZm1"
         }
       ],
       "telecom": [
         {
           "system": "email",
           "value": "sruthi.v@collabnotes.com"
         }
       ]
     }
     ```
     Successful response
     ```
     {
       "resourceType": "OperationOutcome",
       "issue": [
         {
           "severity": "information", 
           "code": "informational",
           "diagnostics": "Successfully created resource \"Practitioner/prac-123/_history/1\" in 32ms"
         }
       ]
     }
     ```
     If the request results in an error, the "severity" will be specified as "error" and a message will be given in the "diagnostics" value field

   POST method is used to create a new resource with a generated ID.
     
     POST [base]/baseDstu3/Practitioner
     ```
     {
       "resourceType": "Practitioner",
       "identifier": [
         {
           "system": "http://clinfhir.com/fhir/NamingSystem/practitioner",
           "value": "z1z1kXlcn3bhaZRsg7izSA1PYZm1"
         }
       ],
       "telecom": [
         {
           "system": "email",
           "value": "sruthi.v@collabnotes.com"
         }
       ]
     }
     ```
     The response will be the same as the PUT method.

2. POST a transaction Bundle
    
    The [transaction](http://hl7.org/implement/standards/fhir/http.html#transaction) operation loads all the resources within a [transaction Bundle](https://github.com/DBCG/cqf-ruler/blob/master/src/main/resources/database-init-bundle.json).
    
    POST [base]/baseDstu3
    ```
    {
      "resourceType": "Bundle",
      "id": "example-transaction",
      "type": "transaction",
      "entry": [
        {
          "resource": {
            ...
          },
          "request": {
            "method": "PUT",
            "url": "[base]/baseDstu3/Resource/ResourceID"
          }
        },
        ...
      ]
    }
    ```
    The response will be a Bundle containing the status and location for each uploaded resource if successful or an OperationOutcome if there were errors.
    ```
    {
      "resourceType": "Bundle",
      "id": "...",
      "type": "transaction-response",
      "entry": [
        {
          "response": {
            "status": "201 Created",
            "location": "Resource/ResourceID/_history/1",
            ...
          },
          ...
        }
      ]
    }
    ```
    As an example, POST this [bundle](https://github.com/DBCG/cqf-ruler/blob/master/src/main/resources/database-init-bundle.json) to http://measure.eval.kanvix.com/cqf-ruler/baseDstu3.    