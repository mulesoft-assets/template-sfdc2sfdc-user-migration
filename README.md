
# Anypoint Template: Salesforce to Salesforce User Migration

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
As a Salesforce admin I want to migrate users from one Salesforce organization to another one.

This Anypoint Template should serve as a foundation for the process of migrating users from one Salesfoce instance to another, being able to specify filtering criteria and desired behavior when a user already exists in the destination org. 

As implemented, this Anypoint Template leverages the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing).
The batch job is divided in *Process* and *On Complete* stages.
Migration process starts from fetching all the existing Users that match the filter criteria from Salesforce Org A.
The User's profile ID is mapped from SalesForce instance A to B, each profile ID from the left (source instance) will be map to the one on the right (target instance). Only the users with the same profile ID as configured source ID are migrated.
The last step of the *Process* stage will group the users and create them in Salesfoce Org B.
Finally during the *On Complete* stage the Anypoint Template will both output statistics data into the console and send a notification email with the results of the batch execution.

# Considerations

There are a couple of things you should take into account before running this Anypoint Template:
1. **Users cannot be deleted in SalesForce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for a new user.
2. **Each user needs to be associated to a Profile:** SalesForce's profiles are what define the permissions the user will have for manipulating data and other users. Each SalesForce account has its own profiles. In this Anypoint Template you will find a processor labeled *assignProfileId and Username to the User* where to map your Profile Ids from the source account to the ones in the target account. Note that for the integration test to run properly, you should change the constant *DEFAULT_PROFILE_ID* in *BusinessLogicTestIT* to one that's valid in your source test organization.
3. **Working with sandboxes for the same account**: Although each sandbox should be a completely different environment, Usernames cannot be repeated in different sandboxes, i.e. if you have a user with username *bob.dylan* in *sandbox A*, you will not be able to create another user with username *bob.dylan* in *sandbox B*. If you are indeed working with Sandboxes for the same SalesForce account you will need to map the source username to a different one in the target sandbox, for this purpose, please refer to the processor labeled *assign ProfileId and Username to the User*.



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.









# Run it!
Simple steps to get Salesforce to Salesforce User Migration running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 
After this, to trigger the use case you just need to hit the local http endpoint with the port you configured in your file. If this is, for instance, `9090` then you should hit: `http://localhost:9090/syncusers` and this will create a CSV report and send it to the mails set.

## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.
Once your app is all set and started, supposing you choose as domain name `sfdcusermigration` to trigger the use case you just need to hit `http://sfdcusermigration.cloudhub.io/syncusers` and report will be sent to the emails configured.

### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**HTTP Connector configuration**     
+ http.port `9090`

**Starting Date e.g. 2017-12-13T03:00:59Z**
+ startingDate `2014-01-30'T'00:00:00.000'Z'`

**Batch Aggregator configuration**
+ page.size `200`

**SalesForce Connector configuration for company A**
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Profile ID from SalesForce instance A(source)**
+ sfdc.a.profile.id `00e200000015oKFAAY`

**SalesForce Connector configuration for company B**
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`

**Profile ID from SalesForce instance B(target)**
+ sfdc.b.profile.id `00e20000001UzDxAAK`

**SMTP Services configuration**
+ smtp.host `smtp.gmail.com`
+ smtp.port `587`
+ smtp.user `gmailuser`
+ smtp.password `gmailpassword`

**Mail Details**
+ mail.from `batch.user.migration%40mulesoft.com`
+ mail.to `your.username@youremaildomain.com`
+ mail.subject `Batch Job Finished Report`

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / ${page.size}***

Being ***X*** the number of Users to be synchronized on each run. 

The division by ***${page.size}*** is because, by default, Users are gathered in groups of ${page.size} for each Upsert API Call in the commit step.

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
Functional aspect of the Anypoint Template is implemented on this XML, directed by one flow responsible of executing the logic.
For the purpose of this particular Template the *mainFlow* uses a [Batch Job](http://www.mulesoft.org/documentation/display/current/Batch+Processing), which handles all the logic of it.



## endpoints.xml
This is the file where you will found the inbound and outbound sides of your integration app.
This Anypoint Template has only an [HTTP Inbound Endpoint](http://www.mulesoft.org/documentation/display/current/HTTP+Endpoint+Reference) as the way to trigger the use case.

###  Inbound Flow
**HTTP Inbound Endpoint** - Start Report Generation
+ `${http.port}` is set as a property to be defined either on a property file or in CloudHub environment variables.
+ The path configured by default is `syncusers` and you are free to change for the one you prefer.
+ The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub will then route requests from your application domain URL to the endpoint.
+ The endpoint is configured as a *request-response* since as a result of calling it the response will be the total of Users migrated and filtered by the criteria specified.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




