# covid-capacity
Monitoring locations in a JetBrains Space to encourage Social Distancing. This 
project is a submission to the [Instil Kotliners 2020](https://instil.co/2020/06/10/kotliners-comp/) 
competition. This was my first attempt at writing something in Kotlin.

While developers are generally able to work from home, there is often some
necessity for team members to meet in person, on site. Given the current 
Covid-19 situation we are still encouraged to maintain social distancing when
together. To help cope with this, we will take advantage of our Space 
subscription, where we can store team members and the location in which we can 
work. By updating the Location description with `covidCapacity: X`, where X 
is a number, we can warn team members that there are more people intending to 
attend a location that can be safely accomodated in the next 7 days. If this is
the case, each team member that is planning to attend that location will be 
sent a notification via Spaces Chat asking them to update their status if they
can.  

## Running
The application is a simple console application, which can be executed manually
or with a cron task to be performed on a schedule. Two envionmental variables 
must be set

* `COVID_SPACENAME` - Name of the Jetbrains Space instance
* `COVID_SPACETOKEN` - Full bearer token for authentication

To properly demonstrate this application a number of users and locations should
be created. When creating the location, an annotation in the description is 
required. e.g. The description for the Instil office could read

> An oasis of tranquility where work gets done. covidCapacity: 2   

When this is complete, for each user, go to their profile `https://tyndyll.jetbrains.space/m/<username>`,
and click on Locations in the left navigation bar. Click on "Add Location". 
This will display a dialog box where a team member can indicate which location 
they intend to be present at, during which dates. 

For the purposes of demonstration, assuming there is a covidCapacity of 2, 
* Create 3 users,
* For users one and two, set them to be attending the location for the next week
* Set the third user to be attending in the middle of that week.

Finally, a personal token is required to run the application. Open the profile
of the user you wish to run the application as and select "Personal Tokens" in 
the navigation bar. Click "New Personal token...", provide a memorable name and 
copy the provided token.

```bash
export COVID_SPACENAME=tyndyll
export COVID_SPACETOKEN="asdfasdfasfsadfasdfdsafsad...."
gradle clean shadowJar
java -jar build/libs/covid-capacity-1.0-SNAPSHOT-all.jar
```

Alternatively, open in IntelliJ, set the environmental variables and execute 
the main function

## TODO
Oh, so much... but to summarise to make this production ready

* Tests...
* The build....
    * As a Go developer I just don't understand
    * There is enough duct tape that it _should_ compile 
* Handle errors from Space API calls
    * For now we very much handle the happy path
* Provide option to run as a daemon which will perform the execution every X minutes
* Refactor to be more Kotlin like
    * This code was very much written by a Kotlin blowin
* Provide an ability to customise the message
* Provide an ability to change the location annotation we use to detect capacity
* Be able to authenticate as an application or service account, rather than 
  with a personal token