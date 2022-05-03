# Plugin

This is the repository for reference implementation of the DaVinci Risk Based Contracts Member Attribution List project.

## Setup

To setup the project follow the instructions mentioned [here](https://github.com/DBCG/cqf-ruler#development)

## Usage

To use the Member Attribution List API's import the [Postman collection](https://github.com/DBCG/cqf-ruler/blob/feature-mal/mal/MAL-API's.postman_collection.json) added as part of this plugin into the Postman client. 

### Instructions to test the MAL API's
1. Create a Bundle of type transaction and add Patient, Coverage, RelatedPerson, Practitioner, PractitionerRole, Organization, Location resources as bundle entries. 
2. Use the POST Bundle request from postman collection to save all the resources into database.
3. Create a Group resource and use POST Group request from postman collection to save the Group resource into database.
4. Use Group Member add request from postman collection to add a new member into the `Group.member` data element.
5. Use Group Member remove request from postman collection to remove a member from the `Group.member` data element.
6. Use Group Export request from postman collection to initiate the request to export all the members data.
7. Use the Polling Location endpoint received in the response headers of Group Export Operation call to know the status of export operation.
8. Use the links received in the response body for each resource to download the data from Binary resource.


## Docker

The Dockerfile builds on top of the base cqf-ruler image and simply copies the jar into the `plugin` directory of the image.
