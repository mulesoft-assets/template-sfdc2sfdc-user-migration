# Mule Kick: SFDC to SFDC User Migration

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [A few Considerations](#afewconsiderations)
    * [Running on CloudHub](#runoncloudhub)
    	* [Deploying your Kick on CloudHub](#deployingyourkickoncloudhub)
    * [Running on premise](#runonopremise)
        * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [inboundEndpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)
+ [Testing the Kick](#testingthekick)

   


# Use Case <a name="usecase"/>
As a Salesforce admin I want to migrate users between two Salesfoce orgs.

This Kick (template) should serve as a foundation for the process of migrating users from one Salesfoce instance to another, being able to specify filtering criterias and desired behaviour when a contact already exists in the destination org. 

As implemented, this Kick leverage the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing).
The batch job is divided in Input, Process and On Complete stages.
During the Input stage the Kick will go to the SalesForce Org A and query all the existing users that match the filter criteria.
During the Process stage, each SFDC User will be filtered depending on, if it has an existing matching user in the SFDC Org B and if the last updated date of the later is greater than the one of SFDC Org A.
The last step of the Process stage will group the users and create them in SFDC Org B.
Finally during the On Complete stage the Kick will both otput statistics data into the console and send a notification email with the results of the batch excecution. 

# Run it! <a name="runit"/>

Simple steps to get SFDC to SFDC User Migration running.

In any of the ways you would like to run this Kick this is an example of the output you'll see after hitting the HTTP endpoint:

<pre>
<h1>Batch Process initiated</h1>
<b>ID:</b>6eea3cc6-7c96-11e3-9a65-55f9f3ae584e<br/>
<b>Records to Be Processed: </b>9<br/>
<b>Start execution on: </b>Mon Jan 13 18:05:33 GMT-03:00 2014
</pre>

## A few Considerations <a name="afewconsiderations" />

There are a couple of things you should take into account before running this kick:
1. **Users cannot be deleted in SalesForce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for a new user.
2. **Each user needs to be associated to a Profile:** SalesForce's profiles are what define the permissions the user will have for manipulating data and other users. Each SalesForce account has its own profiles. In this kick you will find a processor labeled *assignProfileId and Username to the User* where to map your Profile Ids from the source account to the ones in the target account. Note that for the integration test to run properly, you should change the constant *DEFAULT_PROFILE_ID* in *BusinessLogicTestIT* to one that's valid in your source test organization.
3. **Working with sandboxes for the same account**: Although each sandbox should be a completely different environment, Usernames cannot be repeated in different sandboxes, i.e. if you have a user with username *bob.dylan* in *sandbox A*, you will not be able to create another user with username *bob.dylan* in *sandbox B*. If you are indeed working with Sandboxes for the same SalesForce account you will need to map the source username to a different one in the target sandbox, for this purpose, please refer to the processor labeled *assign ProfileId and Username to the User*.

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, supposing you choose as domain name `sfdcusermigration` to trigger the use case you just need to hit `http://sfdcusermigration.cloudhub.io/migrateusers` and report will be sent to the emails configured.

### Deploying your Kick on CloudHub <a name="deployingyourkickoncloudhub"/>
Mule Studio provides you with really easy way to deploy your Kick directly to CloudHub, for the specific steps to do so please check this [link](http://www.mulesoft.org/documentation/display/current/Deploying+Mule+Applications#DeployingMuleApplications-DeploytoCloudHub)


## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

After this, to trigger the use case you just need to hit the local http endpoint with the port you configured in your file. If this is, for instance, `9090` then you should hit: `http://localhost:9090/migrateusers` and this will create a CSV report and send it to the mails set.



## Properties to be configured (With examples)<a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

### Application configuration
+ http.port `9090` 

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/26.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/26.0`


#### EMail Details
+ mail.from `batch.contact.migration%40mulesoft.com`
+ mail.to `your.username@youremaildomain.com`
+ mail.subject `Batch Job Finished Report`

# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Kick is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organised by describing all the XML that conform the Kick.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [inboundEndpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.

## inboundEndpoints.xml<a name="endpointsxml"/>
This is the file where you will found the inbound and outbound sides of your integration app.
This Kick has only an [HTTP Inbound Endpoint](http://www.mulesoft.org/documentation/display/current/HTTP+Endpoint+Reference) as the way to trigger the use case.

###  Inbound Flow
**HTTP Inbound Endpoint** - Start Report Generation
+ `${http.port}` is set as a property to be defined either on a property file or in CloudHub environment variables.
+ The path configured by default is `synccontacts` and you are free to change for the one you prefer.
+ The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub will then route requests from your application domain URL to the endpoint.
+ The endpoint is configured as a *request-response* since as a result of calling it the response will be the total of Contacts synced and filtered by the criteria specified.


## businessLogic.xml<a name="businesslogicxml"/>
Functional aspect of the kick is implemented on this XML, directed by one flow responsible of excecuting the logic.
For the pourpose of this particular Kick the *mainFlow* just excecutes the Batch Job which handles all the logic of it.
This flow has Exception Strategy that basically consists on invoking the *defaultChoiseExceptionStrategy* defined in *errorHandling.xml* file.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 

# Testing the Kick <a name="testingthekick"/>

You will notice that the Kick has been shipped with test.
These devidi them self into two categories:

+ Unit Tests
+ Integration Tests

You can run any of them by just doing right click on the class and clicking on run as Junit test.

Do bear in mind that you'll have to tell the test classes which property file to use.
For you convinience we have added a file mule.test.properties located in "src/test/resources".
In the run configurations of the test just make sure to add the following property:

+ -Dmule.env=test
